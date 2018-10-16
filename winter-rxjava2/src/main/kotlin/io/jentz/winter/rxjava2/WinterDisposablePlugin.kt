package io.jentz.winter.rxjava2

import io.jentz.winter.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Winter plugin that adds all singletons that are instances of [Disposable] to a
 * [CompositeDisposable] and disposes that on graph dispose.
 */
object WinterDisposablePlugin {

    private val initializingComponentPlugin: InitializingComponentPlugin = { _, builder ->
        builder.constant(CompositeDisposable())
    }

    private val postConstructPlugin: PostConstructPlugin = { graph, scope, _, instance ->
        if (scope == Scope.Singleton && instance is Disposable) {
            graph.instance<CompositeDisposable>().add(instance)
        }
    }

    private val disposePlugin: GraphDisposePlugin = { graph ->
        graph.instance<CompositeDisposable>().dispose()
    }

    /**
     * Register this on [WinterPlugins].
     */
    fun install() {
        WinterPlugins.addInitializingComponentPlugin(initializingComponentPlugin)
        WinterPlugins.addPostConstructPlugin(postConstructPlugin)
        WinterPlugins.addGraphDisposePlugin(disposePlugin)
    }

    /**
     * Unregister this from [WinterPlugins].
     */
    fun uninstall() {
        WinterPlugins.removeInitializingComponentPlugin(initializingComponentPlugin)
        WinterPlugins.removePostConstructPlugin(postConstructPlugin)
        WinterPlugins.removeGraphDisposePlugin(disposePlugin)
    }

}
