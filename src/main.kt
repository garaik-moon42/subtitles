import java.io.FileReader
import java.time.LocalTime
import java.util.*

data class Subtitle(val start:LocalTime, val end:LocalTime, val text: String, val pos:Int = Int.MAX_VALUE)

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
            i.next()
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

private fun fixSubtitles(script: String, subtitles: List<Subtitle>):List<Subtitle> {
    val fixedSubtitles = mutableListOf<Subtitle>()
    subtitles.forEachIndexed() { index, subtitle ->
        val from = subtitle.pos
        val to = if (index == subtitles.size - 1) script.length else subtitles[index + 1].pos
        val text = wordAwareWrap(script.substring(from, to).trim(), 50).joinToString("\n")
        fixedSubtitles.add(Subtitle(subtitle.start, subtitle.end, text, subtitle.pos))
    }
    return fixedSubtitles.toList()
}

private fun wordAwareWrap(text: String, width: Int):List<String> {
    val out = mutableListOf<String>()
    var currentLine = ""
    text.split(" ").forEach { word ->
        if (currentLine.length + word.length > width) {
            out.add(currentLine)
            currentLine = ""
        }
        if (currentLine.isNotEmpty()) {
            currentLine += " "
        }
        currentLine += word
    }
    out.add(currentLine)
    return out.toList()
}

private fun distanceAtPos(script: String, subtitle: Subtitle, pos: Int): Int {
    val lhs = subtitle.text.lowercase(Locale.getDefault())
    val rhs = script.substring(pos, pos + subtitle.text.length).lowercase(Locale.getDefault())
    return levenshtein(lhs, rhs)
}

private fun adjustPosition(script: String, pos: Int): Int {
    var adjustedPos = pos
    if (script[adjustedPos].isWhitespace()) {
        adjustedPos++
    }
    else {
        while (adjustedPos > 0 && !script[adjustedPos - 1].isWhitespace()) adjustedPos--
    }
    return adjustedPos
}

private fun detectPositions(script: String, subtitles: List<Subtitle>):List<Subtitle> {
    val subtitlesWithPositions = mutableListOf<Subtitle>()
    var pos = 0
    subtitles.forEachIndexed { subtitleIndex, subtitle ->
        var min = Pair(Int.MAX_VALUE, 0)
        for (i in pos.. min(pos + subtitle.text.length, script.length - subtitle.text.length)) {
            val distance = distanceAtPos(script, subtitle, i)
            if (distance < min.first) {
                min = Pair(distance, i)
            }
        }
        if (min.first == Int.MAX_VALUE) {
            println("WARNING: No match for subtitle #$subtitleIndex:\n$subtitle")
        }
        else {
            subtitlesWithPositions.add(Subtitle(subtitle.start, subtitle.end, subtitle.text, adjustPosition(script, min.second)))
            pos = min.second + subtitle.text.length
        }
    }
    return subtitlesWithPositions.toList()
}

private fun printSubtitles(subtitles: List<Subtitle>) {
    subtitles.forEachIndexed { index, subtitle ->
        println("${index + 1}.")
        val start = subtitle.start.toString().replace('.', ',')
        val end = subtitle.end.toString().replace('.', ',')
        println("$start --> $end")
        println(subtitle.text)
        println()
    }
}

fun main() {
    val script = readScript()
    val subtitles = fixSubtitles(script, detectPositions(script, readSubtitles()))
    printSubtitles(subtitles)
}

