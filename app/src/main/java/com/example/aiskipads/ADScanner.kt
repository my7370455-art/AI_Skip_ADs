package com.example.aiskipads

import android.graphics.Rect

class ADScanner {

    data class Coordinate(val x: Int, val y: Int)

    // Keywords to look for.
    private val keywords = listOf(
        "跳过", "Skip", "关闭", "Close", "跳过广告", "Skip Ad", 
        "5s", "4s", "3s"
    )

    /**
     * Detects if any text in the list matches keywords and returns the coordinate.
     * @param textList List of text found on screen
     * @param boundsMap Map of text to its screen location
     */
    fun detectAndClick(textList: List<String>, boundsMap: Map<String, Rect>): Coordinate? {
        for (text in textList) {
            if (matchesKeyword(text)) {
                val rect = boundsMap[text]
                if (rect != null) {
                    return Coordinate(rect.centerX(), rect.centerY())
                }
            }
        }
        return null
    }

    private fun matchesKeyword(text: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
