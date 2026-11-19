package com.smaitrading.app

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TradingHome(
                    onStartCapture = {
                        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        capturePermission.launch(manager.createScreenCaptureIntent())
                    },
                    onStopCapture = {
                        stopService(Intent(this, ScreenCaptureService::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun TradingHome(onStartCapture: () -> Unit, onStopCapture: () -> Unit) {
    var platform by remember { mutableStateOf("Quotex") }
    var pair by remember { mutableStateOf("EUR/USD") }
    var captureState by remember { mutableStateOf("স্ক্রিন বিশ্লেষণের অনুমতি এখনো দেওয়া হয়নি") }

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
                            DropdownMenuItem(text = { Text(it) }, onClick = { pair = it; expanded = false })
                        }
                    }
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("মোবাইল স্ক্রিন ক্যান্ডেল বিশ্লেষণ", style = MaterialTheme.typography.titleMedium)
                Text(captureState)
                Button(onClick = { onStartCapture(); captureState = "Android-এর screen-sharing permission চাইবে। অনুমতি দিলে capture service চালু হবে।" }, modifier = Modifier.fillMaxWidth()) {
                    Text("স্ক্রিন শেয়ারিং চালু করুন")
                }
                OutlinedButton(onClick = { onStopCapture(); captureState = "স্ক্রিন ক্যাপচার বন্ধ করা হয়েছে" }, modifier = Modifier.fillMaxWidth()) {
                    Text("ক্যাপচার বন্ধ করুন")
                }
                Text("নোট: এই প্রাথমিক সংস্করণে স্ক্রিন ক্যাপচারের অনুমতি ও সার্ভিসের ভিত্তি তৈরি হয়েছে। ক্যান্ডেল শনাক্তকরণ/BUY-SELL ভিশন মডিউল এখনো যুক্ত হয়নি।", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("লাইভ ডেটা বিশ্লেষণ", style = MaterialTheme.typography.titleMedium)
                Text("আগের SM AUTO FOREX BOT-এর Twelve Data ভিত্তিক M1 বিশ্লেষণ (EMA 9/21, RSI 14, MACD) এই অ্যাপে সংযুক্ত করার পর স্ক্রিন বিশ্লেষণের সঙ্গে মিলিয়ে সিগন্যাল দেখাবে।")
                Text("বর্তমান অবস্থা: UI scaffold · live API integration pending", color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("সিগন্যাল নিরাপত্তা", style = MaterialTheme.typography.titleMedium)
                Text("BUY / SELL / WAIT কেবল বিশ্লেষণ-সহায়তা। কোনো অর্ডার স্বয়ংক্রিয়ভাবে পাঠানো হবে না। দ্বন্দ্বপূর্ণ ডেটা বা অস্পষ্ট চার্ট হলে WAIT দেখানো হবে।")
            }
        }
    }
}
