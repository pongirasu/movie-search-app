package com.paott.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paott.model.Movie;

/*
 * データベースへのアクセスを担うリポジトリ
 * */

//映画情報取得、更新のAPI
@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer> {

	// 検索クエリ //
	
	// 検索クエリを修正：title または originalTitle のどちらかを含んでいればヒット
    // 邦題と原題を結合して検索するクエリを汎用的に利用
    @Query("SELECT DISTINCT m FROM Movie m WHERE (UPPER(m.title) LIKE UPPER(CONCAT('%', :titleKeyword, '%')) OR UPPER(m.originalTitle) LIKE UPPER(CONCAT('%', :titleKeyword, '%')))")
    List<Movie> findByTitleOrOriginalTitleContainingIgnoreCase(@Param("titleKeyword") String titleKeyword);

    // 俳優検索のクエリは変更なし
    @Query("SELECT DISTINCT m FROM Movie m WHERE UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword, '%'))")
    List<Movie> findByActorsContainingIgnoreCase(@Param("actorKeyword") String actorKeyword);

    // タイトルまたは原題、かつ俳優1、俳優2 の条件
    @Query("SELECT DISTINCT m FROM Movie m " +
           "WHERE (UPPER(m.title) LIKE UPPER(CONCAT('%', :titleKeyword, '%')) OR UPPER(m.originalTitle) LIKE UPPER(CONCAT('%', :titleKeyword, '%'))) " +
           "AND UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword1, '%')) " +
           "AND UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword2, '%'))")
    List<Movie> findByTitleOrOriginalTitleAndActorsContainingIgnoreCaseAndActorsContainingIgnoreCase(
        @Param("titleKeyword") String titleKeyword,
        @Param("actorKeyword1") String actorKeyword1,
        @Param("actorKeyword2") String actorKeyword2
    );
    
 // タイトルまたは原題、かつ俳優1 の条件
    @Query("SELECT DISTINCT m FROM Movie m " +
           "WHERE (UPPER(m.title) LIKE UPPER(CONCAT('%', :titleKeyword, '%')) OR UPPER(m.originalTitle) LIKE UPPER(CONCAT('%', :titleKeyword, '%'))) " +
           "AND UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword, '%'))")
    List<Movie> findByTitleOrOriginalTitleAndActorsContainingIgnoreCase(
        @Param("titleKeyword") String titleKeyword,
        @Param("actorKeyword") String actorKeyword
    );
    
    // 俳優１，俳優２の条件
    @Query("SELECT DISTINCT m FROM Movie m WHERE UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword1, '%')) AND UPPER(m.actors) LIKE UPPER(CONCAT('%', :actorKeyword2, '%'))")
    List<Movie> findByActorsContainingIgnoreCaseAndActorsContainingIgnoreCase(@Param("actorKeyword1") String actorKeyword1, @Param("actorKeyword2") String actorKeyword2);

    // すべて取得するクエリ
    @Query("SELECT DISTINCT m FROM Movie m")
    List<Movie> findAll(); 

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT m FROM Movie m WHERE m.movieId = :movieId")
    Optional<Movie> findByIdForUpdate(@Param("movieId") int movieId);

    @Query("SELECT DISTINCT m FROM Movie m WHERE m.movieId = :movieId")
    Optional<Movie> findById(@Param("movieId") int movieId);

    @Query("SELECT DISTINCT m FROM Movie m WHERE m.title = :title")
    List<Movie> findByTitle(@Param("title") String title);
}