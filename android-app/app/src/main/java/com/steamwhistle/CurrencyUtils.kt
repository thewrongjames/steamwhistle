package com.steamwhistle

import android.icu.text.NumberFormat
import android.icu.util.Currency

object CurrencyUtils {

    fun toCurrency(cent: Long) : String {
        val format: NumberFormat = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("AUD")

        return format.format(cent/100.0)
    }

    fun toCents(currency: String, default: Long) : Long {
        var cents: Long
        var dollars: Long

        var currencySplit = currency
            .replace("A$", "")
            .replace(",", "")
            .split(".")

        when (currencySplit.size) {
            0 -> return 0
            1 -> {
                try {
                    dollars = currencySplit[0].toLong()
                    cents = 0
                } catch (error: NumberFormatException) {
                    return default
                }
            }
            2 -> {
                try {
                    dollars = if (currencySplit[0].isEmpty()) {
                        0
                    } else {
                        currencySplit[0].toLong()
                    }
                    cents = if (currencySplit[1].isEmpty()) {
                        0
                    } else {
                        currencySplit[1].toLong()
                    }

                } catch (error: NumberFormatException) {
                    return default
                }
            }
            else -> {
                return default
            }
        }
        return (dollars * 100) + cents
    }
}