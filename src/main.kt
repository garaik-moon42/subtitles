import java.io.FileReader
import java.time.LocalTime
import java.util.*
import kotlin.math.min

data class Subtitle(val start:LocalTime, val end:LocalTime, val text:String)

private fun readScript():String {
    val regexMoreThanOneWhitespace = Regex("\\s\\s+")
    val regexAnythingBetweenBrackets = Regex("\\[.*?]")
    val out = StringBuilder()
    FileReader("input/sample-script.txt").useLines { lines ->
        lines
//            .map { it.replace(regexAnythingBetweenBrackets, "") }
            .map { it.replace(regexMoreThanOneWhitespace, " ") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach(out::appendLine)
    }
    return out.toString()
}

private fun readSubtitles():List<Subtitle> {
    val subtitles = mutableListOf<Subtitle>()
    FileReader("input/sample-subtitles.srt").useLines { lines ->
        val i = lines.iterator()
        while (i.hasNext()) {
            val index = i.next().toInt()
            val timing = i.next().replace(",", ".")
            val parts = timing.split(" --> ")
            if (parts.size == 2) {
                val start = LocalTime.parse(parts[0])
                val end = LocalTime.parse(parts[1])
                val text = buildString {
                    while (i.hasNext()) {
                        val line = i.next()
                        if (line.isBlank()) {
                            break
                        }
                        appendLine(line)
                    }
                }
                subtitles.add(Subtitle(start, end, text.trim()))
            }
        }
    }
    return subtitles.toList()
}

fun levenshtein(lhs: String, rhs: String): Int {
    val dp = Array(lhs.length + 1) { IntArray(rhs.length + 1) }

    fun costOfSubstitution(a: Char, b: Char) = if (a == b) 0 else 1
    fun min(vararg numbers: Int) = numbers.minOrNull() ?: Int.MAX_VALUE

    for (i in 0..lhs.length) {
        for (j in 0..rhs.length) {
            if (i == 0) {
                dp[i][j] = j
            } else if (j == 0) {
                dp[i][j] = i
            } else {
                dp[i][j] = min(
                    dp[i - 1][j - 1] + costOfSubstitution(lhs[i - 1], rhs[j - 1]),
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1
                )
            }
        }
    }

    return dp[lhs.length][rhs.length]
}

fun main() {
    val script = readScript()
    val subtitles = readSubtitles()
    var subtitleIndex = 0;
    var pos = 0;
    do {
        val subtitle = subtitles[subtitleIndex]
        var minDistance = Int.MAX_VALUE
        var minPos = 0
        for (i in pos .. script.length - subtitle.text.length) {
            val distance = levenshtein(
                subtitle.text.lowercase(Locale.getDefault()),
                script.substring(i, i + subtitle.text.length).lowercase(Locale.getDefault())
            )
            if (distance < minDistance) {
                minDistance = distance
                minPos = i
            }
        }
        println(subtitleIndex)
        println("minDistance = $minDistance, minPos = $minPos")
        println("subtitle: ${subtitle.text}")
        println("script: ${script.substring(minPos, minPos + subtitle.text.length)}")
        pos = minPos + subtitle.text.length
        subtitleIndex++
    }
    while (pos < script.length && subtitleIndex < subtitles.size)
}
