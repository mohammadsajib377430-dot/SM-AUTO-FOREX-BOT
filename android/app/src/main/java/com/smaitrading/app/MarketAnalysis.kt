package com.smaitrading.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class Candle(val time: String, val open: Double, val high: Double, val low: Double, val close: Double)
data class Analysis(val signal: String, val explanation: String, val lastPrice: Double, val emaFast: Double, val emaSlow: Double, val rsi: Double, val candleTime: String)

object MarketAnalysis {
    fun fetchAndAnalyze(apiKey: String, symbol: String): Analysis {
        require(apiKey.isNotBlank()) { "আগে Twelve Data API key লিখুন" }
        val encodedSymbol = URLEncoder.encode(symbol, "UTF-8")
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val endpoint = URL("https://api.twelvedata.com/time_series?symbol=$encodedSymbol&interval=1min&outputsize=100&apikey=$encodedKey")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.has("status") && json.optString("status") == "error") {
                throw IllegalStateException(json.optString("message", "Market data provider error"))
            }
            val values = json.optJSONArray("values") ?: throw IllegalStateException("ক্যান্ডেল ডেটা পাওয়া যায়নি")
            val candles = (0 until values.length()).map { i ->
                val item = values.getJSONObject(i)
                Candle(item.getString("datetime"), item.getDouble("open"), item.getDouble("high"), item.getDouble("low"), item.getDouble("close"))
            }.reversed()
            if (candles.size < 30) throw IllegalStateException("বিশ্লেষণের জন্য যথেষ্ট ক্যান্ডেল নেই")
            return analyze(candles)
        } finally {
            connection.disconnect()
        }
    }

    fun analyze(candles: List<Candle>): Analysis {
        val closes = candles.map { it.close }
        val fast = ema(closes, 9)
        val slow = ema(closes, 21)
        val rsiValue = rsi(closes, 14)
        val last = candles.last()
        val previous = candles[candles.lastIndex - 1]
        val bullishBody = last.close > last.open
        val bearishBody = last.close < last.open
        val bullishMomentum = last.close > previous.close
        val bearishMomentum = last.close < previous.close

        val buy = fast > slow && rsiValue in 52.0..70.0 && bullishBody && bullishMomentum
        val sell = fast < slow && rsiValue in 30.0..48.0 && bearishBody && bearishMomentum
        val signal = when {
            buy -> "BUY"
            sell -> "SELL"
            else -> "WAIT"
        }
        val reason = when (signal) {
            "BUY" -> "EMA 9 > EMA 21, RSI " + "%.1f".format(rsiValue) + ", শেষ ক্যান্ডেল bullish"
            "SELL" -> "EMA 9 < EMA 21, RSI " + "%.1f".format(rsiValue) + ", শেষ ক্যান্ডেল bearish"
            else -> "ট্রেন্ড, RSI ও শেষ ক্যান্ডেল একমত নয়—অপেক্ষা করুন"
        }
        return Analysis(signal, reason, last.close, fast, slow, rsiValue, last.time)
    }

    private fun ema(values: List<Double>, period: Int): Double {
        val multiplier = 2.0 / (period + 1)
        return values.drop(1).fold(values.first()) { acc, value -> (value - acc) * multiplier + acc }
    }

    private fun rsi(values: List<Double>, period: Int): Double {
        val changes = values.zipWithNext { a, b -> b - a }.takeLast(period)
        val gains = changes.filter { it > 0 }.sum() / period
        val losses = abs(changes.filter { it < 0 }.sum()) / period
        if (losses == 0.0) return 100.0
        val rs = gains / losses
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
