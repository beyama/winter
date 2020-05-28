package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class TestSavedstateViewModel(
    val application: Application,
    val handle: SavedStateHandle,
    val id: String
) : ViewModel()