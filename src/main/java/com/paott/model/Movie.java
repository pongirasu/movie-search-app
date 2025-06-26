package com.paott.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Entity
@Table(name = "movies")
// 映画情報を格納するためのデータモデルクラス
public class Movie {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // 主キーの自動生成
	private int movieId; // 主キーとなるフィールド
	
	// 映画タイトル（邦題）：入力必須
	@NotBlank(message = "映画タイトルは必須です。")
	@Size(max = 255, message = "タイトルは255文字以内で入力してください。")
	@Column(name = "title", nullable = false)
	private String title;
	
	// 映画タイトル（原題）：入力任意
	@Size(max = 255, message = "原題は255文字以内で入力してください。")
	@Column(name = "original_title", nullable = true)
	private String originalTitle;
	
	// nullを許容するための正規表現
	@Pattern(regexp = "^(19|20)[0-9]{2}$|^$", message = "公開年は19か20で始まる4桁の西暦で入力するか、空欄にしてください。")
	@Column(name = "year", nullable = true)
	private String year;
	
	@NotBlank(message = "Noは必須です。")
	@Pattern(regexp = "^[A-Z][0-9]{4}$", message = "Noは先頭1文字は大文字英字、残り4文字は数字で入力してください。")
	@Column(name = "disc_no", nullable = false)
	private String discNo;
	
	@Size(max = 1000, message = "出演者は1000文字以内で入力してください。")
	@Column(name = "actors", nullable = true)
	private String actors;
	
	//  コンストラクタ
	public Movie() {}
	
	public Movie(int movieId, String title) {
		this.movieId = movieId;
		this.title = title;
	}
	
	public Movie(int movieId, String title, String originalTitle, String discNo, String year, String actors) {
		this.movieId = movieId;
		this.title = title;
		this.originalTitle = originalTitle;
		this.discNo = discNo;
		this.year = year;
		this.actors = actors;
	}
	
	public int getMovieId() {
		return movieId;
	}
	
	public void setMovieId(int movieId) {
		this.movieId = movieId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getOriginalTitle() {
		if (this.originalTitle == null || this.originalTitle.trim().isEmpty()) {
			return ""; // null または空文字列、スペースの場合は空文字列を返す
		}
		
		// 正規表現を使用し、文字列の先頭と末尾にある半角/全角の括弧を削除
		String cleanedTitle = this.originalTitle.trim();
		
		cleanedTitle = cleanedTitle.replaceAll("^[\\s\\(（]+", ""); // 先頭の括弧と空白を削除
		cleanedTitle = cleanedTitle.replaceAll("[\\s\\)）]+$", ""); // 末尾の括弧と空白を削除
		
		return cleanedTitle;
		
	}
	
	public void setOriginalTitle(String originalTitle) {
		this.originalTitle = originalTitle;
	}
	
	public String getYear() {
		return year;
	}
	
	public void setYear(String year) {
		this.year = year;
	}
	
	public String getDiscNo() {
		return discNo;
	}
	
	public void setDiscNo(String discNo) {
		this.discNo = discNo;
	}
	
	public String getActors() {
		return actors;
	}
	
	public void setActors(String actors) {
		this.actors = actors;
	}
}
