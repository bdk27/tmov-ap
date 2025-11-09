package com.brian.tmov.service;

public interface TmdbGetImageService {

    /**
     * 取得一個完整的圖片 URL
     * @return 完整的 URL，例如 "https://image.tmdb.org/t/p/w500/path.jpg"
     */
    String getFullImageUrl(String path, String size);

    /**
     * 取得一個預設尺寸 (w500) 的海報 URL
     */
    String getDefaultPosterUrl(String posterPath);

    /**
     * 取得一個預設尺寸 (w1280) 的背景圖 URL
     */
    String getDefaultBackdropUrl(String backdropPath);

    /**
     * 取得一個預設尺寸 (h632) 的個人頭像 URL
     */
    String getDefaultProfileUrl(String profilePath);
}
