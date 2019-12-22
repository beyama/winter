package io.jentz.winter.rxjava2

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.SimplePlugin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Winter plugin that adds a [CompositeDisposable] to every graph, adds all singleton scoped
 * instances which implement [Disposable] to it and disposes the [CompositeDisposable] when
 * the [Graph] gets disposed.
 */
object WinterDisposablePlugin : SimplePlugin() {
    override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
        builder.constant(CompositeDisposable())
    }

    override fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
        if (scope == Scope.Singleton && instance is Disposable) {
            graph.instance<CompositeDisposable>().add(instance)
        }
    }

    override fun graphDispose(graph: Graph) {
        graph.instance<CompositeDisposable>().dispose()
    }
}

fun WinterApplication.installDisposablePlugin() {
    plugins += WinterDisposablePlugin
}

fun WinterApplication.uninstallDisposablePlugin() {
    plugins -= WinterDisposablePlugin
}
