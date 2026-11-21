package com.smaitrading.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Local MediaProjection capture. This confirms that frames arrive on-device.
 * It does not stream the screen to another device or interpret chart candles.
 */
class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var firstFrameReported = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            sendStatus(ACTION_CAPTURE_STOPPED)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }

            val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0)
                ?: throw IllegalStateException("অনুমতির ফলাফল পাওয়া যায়নি")
            val resultData = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            } ?: throw IllegalStateException("স্ক্রিন ক্যাপচারের অনুমতি পাওয়া যায়নি")

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
                ?: throw IllegalStateException("MediaProjection চালু করা যায়নি")
            projection?.registerCallback(projectionCallback, Handler(mainLooper))
            startFrameCapture()
            sendStatus(ACTION_CAPTURE_STARTED)
            return START_NOT_STICKY
        } catch (error: Exception) {
            sendStatus(ACTION_CAPTURE_ERROR, error.message ?: error.javaClass.simpleName)
            stopSelf(startId)
            return START_NOT_STICKY
        }
    }

    private fun startFrameCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { imageReader ->
            imageReader.setOnImageAvailableListener({ source ->
                source.acquireLatestImage()?.use {
                    if (!firstFrameReported) {
                        firstFrameReported = true
                        sendStatus(ACTION_FIRST_FRAME)
                    }
                }
            }, Handler(mainLooper))
        }

        display = projection?.createVirtualDisplay(
            "SM-AI-Screen-Capture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            Handler(mainLooper)
        ) ?: throw IllegalStateException("Virtual display তৈরি করা যায়নি")
    }

    private fun sendStatus(action: String, error: String? = null) {
        val event = Intent(action).setPackage(packageName)
        if (error != null) event.putExtra(EXTRA_ERROR, error)
        sendBroadcast(event)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SM AI screen capture",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setContentTitle("SM AI TRADING")
            .setContentText("ফোনের স্ক্রিন ক্যাপচার চালু আছে")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        display?.release()
        display = null
        reader?.setOnImageAvailableListener(null, null)
        reader?.close()
        reader = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_ERROR = "capture_error"
        const val ACTION_CAPTURE_STARTED = "com.smaitrading.app.CAPTURE_STARTED"
        const val ACTION_FIRST_FRAME = "com.smaitrading.app.FIRST_FRAME"
        const val ACTION_CAPTURE_ERROR = "com.smaitrading.app.CAPTURE_ERROR"
        const val ACTION_CAPTURE_STOPPED = "com.smaitrading.app.CAPTURE_STOPPED"
        private const val CHANNEL_ID = "sm_ai_screen_capture"
        private const val NOTIFICATION_ID = 4102
    }
}
