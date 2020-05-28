package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.lifecycle.ViewModel

class TestViewModel(
    val application: Application,
    val id: String
) : ViewModel()