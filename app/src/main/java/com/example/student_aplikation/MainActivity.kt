package com.example.student_aplikation

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.student_aplikation.databinding.ActivityMainBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val jsInterface = MyJavaScriptInterface()

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.webView) {
            settings.builtInZoomControls = true
            WebView.setWebContentsDebuggingEnabled(true)
            settings.javaScriptEnabled = true
            addJavascriptInterface(jsInterface, "HTMLOUT")
            webViewClient = MyWebViewClient()
            loadUrl(LINK)
            binding.floatingActionButton.setOnClickListener {
                loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

                showTimePicker()
                jsInterface.dataMap.clear()
            }
        }

    }


    fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(CLOCK_24H)
            .setHour(0)
            .setMinute(0)
            .setTitleText("Set how many hours (minutes) \n you want to wake up before class")
            .build()
        timePicker.show(supportFragmentManager, "xujnia")
        timePicker.addOnPositiveButtonClickListener {

            jsInterface.dataMap.forEach { entry ->
                val result = checkData(entry.key.toInt(), entry.value.toInt())
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                if (result) {
                    var hours = entry.value.dropLast(2).toInt()
                    hours -= timePicker.hour
                    intent.putExtra(AlarmClock.EXTRA_HOUR, hours)

                    var minutes = if (entry.value.length == 3) {
                        entry.value.drop(1).toInt()
                    } else {
                        entry.value.drop(2).toInt()
                    }

                    minutes -= timePicker.minute

                    intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes)

                    try {

                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.wtf(TAG, e)

                    }

                }

            }

        }

    }

    private fun checkData(data: Int, timeClass: Int): Boolean {

        val currentDate = SimpleDateFormat("yyyMdd").format(Date()).toInt()

        val currentTime = SimpleDateFormat("kkmm").format(Date()).toInt()

        return currentDate < data || (currentDate == data && currentTime < timeClass)
    }

    fun translation(value: String): Int {


        val alarmDays = listOf<Int>(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        return when (value) {
            "понедельник" -> alarmDays[0]
            "вторник" -> alarmDays[1]
            "среда" -> alarmDays[2]
            "четверг" -> alarmDays[3]
            "пятница" -> alarmDays[4]
            "суббота" -> alarmDays[5]
            else -> alarmDays[6]
        }
    }

//    fun showButton() {
//
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                myWebViewClient.state.collect { state ->
//                    if (state) {
//                        binding.floatingActionButton.visibility = View.VISIBLE
//                    }else{
//                        binding.floatingActionButton.visibility = View.VISIBLE
//                    }
//                }
//            }
//        }
//    }

    companion object {
        const val LINK =
            "https://auth.ka.edu.pl/auth/realms/KA/protocol/openid-connect/auth?client_id=APR&redirect_uri=https%3A%2F%2Fdziekanat.ka.edu.pl&response_type=code%20id_token&scope=openid%20profile%20email&state=OpenIdConnect.AuthenticationProperties%3DDFCP-UHtXDo1fbV6zJzvkNXn_LW1xY9FotBa98yvkUUdrQmu0wd2J4ZuLojJaXxd_i5vdVE-kSLlx0LdVZFBLMkMxW8Rb9NyP_zQR8YCeJ3qzVu60FKVfRbaK0Z7sBzpvm_w4cqiyaSEYRK_bbo7x2S3xE4ST7NPeHATroJPb68haemd_E0vrNiyf-V97dhv_T-4c9Iz6FQobSAuURpIBQ&response_mode=form_post&nonce=638036303387646693.NDA5NTBhY2YtYjllYS00M2RiLThiZDUtMWEyMTE0ZDIyNzE4YzY3OWVhMzgtNjAwMi00Y2I5LWI2NzUtZTEzNjgzODdmNDIw&x-client-SKU=ID_NET461&x-client-ver=5.3.0.0"
    }
}

class MyWebViewClient : WebViewClient() {

    private val _state = MutableStateFlow(false)
    val state: StateFlow<Boolean> = _state.asStateFlow()


    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.loadUrl(request.url.toString())
        return true
    }

    override fun onPageFinished(view: WebView?, url: String) {
        _state.value = url.startsWith("https://dziekanat.ka.edu.pl/Plany/PlanyGrup")
    }

}


class MyJavaScriptInterface {

    val dataMap = mutableMapOf<String, String>()


    @JavascriptInterface
    fun processHTML(html: String) {

        val htmlDocument = Jsoup.parse(html)
        val table = htmlDocument.select("table[class=dane]")
        val elements = table.select("td[class=dxgv]")
        val date = mutableListOf<String>()
        val time = mutableListOf<String>()

        var flagData = true

        elements.forEach { elem ->

            if (flagData) {


                val data =
                    elem.select("[style=border-right-width:0px;]").text().filter { it.isDigit() }

                if (data != "" && data.length == 8) {
                    date.add(data)
                    flagData = false
                }

            } else {
                val test = elem.text().replace(":", "")
                time.add(test)
                flagData = true
            }


        }
        val pairs = date.zip(time)
        pairs.forEach {
            dataMap += it
        }

    }
}

