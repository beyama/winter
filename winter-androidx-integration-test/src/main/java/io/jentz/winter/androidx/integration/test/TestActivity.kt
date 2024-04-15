package io.jentz.winter.androidx.integration.test

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.jentz.winter.Winter

class TestActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Winter.inject(this)
        super.onCreate(savedInstanceState)
    }

}