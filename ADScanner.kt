// 定义一个广告扫描类
// 包含一个函数 detectAndClick，输入参数为当前页面的文字列表
// 如果列表中包含“跳过”、“Skip”或“关闭”，则打印并返回该文字的坐标
class ADScanner {

    data class Coordinate(val x: Int, val y: Int)

    fun detectAndClick(textList: List<String>): Coordinate? {
        val keywords = listOf("跳过", "Skip", "关闭")
        for (text in textList) {
            if (keywords.contains(text)) {
                // 假设我们有一个方法 getCoordinate(text: String): Coordinate 来获取文字的坐标
                val coordinate = getCoordinate(text)
                println("Detected keyword '$text' at coordinates: $coordinate")
                return coordinate
            }
        }
        return null
    }

    private fun getCoordinate(text: String): Coordinate {
        // 这里是一个模拟实现，实际应用中应根据具体情况获取坐标
        return when (text) {
            "跳过" -> Coordinate(100, 200)
            "Skip" -> Coordinate(150, 250)
            "关闭" -> Coordinate(200, 300)
            else -> Coordinate(0, 0)
        }
    }
}