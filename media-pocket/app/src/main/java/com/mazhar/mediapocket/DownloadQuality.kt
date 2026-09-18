package com.mazhar.mediapocket

enum class DownloadQuality(val height: Int, val label: String, val detail: String) {
    BEST(-1, "Best", "Original"),
    UHD_4K(2160, "4K", "2160p"),
    QHD_2K(1440, "2K", "1440p"),
    FHD(1080, "1080p", "Full HD"),
    HD(720, "720p", "HD"),
    SD(480, "480p", "SD");

    companion object {
        fun fromHeight(height: Int): DownloadQuality = entries.firstOrNull { it.height == height } ?: BEST
    }
}
