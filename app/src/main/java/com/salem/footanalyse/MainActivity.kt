package com.salem.footanalyse

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private val io = Executors.newFixedThreadPool(3)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 et 16 imposent l'affichage bord à bord : on le gère proprement.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#16261D"))
        setContentView(web)

        // Évite que le contenu passe sous la barre d'état, la barre de navigation ou le clavier.
        ViewCompat.setOnApplyWindowInsetsListener(web) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true      // sauvegarde des données (bankroll, journal…)
            allowFileAccess = false
            allowContentAccess = false
        }

        web.addJavascriptInterface(NetBridge(), "AndroidNet")

        web.webViewClient = object : WebViewClient() {
            // Les liens externes (Flashscore, SofaScore, Google) s'ouvrent dans le navigateur.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "http" || uri.scheme == "https") {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        // Aucun navigateur disponible : on ignore le lien au lieu de fermer l'application.
                    }
                    return true
                }
                return false
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (web.canGoBack()) web.goBack() else finish()
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("file:///android_asset/index.html")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        io.shutdownNow()
        web.destroy()
        super.onDestroy()
    }

    /** Connexion internet native vers API-Football (évite les blocages CORS de la WebView). */
    inner class NetBridge {
        @JavascriptInterface
        fun get(id: Int, url: String, key: String) {
            if (!url.startsWith("https://v3.football.api-sports.io/")) {
                deliver(id, -1, "Adresse non autorisée")
                return
            }
            io.execute {
                try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.setRequestProperty("x-apisports-key", key)
                    c.setRequestProperty("Accept", "application/json")
                    c.connectTimeout = 15000
                    c.readTimeout = 20000
                    val code = c.responseCode
                    val stream = if (code in 200..299) c.inputStream else c.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    c.disconnect()
                    deliver(id, code, body)
                } catch (e: Exception) {
                    deliver(id, -1, "Pas de connexion internet. Vérifiez vos données mobiles ou le Wi-Fi.")
                }
            }
        }
    }

    private fun deliver(id: Int, status: Int, body: String) {
        val js = "window.__netCb($id,$status,${JSONObject.quote(body)})"
        runOnUiThread { if (!isFinishing) web.evaluateJavascript(js, null) }
    }
}
