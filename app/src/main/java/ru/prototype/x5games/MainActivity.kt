package ru.prototype.x5games

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class MainActivity : Activity() {
    // Created once per app process. It is not written to preferences, files, or a database.
    private lateinit var clientId: String
    private lateinit var root: LinearLayout
    private var webView: WebView? = null

    private val skyBlue = Color.rgb(135, 206, 235)
    private val darkBlue = Color.rgb(21, 51, 74)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = "X5_${Random.nextInt(1, 101)}"
        showHome()
    }

    private fun showHome() {
        webView?.let { old ->
            (old.parent as? ViewGroup)?.removeView(old)
            old.destroy()
        }
        webView = null

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(skyBlue)
        }

        val greeting = TextView(this).apply {
            text = "Привет, человек"
            textSize = 28f
            setTextColor(darkBlue)
            typeface = Typeface.DEFAULT
            gravity = Gravity.CENTER
        }
        root.addView(greeting, matchWrap())

        val identifier = TextView(this).apply {
            text = "твой идентификатор: $clientId"
            textSize = 16f
            setTextColor(darkBlue)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(28))
        }
        root.addView(identifier, matchWrap())

        val openButton = Button(this).apply {
            text = "Каталог игр"
            isAllCaps = false
            setTextColor(darkBlue)
            setOnClickListener { showCatalog() }
        }
        root.addView(openButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
    }

    private fun showCatalog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(skyBlue)
        }

        val backButton = Button(this).apply {
            text = "← Назад"
            isAllCaps = false
            setTextColor(darkBlue)
            setOnClickListener { showHome() }
        }
        layout.addView(backButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    // Reapply after the site has rendered. The page can build its header
                    // asynchronously, so the injected script also watches DOM changes.
                    view.evaluateJavascript(HIDE_SITE_HEADER_SCRIPT, null)
                }
            }
            webChromeClient = WebChromeClient()
        }
        layout.addView(browser, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        webView = browser
        setContentView(layout)

        val encodedId = URLEncoder.encode(clientId, StandardCharsets.UTF_8.name())
        browser.loadUrl("https://beeline.ru/games/games-page/?clientId=$encodedId")
    }

    @Deprecated("Deprecated in Android API, retained for compatibility with older Android versions")
    override fun onBackPressed() {
        if (webView != null) {
            showHome()
        } else {
            super.onBackPressed()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private companion object {
        // Heuristic DOM/CSS injection for a prototype. The live site's exact selectors
        // could not be inspected from this environment, so several common header names
        // are covered and the script keeps checking if the page renders its shell later.
        private const val HIDE_SITE_HEADER_SCRIPT = """
            (function() {
              if (!document.documentElement) return;
              function hideLikelyTopBar() {
                var selectors = [
                  'header', 'nav', '[role="banner"]', '#header', '#header-wrap',
                  '#site-header', '.header', '.header-wrap', '.site-header',
                  '.main-header', '.top-header', '.navbar', '.navigation-header',
                  '[class*="header__"]', '[class*="Header_header"]', 'nav'
                ];
                var seen = [];
                selectors.forEach(function(sel) {
                  try { document.querySelectorAll(sel).forEach(function(el) {
                    if (seen.indexOf(el) < 0) seen.push(el);
                  }); } catch (e) {}
                });
                var vh = window.innerHeight || 800;
                seen.forEach(function(el) {
                  var r = el.getBoundingClientRect();
                  var cs = window.getComputedStyle(el);
                  if (r.width > window.innerWidth * 0.65 && r.top < 180 && r.height > 0 &&
                      r.height < Math.min(240, vh * 0.4) &&
                      cs.position !== 'absolute' && cs.display !== 'none') {
                    el.style.setProperty('display', 'none', 'important');
                    el.style.setProperty('visibility', 'hidden', 'important');
                    el.style.setProperty('height', '0', 'important');
                    el.style.setProperty('min-height', '0', 'important');
                    el.style.setProperty('margin', '0', 'important');
                    el.style.setProperty('padding', '0', 'important');
                  }
                });
              }

              hideLikelyTopBar();
              if (!window.__x5HeaderObserver) {
                window.__x5HeaderObserver = new MutationObserver(function() {
                  hideLikelyTopBar();
                });
                window.__x5HeaderObserver.observe(document.documentElement, {
                  childList: true, subtree: true, attributes: true,
                  attributeFilter: ['class', 'style']
                });
              }
            })();
        """
    }
}
