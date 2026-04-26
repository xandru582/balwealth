package com.empiretycoon.game.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("es-ES")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}
private val intFmt = DecimalFormat("#,###", symbols)
private val money2 = DecimalFormat("#,##0.00", symbols)

fun Double.fmtMoney(): String {
    val abs = kotlin.math.abs(this)
    val sign = if (this < 0) "-" else ""
    return when {
        abs >= 1_000_000_000 -> "$sign${money2.format(abs / 1_000_000_000)} B €"
        abs >= 1_000_000 -> "$sign${money2.format(abs / 1_000_000)} M €"
        abs >= 10_000 -> "$sign${intFmt.format(abs.toLong())} €"
        else -> "$sign${money2.format(abs)} €"
    }
}

fun Int.fmtInt(): String = intFmt.format(this.toLong())
fun Long.fmtInt(): String = intFmt.format(this)

fun Double.fmtNumber(): String = money2.format(this)

fun Double.fmtPct(): String = "${money2.format(this * 100)}%"

fun Int.fmtTimeSeconds(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return when {
        h > 0 -> "%dh %dm".format(h, m)
        m > 0 -> "%dm %ds".format(m, s)
        else -> "%ds".format(s)
    }
}

fun Double.fmtTimeSeconds(): String = toInt().fmtTimeSeconds()
