package com.conamobile.rightswipetobackfragment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ExampleCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            navigator.addFragment(BlankFragment())
        }

        navigator.onPageScrolled = {position, positionOffset, positionOffsetPixels ->
            Log.i(TAG, "position: $position  positionOffset: $positionOffset  positionOffsetPixels: $positionOffsetPixels")
        }
        navigator.onNotifyDataChanged = { fragmentCount -> }
        navigator.onPageScrollStateChanged = { state -> }
    }

    // Intercept and block touch event when the new fragment is opening
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return if (navigator.isBlockTouchEvent)
            true
        else
            super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (navigator.fragmentCount > 1) {
            navigator.goToPreviousFragmentAndRemoveLast()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSuccess() {
        Toast.makeText(this, "CALLBACK", Toast.LENGTH_SHORT).show()
    }
}

interface ExampleCallback {
    fun onSuccess()
}

private const val TAG = "MainActivity"