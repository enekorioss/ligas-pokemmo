package com.axion.farmligas

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.webkit.WebViewAssetLoader

class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bubble: ImageView
    private var expanded: FrameLayout? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var assetLoader: WebViewAssetLoader

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        startForeground(1, buildNotification())
        createBubble()
    }

    private fun createBubble() {
        bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_info_details)
            setBackgroundResource(R.drawable.bubble_bg)
            setPadding(20, 20, 20, 20)
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubble.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = bubbleParams.x; initY = bubbleParams.y
                    touchX = ev.rawX; touchY = ev.rawY
                    moved = false; false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - touchX
                    val dy = ev.rawY - touchY
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    bubbleParams.x = initX + dx.toInt()
                    bubbleParams.y = initY + dy.toInt()
                    wm.updateViewLayout(bubble, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) expandPanel()
                    true
                }
                else -> false
            }
        }

        wm.addView(bubble, bubbleParams)
    }

    private fun expandPanel() {
        if (expanded != null) { collapsePanel(); return }

        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xCC4ADE80.toInt())
            setPadding(16, 12, 16, 12)
        }
        val title = TextView(this).apply {
            text = "FarmLigas"
            setTextColor(0xFF000000.toInt())
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "X"
            setTextColor(0xFF000000.toInt())
            textSize = 22f
            setPadding(16, 0, 16, 0)
            setOnClickListener { collapsePanel() }
        }
        header.addView(title)
        header.addView(close)

        val container = FrameLayout(this).apply {
            setBackgroundColor(0xFF0A0A0A.toInt())
            addView(header, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(web, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { topMargin = 100 })
        }

        val panelParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            (resources.displayMetrics.heightPixels * 0.75).toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        wm.addView(container, panelParams)
        expanded = container
    }

    private fun collapsePanel() {
        expanded?.let { wm.removeView(it); expanded = null }
    }

    private fun buildNotification(): Notification {
        val channelId = "farmligas_bubble"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Burbuja FarmLigas", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("FarmLigas activo")
            .setContentText("Toca la burbuja para abrir la guia")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        collapsePanel()
        if (::bubble.isInitialized) { try { wm.removeView(bubble) } catch (_: Exception) {} }
    }
}
