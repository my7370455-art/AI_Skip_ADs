package com.example.aiskipads

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipService : AccessibilityService() {

    private val adScanner = ADScanner()
    
    // Anti-infinite loop state
    private val clickHistory = mutableMapOf<String, Int>()
    private val lastClickTimeMap = mutableMapOf<String, Long>()
    private val blockedTexts = mutableMapOf<String, Long>()
    private val nodeCache = mutableMapOf<String, AccessibilityNodeInfo>()
    private var lastClickTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AdSkipService", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        Log.v("AdSkipService", "收到事件: ${event.eventType}")

        // Try getting root node, fallback to event source
        val rootNode = rootInActiveWindow ?: event.source ?: return
        
        // Screen Metrics
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
            
            // 3. Recursive traversal to extract text and bounds
            val textList = mutableListOf<String>()
            val boundsMap = mutableMapOf<String, Rect>()
            nodeCache.clear() // Clear cache before new traversal
            collectText(rootNode, textList, boundsMap, screenWidth, screenHeight)

            // Filter blocked texts
            val now = System.currentTimeMillis()
            val validTextList = textList.filter { 
                val blockUntil = blockedTexts[it] ?: 0L
                now > blockUntil
            }
            
            // 4. Get coordinates from scanner
            val result = adScanner.detectAndClick(validTextList, boundsMap, screenWidth, screenHeight)
            
            // 5. Build gesture and click
            if (result != null) {
                val text = result.text

                // Click Cooldown check (Increased to 2000ms)
                if (now - lastClickTimestamp < 2000) {
                    return
                }
                
                // Check for infinite loop
                if (shouldBlock(text, now)) {
                    Log.w("AdSkipService", "Blocking repetitive text: $text")
                    return
                }

                var finalX = result.coordinate.x
                var finalY = result.coordinate.y

                // Coordinate fine-tuning
                if (finalX < 20) finalX += 10
                else if (finalX > screenWidth - 20) finalX -= 10
                
                if (finalY < 20) finalY += 10
                else if (finalY > screenHeight - 20) finalY -= 10

                // Log only on valid click (as requested)
                Log.d("AdSkipService", "Clicking at $finalX, $finalY")
                
                // Dual Action: Gesture + Node Action
                clickAt(finalX, finalY)
                
                // Perform node action if available
                val targetNode = nodeCache[text]
                if (targetNode != null) {
                    val performed = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("AdSkipService", "Node action performed: $performed")
                }
                
                lastClickTimestamp = now
            }
        // } Closing brace for the removed if statement
    }

    private fun shouldBlock(text: String, now: Long): Boolean {
        val lastTime = lastClickTimeMap[text] ?: 0L
        val count = clickHistory[text] ?: 0

        // If clicked recently (< 2000ms), increment count
        if (now - lastTime < 2000) {
            val newCount = count + 1
            clickHistory[text] = newCount
            lastClickTimeMap[text] = now
            
            if (newCount > 3) {
                // Block for 10 seconds
                blockedTexts[text] = now + 10000 
                clickHistory[text] = 0 // Reset counter
                return true
            }
        } else {
            // Reset if gap is large
            clickHistory[text] = 1
            lastClickTimeMap[text] = now
        }
        return false
    }

    private fun collectText(node: AccessibilityNodeInfo?, list: MutableList<String>, map: MutableMap<String, Rect>, screenWidth: Int, screenHeight: Int) {
        if (node == null) return
        
        // Force refresh to get latest state
        node.refresh()

        val text = node.text?.toString()
        val description = node.contentDescription?.toString()
        // Priority: Use text if available, otherwise use contentDescription
        val content = if (!text.isNullOrEmpty()) text else description

        if (!content.isNullOrEmpty()) {
            list.add(content!!)
            
            // Search for clickable parent
            var clickCandidate = node!!
            var curr: AccessibilityNodeInfo? = node
            val tempRect = Rect()

            while (curr != null) {
                if (curr?.isClickable == true) {
                    curr?.getBoundsInScreen(tempRect)
                    // Parent size constraint: limit to 50% of screen size to avoid full-screen transparent overlays
                    if (tempRect.width() <= screenWidth * 0.5 && tempRect.height() <= screenHeight * 0.5) {
                        clickCandidate = curr!!
                        break
                    }
                }
                curr = curr?.parent
            }

            val rect = Rect()
            clickCandidate.getBoundsInScreen(rect)
            map[content] = rect
            nodeCache[content] = clickCandidate // Cache the node for direct action
            Log.v("ScannerDebug", "探测到节点类型: ${node.className} | 内容: $content | 最终点击目标: ${clickCandidate.className}")
        } else {
            if (node.isClickable) {
                Log.v("ScannerDebug", "探测到节点类型: ${node.className} | 内容为空 | isClickable: true")
            }
        }
        
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), list, map, screenWidth, screenHeight)
        }
    }

    private fun clickAt(x: Int, y: Int) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        // Micro-swipe to bypass anti-click checks
        path.lineTo((x + 1).toFloat(), (y + 1).toFloat())

        val builder = GestureDescription.Builder()
        // startTime = 50L, duration = 200L
        builder.addStroke(GestureDescription.StrokeDescription(path, 50, 200))

        val success = dispatchGesture(builder.build(), null, null)

        Log.d("AdSkipService", "手势分发结果: $success")
        if (!success) {
            Log.e("AdSkipService", "分发失败! 失败坐标: $x, $y")
        }
    }

    override fun onInterrupt() {
        Log.d("AdSkipService", "Service Interrupted")
    }
}

