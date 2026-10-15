package com.mmushtaq04.buysell.domain

object PartyNormalizer {

    fun normalizePhone(rawPhone: String?): String {
        if (rawPhone.isNullOrBlank()) return ""
        val digitsOnly = rawPhone.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return ""

        return when {
            digitsOnly.length == 12 && digitsOnly.startsWith("923") -> "0" + digitsOnly.substring(2)
            digitsOnly.length == 10 && digitsOnly.startsWith("3") -> "0$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("03") -> digitsOnly
            else -> digitsOnly
        }
    }

    fun normalizeCnic(rawCnic: String?): String {
        if (rawCnic.isNullOrBlank()) return ""
        return rawCnic.filter { it.isDigit() }
    }
}
