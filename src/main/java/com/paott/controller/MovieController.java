package com.paott.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.paott.model.Movie;
import com.paott.service.MovieImportService;
import com.paott.service.MovieService;

/*<リクエストの処理、モデルへのデータの受け渡しなどを行うコントローラークラス>
 * 説明の+をクリックすると説明が展開
 * */

/*<このコントローラークラスの説明>
 * 
 * MovieControllerは、Webページからのリクエスト（映画検索、編集画面の表示、編集データの送信、削除など）と、
 * RESTfulAPIからのリクエスト（特定の映画情報の取得、映画情報の更新）の両方を処理する。
 * MovieServiceを介してビジネスロジック（データの検索、更新、削除）を実行し、処理結果に応じて適切なビューを
 * 表示したり、ＨＴＴＰレスポンスを返したりする。
 * 
 * @Controller : このクラスがコントローラーであることをspringに伝える
 * 
 * @RequestMapping("/movies") : このコントローラー内のすべてのメソッドが /movies 
 * というベースパスを持つURLにマッピングされる（ /movies/search など）
 * 
 * @Autowired : MovieService 型の Bean（Spring によって管理されるオブジェクト）を、
 * このコントローラーの movieService フィールドに自動的に注入（割り当て）する
 * 
 * */

@Controller 
@RequestMapping("/movies")
public class MovieController {
	@Autowired
	private MovieService movieService;
	@Autowired
	private MovieImportService movieImportService;
	
	// コンストラクタインジェクション
	public MovieController(MovieService movieService, MovieImportService movieImportService) {
		this.movieService = movieService;
		this.movieImportService = movieImportService;
	}
	
	// 映画検索 ********************************************************************************
	@GetMapping("/search") 
	public String searchMovies(@RequestParam(name = "titleKeyword", required = false) String titleKeyword,
												@RequestParam(name = "actorKeyword1",  required = false) String actorKeyword1,
												@RequestParam(name = "actorKeyword2", required = false) String actorKeyword2,
												Model model) {
		// 部分一致検索のための変数
		//String titleLikeKeyword = (titleKeyword != null && !titleKeyword.isEmpty()) ? "%" + titleKeyword + "%" : null;
		
		// titleKeywordはそのままサービスに渡し、サービス側でLIKE検索の％を付与
		List<Movie> results = movieService.searchMovies(titleKeyword, actorKeyword1, actorKeyword2);
		model.addAttribute("results", results);
		return "search";
	}
	// 映画検索 ********************************************************************************
	
	
	// 編集画面表示 *****************************************************************************
	@PostMapping("/edit")
	public String showEditForm(@RequestParam(name = "selectedMovies", required = false)
												List<Integer> selectedMovies,
												Model model, RedirectAttributes redirectAttribute) {
		if (selectedMovies == null || selectedMovies.isEmpty()) {
			redirectAttribute.addFlashAttribute("errorMessage", "編集する映画を選択してください。");
			return "redirect:/movies/search";
		}
		if (selectedMovies.size() > 1) {
			redirectAttribute.addFlashAttribute("errorMessage", "一度に編集できる映画は１件のみです。");
			return "redirect:/movies/search";
		}
		Optional<Movie> movieOptional = movieService.getMovieByIdWithActors(selectedMovies.get(0));
		if (!movieOptional.isPresent()) {
			redirectAttribute.addFlashAttribute("errorMessage", "選択された映画が見つかりませんでした。");
			return "redirect:/movies/search";
		}
		
		Movie movie = movieOptional.get();
		// originalTitle（原題）が null の場合、空文字に設定（Thymeleafで扱いやすくするため）
		if (movie.getOriginalTitle() == null) {
			movie.setOriginalTitle("");
		}
		
		model.addAttribute("movie", movie);
		return "edit";
	}
	// 編集画面表示 *****************************************************************************
	
	
	// 編集画面 ********************************************************************************
	@PostMapping("/update/{movieId}")
	public String updateMovie(@PathVariable int movieId,
												@Valid @ModelAttribute("movie") Movie movie,
												BindingResult result,
												Model model,
												RedirectAttributes redirectAttributes) {
		// JavaScriptから受け取ったactors文字列を整形
		//String originalActors = movie.getActors();
		String creanedActors = cleanAndFormatActors(movie.getActors());
		movie.setActors(creanedActors);
		
		// 原題が空文字の場合にnullに変換（DBにnullとして保存するため）
		if (movie.getOriginalTitle() != null && movie.getOriginalTitle().trim().isEmpty()) {
			movie.setOriginalTitle(null);
		} else if (movie.getOriginalTitle() != null) {
			movie.setOriginalTitle(movie.getOriginalTitle().trim()); // 前後の空白を削除
		}
		
		if (result.hasErrors()) {
			// バリデーションエラー時でもフォームの値を保持し、バリデーションエラーでフォームに戻る場合、JavaScriptが再生成できるよう
			// movie.actorsにクリーンな値が入っていることを確認（cleanedActorsでセット済み）
			model.addAttribute("movie", movie);
			// 原題が null の場合は空文字に設定（HTMLで扱いやすくするため）
			if (movie.getOriginalTitle() == null) {
				movie.setOriginalTitle("");
			}
			return "edit"; // バリデーションエラーがあれば、edit.htmlを再表示
		}
		try {
			Movie updatedMovie = movieService.updateMovie(movieId, movie); // サービスメソッドに渡す
			if (updatedMovie != null) {
				redirectAttributes.addFlashAttribute("successMessage", "映画を更新しました。"); // リダイレクトメッセージ
				return "redirect:/movies/search"; // 検索画面にリダイレクト
			} else {
				model.addAttribute("errorMessage", "映画情報の更新に失敗しました。"); // エラーメッセージ
				// 更新失敗時もフォームの値を保持
				model.addAttribute("movie", movie);
				// 原題が null の場合は空文字に設定
				if (movie.getOriginalTitle() == null) {
					movie.setOriginalTitle("");
				}
				return "edit"; // 更新失敗時はedit.htmlを再表示
			} 
		} catch(Exception e) {
			model.addAttribute("errorMessage", "映画の更新中にエラーが発生しました: " + e.getMessage());
			model.addAttribute("movie", movie); // エラー時もフォームの値を保持
			// 原題が null の場合は空文字に設定
			if (movie.getOriginalTitle() == null) {
				movie.setOriginalTitle("");
			}
			e.printStackTrace(); // サーバーログにスタックトレースを出力
			return "edit";
		}
	}
	// 編集画面 ********************************************************************************
	
	
	// 削除処理 ********************************************************************************
	@PostMapping("/delete")
	public String deleteMovies(@RequestParam(name = "selectedMovies", required = false)
												List<Integer> selectedMovies,
												RedirectAttributes redirectAttributes) {
		if (selectedMovies == null || selectedMovies.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "削除する映画を選択してください。");
			return "redirect:/movies/search";
		}
		movieService.deleteMovies(selectedMovies);
		redirectAttributes.addFlashAttribute("successMessage", "選択した映画を削除しました。");
		return "redirect:/movies/search";
	}
	// 削除処理 ********************************************************************************
	
	
	// 追加画面表示 *****************************************************************************
	@GetMapping("/add")
	public String showAddForm(Model model) {
		Movie movie = new Movie();
		movie.setOriginalTitle(""); // 初期表示時に原題を空文字に設定
		model.addAttribute("movie", movie); // Movie をモデルに追加
		// 新規追加時は空の出演者リストを渡す
		model.addAttribute("initialActors", new ArrayList<String>());
		return "add";
	}
	// 追加画面表示 *****************************************************************************
	
	
	// 追加処理 ********************************************************************************
	@PostMapping("/add")
	public String addMovie(@Valid @ModelAttribute Movie movie, BindingResult result, Model model,
											@RequestParam(name = "actors[]", required = false) List<String> actorsList,
											@RequestParam(name = "forceSave", defaultValue = "false") boolean forceSave,
											RedirectAttributes redirectAttributes) {
		
		// 出演者リストを"／”区切りの文字列に変換してMovieオブジェクトに設定
		if (actorsList != null && !actorsList.isEmpty()) {
			String rawActorsString = actorsList.stream()
					.map(String::trim)
					.collect(Collectors.joining("／"));
			movie.setActors(cleanAndFormatActors(rawActorsString)); // 整形を適用
		} else {
			movie.setActors(null); // 出演者がいない場合は null をセット
		}
		
		// 原題が空文字の場合にnullに変換（DBにnullとして保存するため）
		if (movie.getOriginalTitle() != null && movie.getOriginalTitle().trim().isEmpty()) {
			movie.setOriginalTitle(null);
		} else if (movie.getOriginalTitle() != null) {
			movie.setOriginalTitle(movie.getOriginalTitle().trim()); // 前後の空白を削除
		}
		
		if (result.hasErrors()) {
			// バリデーションエラーの場合でも、入力された出演者リストを保持
			model.addAttribute("initialActors", actorsList);
			// 原題がnullの場合は空文字に設定
			if (movie.getOriginalTitle() == null) {
				movie.setOriginalTitle("");
			}
			return "add"; // バリデーションエラーであれば add.html を再表示
		}
		
		Movie savedMovie = movieService.addMovie(movie, forceSave);
		
		if (savedMovie != null) {
			// 正常に保存された場合（新規タイトル、または既存タイトルで forceSave = true の場合）
			redirectAttributes.addFlashAttribute("successMessage", "映画を追加しました。");
			return "redirect:/movies/search"; // 検索画面にリダイレクト
		} else {
			// 同じタイトルが存在し、forceSave = false の場合
			model.addAttribute("warningMessage", "同じタイトルが既に登録されています。登録しますか？");
			model.addAttribute("movie", movie); // フォームの入力値を保持
			// 入力された出演者リストをモデルに保持
			model.addAttribute("initialActors", actorsList);
			// 原題がnullの場合は空文字に設定
			if (movie.getOriginalTitle() == null) {
				movie.setOriginalTitle("");
			}
			return "add";
		}
	}
	
	// 追加処理 ********************************************************************************
	
	
	// 映画情報取得API **************************************************************************
	@GetMapping("/{movieId}")
	@ResponseBody
	public ResponseEntity<Movie> getMovie(@PathVariable int movieId) {
		Movie movie = movieService.getMovieById(movieId);
		if (movie == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(movie);
	}
	// 映画情報取得API **************************************************************************
	
	
	// 映画情報更新API **************************************************************************
	@PutMapping("/{movieId}")
	public ResponseEntity<Movie> updateMovie(@PathVariable int movieId, @RequestBody Movie movie) {
		// REST API からの更新時も原題のトリムとnull変換を行う
		if (movie.getOriginalTitle() != null && movie.getOriginalTitle().trim().isEmpty()) {
			movie.setOriginalTitle(null);
		} else if (movie.getOriginalTitle() != null) {
			movie.setOriginalTitle(movie.getOriginalTitle().trim());
		}
		Movie updatedMovie = movieService.updateMovie(movieId, movie);
		if (updatedMovie == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(updatedMovie);
	}
	// 映画情報更新API **************************************************************************
	
	
	// JSONアップロードAPI **********************************************************************
	// ファイルアップロードフォームを表示するエンドポイント
	@GetMapping("/import")
	public String showImportForm() {
		return "importForm";
	}
	
	@PostMapping("/import/upload")
	public String jsonFileUpload(@RequestParam("file") MultipartFile file, Model model) {
		if (file.isEmpty()) {
			model.addAttribute("errorMessage", "ファイルが選択されていません。");
			return "importForm";
		}
		
		if (!"application/json".equals(file.getContentType())) {
			model.addAttribute("errorMessage", "JSONファイルのみをアップロードしてください。");
			return "importForm";
		}
		
		try {
			// ImportServiceFromJson の戻り値としてインポート結果を受け取る
			MovieImportService.ImportResult importResult = movieImportService.ImportMoviesFromJson(file.getInputStream());
			model.addAttribute("importResult", importResult); // 結果をモデルに追加
			
			// 成功メッセージとエラーメッセージを分離して設定
			if (importResult.getSkippedCount() == 0) {
				model.addAttribute("successMessage", "映画データのインポートが完了しました。詳細はサーバーログを確認してください。");
			} else {
				model.addAttribute("errorMessage", "映画データのインポートが完了しましたが、一部スキップされました。詳細は以下をご確認ください。"); // エラーメッセージにキー変更
			}
			// 成功メッセージは通常、インポートサービスからのログに依存
			return "importForm"; // 処理後も同じフォームに戻る
		} catch (IOException e) {
			model.addAttribute("errorMessage", "ファイル読み込み中にエラーが発生しました: " + e.getMessage());
			e.printStackTrace();
			return "importForm";
		} catch (Exception e) { // その他の予期せぬエラー
			model.addAttribute("errorMessage", "インポート中に予期せぬエラーが発生しました: " + e.getMessage());
			e.printStackTrace();
			return "importForm";
		}
	}
	// JSONアップロードAPI **********************************************************************
	
	// ヘルパーメソッド
	private String cleanAndFormatActors(String actorsInput) {
		if (actorsInput == null || actorsInput.trim().isEmpty()) {
			return "";
		}
		// カンマ、スラッシュ（半角/全角）、空白文字（スペース、タブ、改行）で分割
		String tempFormatted = actorsInput.replaceAll("[,、/／\\s\n\r]+", "|");
		List<String> parts = Arrays.stream(tempFormatted.split("\\|"))
				   .map(String::trim)
				   .filter(s -> !s.isEmpty())
				   .collect(Collectors.toList());
		// 全角主ラッシュ「／」で結合
		return String.join("／", parts);
	}
}
