package com.example.aiskipads

import android.graphics.Rect

class ADScanner {

    data class Coordinate(val x: Int, val y: Int)
    data class ScanResult(val coordinate: Coordinate, val text: String)

    // Keywords to look for.
    private val keywords = listOf(
        // Chinese common terms
        "跳过", "关闭", "取消", "跳过广告", "点击跳过", "不感兴趣", "跳过在此", "倒计时",
        
        // English common terms
        "Skip", "Close", "Cancel", "Skip Ad", "Click to skip", "Not interested", "No thanks", "Dismiss",
        
        // Countdown patterns
        "5s", "4s", "3s", "2s", "1s",
        "5秒", "4秒", "3秒", "2秒", "1秒", "秒",

        // Content descriptions for icons/buttons
        "关闭按钮", "close_icon", "close_btn", "closeButton", 
        "skip_ad", "skip_icon", "skip_btn", "skipButton",
        "iv_close", "iv_skip", "img_close", "img_skip",
        "btn_close", "btn_skip", "btn_cancel"
    )

    private val countdownPattern = Regex("\\d+\\s*[sS秒]")

    /**
     * Detects if any text in the list matches keywords and returns the coordinate.
     * @param textList List of text found on screen
     * @param boundsMap Map of text to its screen location
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     */
    fun detectAndClick(textList: List<String>, boundsMap: Map<String, Rect>, screenWidth: Int, screenHeight: Int): ScanResult? {
        var bestResult: ScanResult? = null
        var maxScore = 0

        for (text in textList) {
            val rect = boundsMap[text] ?: continue
            val score = calculateScore(text, rect, screenWidth, screenHeight)
            
            if (score > maxScore && score >= 75) {
                maxScore = score
                bestResult = ScanResult(Coordinate(rect.centerX(), rect.centerY()), text)
            }
        }
        return bestResult
    }

    private fun calculateScore(text: String, rect: Rect, screenWidth: Int, screenHeight: Int): Int {
        var score = 0

        // 1. Semantic Scoring with Regex
        if (text.contains("跳过") || text.contains("Skip", ignoreCase = true) || text.contains("不再显示")) {
            score += 70
        } else if (countdownPattern.containsMatchIn(text)) {
            score += 50
        }

        // 2. Location Scoring (Percentage)
        // Top 25% area
        if (rect.top < screenHeight * 0.25) {
            score += 20
            // Top-Right area (Left > 50% width)
            if (rect.left > screenWidth * 0.5) {
                score += 10
            }
        }

        // 3. Size Filtering
        val width = rect.width()
        val height = rect.height()
        if (width in 50..400 && height in 30..200) {
            score += 10
        }

        // 4. Text Length Penalty
        if (text.length > 6) {
            score -= 50
        }

        return score
    }

    private fun matchesKeyword(text: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
