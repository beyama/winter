package io.jentz.winter.android.test

import io.jentz.winter.Scope
import io.jentz.winter.WinterPlugins
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Winter plugin that adds all singletons that are instances of [Disposable] to an [CompositeDisposable] and disposes
 * that on graph dispose.
 */
fun installWinterDisposablePlugin() {
    WinterPlugins.addInitializingComponentPlugin { _, builder ->
        builder.constant(CompositeDisposable())
    }

    WinterPlugins.addPostConstructPlugin { graph, scope, _, instance ->
        if (scope == Scope.Singleton && instance is Disposable) {
            graph.instance<CompositeDisposable>().add(instance)
        }
    }

    WinterPlugins.addGraphDisposePlugin { graph ->
        graph.instance<CompositeDisposable>().dispose()
    }

}