import java.io.FileReader
import java.time.LocalTime
import java.util.*

data class Subtitle(val start:LocalTime, val end:LocalTime, val text:String)

private fun readScript():String {
    val regexAnyWhitespace = Regex("\\s+")
    val regexAnythingBetweenBrackets = Regex("\\[.*?]")
    val regexAnyNonStandardCharacter = Regex("[^-?\\\\!.,;'\"a-zA-Z0-9\\s]")
    val out = StringBuilder()
    FileReader("input/sample-script.txt").useLines { lines ->
        lines
            .map { it.replace(regexAnythingBetweenBrackets, "") }
            .map { it.replace(regexAnyNonStandardCharacter, "") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach(out::appendLine)
    }
    return out.replace(regexAnyWhitespace, " ").trim()
}

private fun readSubtitles():List<Subtitle> {
    val regexAnyWhitespace = Regex("\\s+")
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
                subtitles.add(Subtitle(start, end, text.replace(regexAnyWhitespace, " ").trim()))
            }
        }
    }
    return subtitles.toList()
}

private fun min(vararg numbers: Int) = numbers.minOrNull() ?: Int.MAX_VALUE

private fun levenshtein(lhs: String, rhs: String): Int {
    val dp = Array(lhs.length + 1) { IntArray(rhs.length + 1) }

    fun costOfSubstitution(a: Char, b: Char) = if (a == b) 0 else 1

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

private fun displayResult(script: String, subtitles:List<Subtitle>, subtitlePositions:List<Pair<Int, Int>>) {
    subtitles.forEachIndexed { index, subtitle ->
        println("${index + 1}.")
        println("Subtitle >${subtitle.text}<")
        println("Script   >${script.substring(subtitlePositions[index].first, subtitlePositions[index].second)}<")
        println("------------------------------------------------------------")
    }
}

private fun distanceAtPos(script: String, subtitle: Subtitle, pos: Int): Int {
    val lhs = subtitle.text.lowercase(Locale.getDefault())
    val rhs = script.substring(pos, pos + subtitle.text.length).lowercase(Locale.getDefault())
    return levenshtein(lhs, rhs)
}

private fun adjustPositions(script: String, start: Int, end: Int): Pair<Int, Int> {
    fun isPunctuation(c: Char) = c == '.' || c == ',' || c == ';' || c == '!' || c == '?' || c == ':' || c == '\'' || c == '"'

    var adjustedStart = start
    var adjustedEnd = end
    if (script[adjustedStart].isWhitespace()) {
        adjustedStart++
    }
    else {
        while (adjustedStart > 0 && !script[adjustedStart - 1].isWhitespace()) adjustedStart--
    }
    if (script[adjustedEnd - 1].isWhitespace()) {
        adjustedEnd--
    }
    else if (!isPunctuation(script[adjustedEnd - 1])) {
        while (adjustedEnd < script.length && script[adjustedEnd].isLetterOrDigit()) adjustedEnd++
        if (!script[adjustedEnd].isWhitespace()) adjustedEnd++
    }
    return Pair(adjustedStart, adjustedEnd)
}

private fun detectPositions(subtitles: List<Subtitle>, script: String): MutableList<Pair<Int, Int>> {
    val subtitlePositions = mutableListOf<Pair<Int, Int>>()
    var subtitleIndex = 0;
    var pos = 0;
    do {
        if (subtitleIndex == 142) {
            println("here we are")
        }
        val subtitle = subtitles[subtitleIndex]
        var minDistance = Int.MAX_VALUE
        var minPos = 0

        for (i in pos.. min(pos + subtitle.text.length, script.length - subtitle.text.length)) {
            val distance = distanceAtPos(script, subtitle, i)
            if (distance < minDistance) {
                minDistance = distance
                minPos = i
            }
        }

        subtitlePositions.add(adjustPositions(script, minPos, minPos + subtitle.text.length))
        pos = minPos + subtitle.text.length
        subtitleIndex++
        print(".")
    } while (pos < script.length && subtitleIndex < subtitles.size)
    return subtitlePositions
}

fun main() {
    val script = readScript()
    val subtitles = readSubtitles()
    val subtitlePositions = detectPositions(subtitles, script)
    println()
    displayResult(script, subtitles, subtitlePositions)
}

