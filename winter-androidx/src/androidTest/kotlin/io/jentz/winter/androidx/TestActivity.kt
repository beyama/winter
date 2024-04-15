package io.jentz.winter.androidx

import android.os.Bundle
import androidx.activity.ComponentActivity
import io.jentz.winter.Winter

class TestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Winter.inject(this)
        super.onCreate(savedInstanceState)
    }

}