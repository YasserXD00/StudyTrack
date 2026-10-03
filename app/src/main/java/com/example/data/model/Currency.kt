package com.example.data.model

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    MAD("MAD", "MAD", "Moroccan Dirham"),
    JPY("JPY", "¥", "Japanese Yen"),
    CNY("CNY", "¥", "Chinese Yuan");

    companion object {
        fun fromCode(code: String): Currency {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}
