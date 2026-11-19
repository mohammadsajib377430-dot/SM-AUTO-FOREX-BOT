package com.smaitrading.app

/**
 * Combines independent market-data and screen-candle readings.
 * A visual reading is accepted only when it is recent and confidently classified.
 * Any disagreement or missing source produces WAIT rather than a forced entry.
 */
data class VisualSnapshot(
    val direction: String, // "BUY", "SELL", or "NEUTRAL"
    val confidence: Double, // 0.0..1.0
    val capturedAtMillis: Long,
    val chartRecognized: Boolean
)

data class FusedSignal(
    val signal: String,
    val explanation: String,
    val marketSignal: String,
    val screenSignal: String
)

object SignalFusion {
    fun combine(
        market: Analysis?,
        screen: VisualSnapshot?,
        nowMillis: Long = System.currentTimeMillis()
    ): FusedSignal {
        if (market == null) return FusedSignal("WAIT", "লাইভ মার্কেট ডেটা পাওয়া যায়নি", "MISSING", screen?.direction ?: "MISSING")
        if (screen == null || !screen.chartRecognized) {
            return FusedSignal("WAIT", "মোবাইল চার্ট এখনো নির্ভরযোগ্যভাবে শনাক্ত হয়নি", market.signal, "UNAVAILABLE")
        }
        val age = nowMillis - screen.capturedAtMillis
        if (age < 0 || age > 5000) {
            return FusedSignal("WAIT", "স্ক্রিনের ক্যান্ডেল ডেটা পুরোনো; নতুন স্ক্রিন ফ্রেম দরকার", market.signal, "STALE")
        }
        if (screen.confidence < 0.85) {
            return FusedSignal("WAIT", "স্ক্রিন বিশ্লেষণের confidence কম; ট্রেড এড়িয়ে চলুন", market.signal, screen.direction)
        }
        if (market.signal == "WAIT") {
            return FusedSignal("WAIT", "লাইভ ডেটার নিয়মগুলো একমত নয়", market.signal, screen.direction)
        }
        if (market.signal != screen.direction) {
            return FusedSignal("WAIT", "লাইভ ডেটা ও মোবাইল চার্টের দিক মিলছে না", market.signal, screen.direction)
        }
        return FusedSignal(
            market.signal,
            "লাইভ ডেটা ও স্ক্রিন বিশ্লেষণ একমত। তবুও ডেমোতে যাচাই করুন; এটি নিশ্চিত ফল নয়।",
            market.signal,
            screen.direction
        )
    }
}
