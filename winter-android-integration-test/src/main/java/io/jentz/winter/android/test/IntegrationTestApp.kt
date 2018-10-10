package io.jentz.winter.android.test

import android.app.Application
import io.jentz.winter.ComponentBuilder.SubcomponentIncludeMode.DoNotInclude
import io.jentz.winter.GraphRegistry
import io.jentz.winter.Injection
import io.jentz.winter.android.AndroidPresentationScopeAdapter
import io.jentz.winter.android.generatedComponent
import io.jentz.winter.android.test.quotes.QuotesViewModel
import io.jentz.winter.android.test.quotes.QuotesViewState
import io.jentz.winter.android.test.scope.ActivityScope
import io.jentz.winter.android.test.scope.ApplicationScope
import io.jentz.winter.android.test.viewmodel.ViewModel
import io.jentz.winter.component

class IntegrationTestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        installWinterDisposablePlugin()

        Injection.adapter = AndroidPresentationScopeAdapter()

        GraphRegistry.applicationComponent = component {
            include(generatedComponent, subcomponentIncludeMode = DoNotInclude)
            include(generatedComponent.subcomponent(ApplicationScope::class), subcomponentIncludeMode = DoNotInclude)

            subcomponent("presentation") {
                singleton<ViewModel<QuotesViewState>>(generics = true) {
                    QuotesViewModel(instance())
                }

                subcomponent("activity") {
                    include(generatedComponent.subcomponent(ActivityScope::class))
                }
            }
        }

        Injection.createGraph(this)
    }

}