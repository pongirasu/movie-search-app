package com.paott.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paott.model.Movie;
import com.paott.repository.MovieRepository;

@Service
public class MovieImportService {
	
	private final MovieRepository movieRepository;
	
	private final Validator validator; // Bean Validation を手動で実行するために追加
	
	// JSONのtitleから邦題と原題を抽出するための正規表現
	private static final Pattern TITLE_SPLIT_PATTERN = Pattern.compile("^(.*?)(?:<br>\\s*\\(?(.*?)\\)?)?$");
	
	// コンストラクタインジェクション
	/*Validatorの手動注入
	 * Validation.buildDefaultValidatorFactory().getValidator()を使用してValidatorを取得し、手動でBean Validationを実行している。
	 * JSONから読み込んだ Movie オブジェクトに対して、通常のWebフォーム入力時と同じバリデーションルールを適用できる。
	 * */
	public MovieImportService(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();
	}
	
	// インポート結果を返すための内部クラス
	/*ImportResultクラスの導入
	 * インポートの成功件数、スキップされた件数、スキップされた映画の詳細を保持する。
	 * インポート処理結果を明確に飛び出し元に伝えることができる。
	 * */
    public static class ImportResult {
        private int importedCount;
        private int skippedCount;
        private List<SkippedMovieInfo> skippedMovies;

        public ImportResult() {
            this.importedCount = 0;
            this.skippedCount = 0;
            this.skippedMovies = new ArrayList<>();
        }

        public void incrementImported() { this.importedCount++; }
        public void incrementSkipped() { this.skippedCount++; }
        public void addSkippedMovie(String title, String discNo, List<String> errors) {
            this.skippedMovies.add(new SkippedMovieInfo(title, discNo, errors));
        }

        public int getImportedCount() { return importedCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<SkippedMovieInfo> getSkippedMovies() { return skippedMovies; }

        public static class SkippedMovieInfo {
            private String title;
            private String discNo;
            private List<String> errors;

            public SkippedMovieInfo(String title, String discNo, List<String> errors) {
                this.title = title;
                this.discNo = discNo;
                this.errors = errors;
            }

            public String getTitle() { return title; }
            public String getDiscNo() { return discNo; }
            public List<String> getErrors() { return errors; }
        }
    }
	
    @Transactional // インポート全体をトランザクション化
	public ImportResult ImportMoviesFromJson(InputStream jsonInputStream) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		// 未知のプロパティを無視する設定を追加（title/discNo/year/actors以外は無視）
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		// JSONデータをそのままMovieオブジェクトのリストとして読み込む
		// Movieクラスに定義されていないJSONのキーは自動的に無視
		List<Movie> rawMovies;
		try {
			// JSONデータをそのままMovieオブジェクトのリストとして取り込む
			// Movieクラスに定義されていないJSONキーは自動的に無視
			rawMovies = objectMapper.readValue(jsonInputStream, objectMapper.getTypeFactory().constructCollectionType(List.class, Movie.class));
		} catch (IOException e){
			System.err.println("ERROR: JSONファイルの読み込みパース中にエラーが発生しました。: " + e.getMessage());
			throw e; // JSONパースエラーはここで捕捉して呼び出し元に伝える
		}
		
		ImportResult result = new ImportResult();
		
		for (Movie movie : rawMovies) {
			// =======================================================
            // 1. 各フィールドのデータ整形とルール適用
            // =======================================================
			
			// titleの整形（@NotBlankが効くように前後の空白除去）
			if (movie.getTitle() != null) {
				String fullTitle = movie.getTitle().trim();
				Matcher matcher = TITLE_SPLIT_PATTERN.matcher(fullTitle);
				if (matcher.matches()) {
					movie.setTitle(matcher.group(1).trim()); // 邦題
					String originalTitle = matcher.group(2); // 原題
					if (originalTitle != null && !originalTitle.trim().isEmpty()) {
						movie.setOriginalTitle(originalTitle.trim());
					} else {
						movie.setOriginalTitle(null); // 空文字の場合はnull
					}
				} else {
					movie.setTitle(fullTitle); // マッチしない場合はそのまま邦題に設定
					movie.setOriginalTitle(null); // 原題はnull
				}
			} else {
				movie.setTitle(""); // nullの場合は空文字列に設定し、@NotBlankでエラーにさせる
				movie.setOriginalTitle(null); // 原題はnull
			}
			
			// discNoの整形（@NotBlankと@Patternが効くように前後の空白文字除去）
			if (movie.getDiscNo() != null) {
				movie.setDiscNo(movie.getDiscNo().trim());
			} else {
				movie.setDiscNo(""); // nullの場合は空文字列に設定し、@NotBlankでエラーにさせる
			}
			
			// yearの整形
			// 4桁の西暦以外は""として保存
			String rawYear = movie.getYear();
			if (rawYear != null) {
				String trimmedYear = rawYear.trim();
				// 数字以外の文字を除去して、純粋な数字部分を抽出
				String numericPart = trimmedYear.replaceAll("[^0-9]", "");
				
				// 正規表現にマッチするか、または完全に空文字か
				if (numericPart.matches("^(19|20)[0-9]{2}$")) {
					movie.setYear(numericPart); // 有効な年
				} else if (trimmedYear.isEmpty()) {
					movie.setYear(""); // 元が空文字列ならそのまま空文字列
				} else {
					System.err.println
					("WARN: 映画タイトル ' " + movie.getTitle() + " ' の公開年が不正です。' " + rawYear + " ' -> 無効なため空文字列に設定します。");
					movie.setYear(""); // 不正な年を空文字列に設定
				}
			} else {
				movie.setYear("");
			}
			
			// actorsの整形
			// nullの場合""として保存
			if (movie.getActors() == null) {
				movie.setActors("");
			} else {
				// actorsには特別なルールがないため、前後の空白だけ除去しておく
				movie.setActors(movie.getActors().trim());
			}
			
			// =======================================================
            // 2. Movieエンティティのバリデーション（Bean Validation）
            // =======================================================
			
			// ここで、整形されたmovieオブジェクトに対してバリデーションを実行
			Set<ConstraintViolation<Movie>> violations = validator.validate(movie);
			
			if (!violations.isEmpty()) {
				List<String> errors = new ArrayList<>();
				for (ConstraintViolation<Movie> violation : violations) {
					errors.add(violation.getPropertyPath() + "：" 
							+ violation.getMessage() + " (値: '" 
							+ (violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : "null") + "')");
				}
				result.addSkippedMovie(movie.getTitle(), movie.getDiscNo(), errors);
				result.incrementSkipped();
				System.err.println
				("SKIPPING: 映画タイトル '" + movie.getTitle() + "' (Disc No: '" + movie.getDiscNo() + "') は以下の理由でスキップされました:");
				errors.forEach(System.err::println); // コンソールにもエラーを出力
				continue; // 次の映画へ
			}
			
			// =======================================================
            // 3. データベースへの保存
            // =======================================================
			try {
				movieRepository.save(movie);
				result.incrementImported();
				System.out.println("IMPORTED: " + movie.getTitle() + " (Disc No: " + movie.getDiscNo() + ")");
			} catch (Exception e) {
				// データベース保存時のエラー
				result.incrementSkipped();
				List<String> errors = new ArrayList<>();
				errors.add("データベース保存中にエラーが発生しました：" + e.getMessage());
				result.addSkippedMovie(movie.getTitle(), movie.getDiscNo(), errors);
				System.err.println
				("ERROR: 映画タイトル '" + movie.getTitle() + "' (Disc No: '" + movie.getDiscNo() + "') のデータベース保存中にエラーが発生しました: " + e.getMessage());
				e.printStackTrace(); // 例外の詳細をログに出力
				// 例外の詳細をログに出力するなど、適切なエラーハンドリングを行う
			}
		}
		System.out.println("--- インポート結果 ---");
        System.out.println("インポート成功件数: " + result.getImportedCount());
        System.out.println("スキップされた件数: " + result.getSkippedCount());
        return result; // 結果を返す
	}
}
