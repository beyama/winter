package io.jentz.winter.androidx

import android.os.Bundle
import androidx.activity.ComponentActivity
import io.jentz.winter.Graph
import io.jentz.winter.Winter
import io.jentz.winter.delegate.provideDelegate

class TestActivity : ComponentActivity() {

    private val injector by Winter
    val graph: Graph by injector.instance()
    val applicationService: ApplicationScopedService by injector()
    val viewModelService: ViewModelScopedService by injector()
    val activityService: ActivityScopedService by injector()

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject()
        super.onCreate(savedInstanceState)
    }

}