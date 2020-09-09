package com.github.itxiaox.qrscan

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jishimed.zxing.ui.CaptureFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var captureFragment : CaptureFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_start.setOnClickListener {
            captureFragment = CaptureFragment()
            captureFragment.setListener {
                Log.i("test", "onCreate-setListener:$it ")
                captureFragment.dismiss()
                tv_show_result.text = "扫描结果：$it"
            }
            captureFragment.setInputClickListener { captureFragment, msg ->
                Log.i("test", "onCreate-inputClickListener:$msg ")
            }
            captureFragment.show(this@MainActivity.supportFragmentManager,CaptureFragment::class.java.name)
        }
    }
}