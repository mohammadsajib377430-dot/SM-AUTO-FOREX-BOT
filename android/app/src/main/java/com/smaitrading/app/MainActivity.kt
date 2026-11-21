package com.smaitrading.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val captureStatus = mutableStateOf("স্ক্রিন ক্যাপচার এখনো চালু হয়নি")

    private val captureEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ScreenCaptureService.ACTION_CAPTURE_STARTED ->
                    captureStatus.value = "স্ক্রিন ক্যাপচার চালু হয়েছে; প্রথম ফ্রেমের অপেক্ষায়..."
                ScreenCaptureService.ACTION_FIRST_FRAME ->
                    captureStatus.value = "স্ক্রিন ক্যাপচার সচল · ফোনের স্ক্রিন থেকে ফ্রেম পাওয়া যাচ্ছে"
                ScreenCaptureService.ACTION_CAPTURE_ERROR ->
                    captureStatus.value = "ক্যাপচার চালু হয়নি: " +
                        (intent.getStringExtra(ScreenCaptureService.EXTRA_ERROR) ?: "অজানা সমস্যা")
                ScreenCaptureService.ACTION_CAPTURE_STOPPED ->
                    captureStatus.value = "স্ক্রিন ক্যাপচার বন্ধ"
            }
        }
    }

    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            captureStatus.value = "অনুমতি পাওয়া গেছে · ক্যাপচার সার্ভিস চালু হচ্ছে..."
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            try {
                ContextCompat.startForegroundService(this, service)
            } catch (error: Exception) {
                captureStatus.value = "সার্ভিস চালু হয়নি: " + (error.message ?: error.javaClass.simpleName)
            }
        } else {
            captureStatus.value = "স্ক্রিন ক্যাপচারের অনুমতি দেওয়া হয়নি"
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ScreenCaptureService.ACTION_CAPTURE_STARTED)
            addAction(ScreenCaptureService.ACTION_FIRST_FRAME)
            addAction(ScreenCaptureService.ACTION_CAPTURE_ERROR)
            addAction(ScreenCaptureService.ACTION_CAPTURE_STOPPED)
        }
        ContextCompat.registerReceiver(this, captureEvents, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(captureEvents) }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TradingHome(
                    captureStatus = captureStatus.value,
                    onStartCapture = {
                        captureStatus.value = "Android-এর অনুমতি চাইছে..."
                        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        capturePermission.launch(manager.createScreenCaptureIntent())
                    },
                    onStopCapture = {
                        stopService(Intent(this, ScreenCaptureService::class.java))
                        captureStatus.value = "স্ক্রিন ক্যাপচার বন্ধ করার অনুরোধ পাঠানো হয়েছে"
                    },
                    onFetch = { key, symbol, callback ->
                        Thread {
                            val result = runCatching { MarketAnalysis.fetchAndAnalyze(key, symbol) }
                            runOnUiThread { callback(result) }
                        }.start()
                    }
                )
            }
        }
    }
}

@Composable
private fun TradingHome(
    captureStatus: String,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onFetch: (String, String, (Result<Analysis>) -> Unit) -> Unit
) {
    var platform by remember { mutableStateOf("Quotex") }
    var pair by remember { mutableStateOf("EUR/USD") }
    var apiKey by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Analysis?>(null) }
    var status by remember { mutableStateOf("Twelve Data API key দিয়ে ১-মিনিটের ক্যান্ডেল বিশ্লেষণ শুরু করুন") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SM AI TRADING", style = MaterialTheme.typography.headlineMedium)
        Text("Quotex + Forex · Signal assistant", color = MaterialTheme.colorScheme.secondary)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("প্ল্যাটফর্ম")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = platform == "Quotex", onClick = { platform = "Quotex" }, label = { Text("Quotex") })
                    FilterChip(selected = platform == "Forex", onClick = { platform = "Forex" }, label = { Text("Forex") })
                }
                Text("মার্কেট")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(pair) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { pair = it; expanded = false; result = null })
                        }
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Twelve Data API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !loading && apiKey.isNotBlank(),
                    onClick = {
                        loading = true
                        status = "লাইভ ১-মিনিটের ক্যান্ডেল আনা হচ্ছে..."
                        onFetch(apiKey, pair) { response ->
                            loading = false
                            response.onSuccess {
                                result = it
                                status = "ডেটা এসেছে · " + it.candleTime
                            }.onFailure {
                                result = null
                                status = it.message ?: "ডেটা আনতে সমস্যা হয়েছে"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (loading) "বিশ্লেষণ চলছে..." else "লাইভ ডেটা বিশ্লেষণ করুন") }
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
        result?.let { analysis ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("সিগন্যাল · " + platform + " · " + pair, style = MaterialTheme.typography.titleMedium)
                    Text(analysis.signal, style = MaterialTheme.typography.headlineLarge,
                        color = when (analysis.signal) {
                            "BUY" -> MaterialTheme.colorScheme.primary
                            "SELL" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        })
                    Text(analysis.explanation)
                    Text("শেষ মূল্য: " + analysis.lastPrice)
                    Text("EMA 9: " + analysis.emaFast + " · EMA 21: " + analysis.emaSlow)
                    Text("RSI 14: " + "%.1f".format(analysis.rsi))
                    Text("ক্যান্ডেল সময়: " + analysis.candleTime, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("মোবাইল স্ক্রিন ক্যাপচার", style = MaterialTheme.typography.titleMedium)
                Text(captureStatus)
                Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) {
                    Text("স্ক্রিন ক্যাপচার চালু করুন")
                }
                OutlinedButton(onClick = onStopCapture, modifier = Modifier.fillMaxWidth()) {
                    Text("ক্যাপচার বন্ধ করুন")
                }
                Text(
                    "এটি আপাতত ফোনের স্ক্রিন স্থানীয়ভাবে ক্যাপচার করে এবং ফ্রেম আসছে কি না দেখায়। অন্য মোবাইলে লাইভ পাঠানো বা Quotex চার্ট থেকে স্বয়ংক্রিয় ক্যান্ডেল শনাক্তকরণ এখনো যুক্ত হয়নি।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("সিগন্যাল নিরাপত্তা", style = MaterialTheme.typography.titleMedium)
                Text("BUY / SELL / WAIT কেবল বিশ্লেষণ-সহায়তা। কোনো অর্ডার স্বয়ংক্রিয়ভাবে পাঠানো হয় না। Quotex-এর নিজস্ব কোট ও Twelve Data-এর কোট/সময় আলাদা হতে পারে; দুই উৎস না মিললে WAIT রাখা হবে।")
            }
        }
    }
}
