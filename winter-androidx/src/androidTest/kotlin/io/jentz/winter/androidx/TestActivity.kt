package io.jentz.winter.androidx

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.jentz.winter.Winter
import io.jentz.winter.delegate.inject

class TestActivity : FragmentActivity() {

    val title: String by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Winter.inject(this)
        super.onCreate(savedInstanceState)
    }

}