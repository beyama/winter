package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.lifecycle.ViewModel

class SomeViewModelDependency

class TestViewModel(val application: Application, dependency: SomeViewModelDependency) : ViewModel()