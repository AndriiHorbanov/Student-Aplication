package com.example.student_aplikation

import  java.util.Calendar
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.view.MotionEvent
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.student_aplikation.databinding.ActivityMainBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jsoup.Jsoup
import java.util.*




class MainActivity : AppCompatActivity() {

    private val jsInterface = MyJavaScriptInterface()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.webView) {
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true
            addJavascriptInterface(jsInterface, "HTMLOUT")
            webViewClient = MyWebViewClient()
            loadUrl(LINK)
            binding.floatingActionButton.setOnClickListener {
                loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
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
            jsInterface.minusPare = Pair(timePicker.hour, timePicker.minute)

            openSomeActivityForResult()
        }

    }

    fun openSomeActivityForResult() {

        val intent = Intent(AlarmClock.ACTION_SET_ALARM)

        if (jsInterface.dataMap.isEmpty()) return

        when (jsInterface.dataMap.last().get(Calendar.DAY_OF_WEEK)) {
            1 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.SATURDAY))
            2 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.MONDAY))
            3 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.TUESDAY))
            4 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.WEDNESDAY))
            5 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.THURSDAY))
            6 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.FRIDAY))
            7 -> intent.putExtra(AlarmClock.EXTRA_DAYS, intArrayOf(Calendar.SATURDAY))
        }

        intent.putExtra(AlarmClock.EXTRA_HOUR, calkulate().first)
        intent.putExtra(AlarmClock.EXTRA_MINUTES, calkulate().second)
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true)

        jsInterface.dataMap.removeLast()

        resultLauncher.launch(intent)

    }


    fun calkulate(): Pair<Int, Int> {

        var hour = jsInterface.dataMap.last().get(Calendar.HOUR_OF_DAY)
        var minute = jsInterface.dataMap.last().get(Calendar.MINUTE)

        if (minute - jsInterface.minusPare.second < 0) {
            hour -= 1
            minute = 60 - (jsInterface.minusPare.second - minute)
            hour -= jsInterface.minusPare.first
        } else {
            minute -= jsInterface.minusPare.second
            hour -= jsInterface.minusPare.first
        }

        return Pair(hour, minute)

    }

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->


            openSomeActivityForResult()

        }


    companion object {
        const val LINK =
            "https://auth.ka.edu.pl/auth/realms/KA/protocol/openid-connect/auth?client_id=APR&redirect_uri=https%3A%2F%2Fdziekanat.ka.edu.pl&response_type=code%20id_token&scope=openid%20profile%20email&state=OpenIdConnect.AuthenticationProperties%3DDFCP-UHtXDo1fbV6zJzvkNXn_LW1xY9FotBa98yvkUUdrQmu0wd2J4ZuLojJaXxd_i5vdVE-kSLlx0LdVZFBLMkMxW8Rb9NyP_zQR8YCeJ3qzVu60FKVfRbaK0Z7sBzpvm_w4cqiyaSEYRK_bbo7x2S3xE4ST7NPeHATroJPb68haemd_E0vrNiyf-V97dhv_T-4c9Iz6FQobSAuURpIBQ&response_mode=form_post&nonce=638036303387646693.NDA5NTBhY2YtYjllYS00M2RiLThiZDUtMWEyMTE0ZDIyNzE4YzY3OWVhMzgtNjAwMi00Y2I5LWI2NzUtZTEzNjgzODdmNDIw&x-client-SKU=ID_NET461&x-client-ver=5.3.0.0"
    }


}


class MyWebViewClient : WebViewClient() {

    private val _state = MutableStateFlow(false)
//    val state: StateFlow<Boolean> = _state.asStateFlow()


    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.loadUrl(request.url.toString())
        return true
    }

    override fun onPageFinished(view: WebView?, url: String) {
        _state.value = url.startsWith("https://dziekanat.ka.edu.pl/Plany/PlanyGrup")
    }

}

fun convertToData(dateString: String): Calendar {
    val year = dateString.dropLast(4).toInt()
    val month = dateString.drop(4).dropLast(2).toInt()
    val day = dateString.drop(6).toInt()
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone("GMT")
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    return calendar
}

fun convertToTime(time: String): Calendar {
    val (hours, minutes) = time.split(":")
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone("GMT")
    calendar.set(Calendar.HOUR_OF_DAY, hours.toInt())
    calendar.set(Calendar.MINUTE, minutes.toInt())
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}


class MyJavaScriptInterface {

    val dataMap = mutableListOf<Calendar>()
    var minusPare: Pair<Int, Int> = Pair(0, 0)


    @JavascriptInterface
    fun processHTML(html: String) {
        val htmlDocument = Jsoup.parse(html)
        val table = htmlDocument.select("table[class=dane]")
        val elements = table.select("td[class=dxgv]")
        val date = mutableListOf<Calendar>()
        val time = mutableListOf<Calendar>()

        var flagData = true

        elements.forEach { elem ->

            if (flagData) {


                val data =
                    elem.select("[style=border-right-width:0px;]").text().filter { it.isDigit() }


                if (data != "" && data.length == 8) {

                    date.add(convertToData(data))
                    flagData = false
                }

            } else {
                time.add(convertToTime(elem.text()))
                flagData = true
            }


        }


        val pairs = date.zip(time)
        val newDate = Calendar.getInstance()
        newDate.add(Calendar.WEEK_OF_YEAR, 1)


        pairs.forEach {
            val zipCalendars = Calendar.getInstance()
            zipCalendars.set(Calendar.YEAR, it.first.get(Calendar.YEAR))
            zipCalendars.set(Calendar.MONTH, it.first.get(Calendar.MONTH))
            zipCalendars.set(Calendar.DAY_OF_MONTH, it.first.get(Calendar.DAY_OF_MONTH))
            zipCalendars.set(Calendar.MINUTE, it.second.get(Calendar.MINUTE))
            zipCalendars.set(Calendar.HOUR_OF_DAY, it.second.get(Calendar.HOUR_OF_DAY))



            if (Calendar.getInstance().before(zipCalendars) && zipCalendars.before(newDate)) {
                dataMap.add(zipCalendars)
            }


        }


    }
}

