package com.paott.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paott.model.Movie;
import com.paott.repository.MovieRepository;

/*<ビジネスロジック（データの検索、更新、削除）の処理をするサービスクラス>
 *  説明の+をクリックすると説明が展開
 * */

@Service
public class MovieService {
	
	@Autowired
	private MovieRepository movieRepository;
	
	
	// 映画検索API ********************************************************************************
    @Transactional(readOnly = true)
    public List<Movie> searchMovies(String titleKeyword, String actorKeyword1, String actorKeyword2) {
        // null または空文字の場合は、検索条件として無視する
        boolean hasTitleKeyword = (titleKeyword != null && !titleKeyword.isEmpty());
        boolean hasActorKeyword1 = (actorKeyword1 != null && !actorKeyword1.isEmpty());
        boolean hasActorKeyword2 = (actorKeyword2 != null && !actorKeyword2.isEmpty());

        if (hasTitleKeyword && hasActorKeyword1 && hasActorKeyword2) {
            return movieRepository.findByTitleOrOriginalTitleAndActorsContainingIgnoreCaseAndActorsContainingIgnoreCase(titleKeyword, actorKeyword1, actorKeyword2);
        } else if (hasTitleKeyword && hasActorKeyword1) {
            return movieRepository.findByTitleOrOriginalTitleAndActorsContainingIgnoreCase(titleKeyword, actorKeyword1);
        } else if (hasTitleKeyword && hasActorKeyword2) {
            // actorKeyword2のみで検索する場合は、actorKeyword1を空として渡す（または同じクエリで）
            return movieRepository.findByTitleOrOriginalTitleAndActorsContainingIgnoreCase(titleKeyword, actorKeyword2); // actorKeyword2をactorKeywordとして利用
        } else if (hasActorKeyword1 && hasActorKeyword2) {
            return movieRepository.findByActorsContainingIgnoreCaseAndActorsContainingIgnoreCase(actorKeyword1, actorKeyword2);
        } else if (hasTitleKeyword) {
            return movieRepository.findByTitleOrOriginalTitleContainingIgnoreCase(titleKeyword);
        } else if (hasActorKeyword1) {
            return movieRepository.findByActorsContainingIgnoreCase(actorKeyword1);
        } else if (hasActorKeyword2) {
            return movieRepository.findByActorsContainingIgnoreCase(actorKeyword2); // actorKeyword2をactorKeywordとして利用
        } else {
            return movieRepository.findAll();
        }
    }
    // 映画検索API ********************************************************************************

	
	// 映画情報取得API ************************************************
	public Movie getMovieById(int movieId) {
		return movieRepository.findById(movieId).orElse(null);
	}
	// 映画情報取得API ************************************************
	
	
	// 映画情報編集API***************************************************************************
	@Transactional(readOnly = true) // 読み取り専用
	public Optional<Movie> getMovieByIdWithActors(int movieId) {
		return movieRepository.findById(movieId);
	}
	// 映画情報編集API***************************************************************************
	
	
	// 映画情報更新API ***********************************************************************************
	@Transactional
	public Movie updateMovie(int movieId, Movie movie) { // パラメータを追加
		// 現在のトランザクションが完了するまで他のトランザクションからの書き込みをブロックするメソッドを適用
		Optional<Movie> existingMovieOptional = movieRepository.findByIdForUpdate(movieId);
		Movie existingMovie = existingMovieOptional.orElse(null);
		
		if (existingMovie == null) {
			return null;
		}
		existingMovie.setTitle(movie.getTitle());
		existingMovie.setOriginalTitle(movie.getOriginalTitle());
		existingMovie.setYear(movie.getYear());
		existingMovie.setDiscNo(movie.getDiscNo());
		existingMovie.setActors(movie.getActors());

		return movieRepository.save(existingMovie); // 映画情報の更新と関連付けの保存
	}
	// 映画情報更新API ***********************************************************************************
	
	
	// 映画情報削除API *************************************************************************
	@Transactional
	public void deleteMovies(List<Integer> movieIds) {
		for (Integer movieId : movieIds) {
			movieRepository.deleteById(movieId);
		}
	}
	// 映画情報削除API *************************************************************************
	
	
	// 映画情報追加API ***************************************************************************************
	public List<Movie> findMovieByTitle(String title) {
		return movieRepository.findByTitle(title);
	}
	
	@Transactional
	public Movie addMovie(Movie movie, boolean forceSave) {
		
		/*タイトルで既存の映画を検索 
		 * ここでの findByTitle は、警告を出すための「重複チェック」としてのみ機能
		 * 同じタイトルがあっても、forceSave = true なら新規保存をする*/
		List<Movie> existingMovie = movieRepository.findByTitle(movie.getTitle());
		
		if (!existingMovie.isEmpty() && !forceSave) {
			// 同じタイトルが存在し、かつ強制保存「はい」でない場合は、null を返してコントローラーに警告を表示させる
			return null;
		}
		/*上記の if ブロックに入らなかった場合（新規タイトル、または既存タイトルだが forceSave = true の場合）
		 * 常に新しいレコードとして保存する
		 * movie オブジェクトには、コントローラーで設定された actors 情報が既に含まれている
		 * */
		return movieRepository.save(movie);
	}
	// 映画情報追加API ***************************************************************************************
	
	
	// JSONファイルアップロードに関するAPI
	@Transactional
	public void saveAllMovies(List<Movie> movies) {
		movieRepository.saveAll(movies);
	}
	
	
	// 必要に応じて出演者関連のメソッドを追加
}

