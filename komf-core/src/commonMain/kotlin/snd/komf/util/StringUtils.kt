package snd.komf.util

import org.apache.commons.lang3.StringUtils

private val fullwidthRegex = "[\uff01-\uff5e]".toRegex()

fun replaceFullwidthChars(input: String): String {
    return input.replace(fullwidthRegex) { matchResult ->
        val codePoint = matchResult.value.codePointAt(0) - 0xfee0
        Character.toString(codePoint.toChar())
    }
}

fun replaceFullwidthChars(input: Int): String {
    val codePoint = input - 0xfee0
    return Character.toString(codePoint.toChar())
}

fun stripAccents(input: String): String = StringUtils.stripAccents(input)