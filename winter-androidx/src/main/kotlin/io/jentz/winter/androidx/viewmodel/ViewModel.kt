package io.jentz.winter.androidx.viewmodel

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.jentz.winter.ClassTypeKey
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.delegate.InjectedProperty
import io.jentz.winter.delegate.injectLazy

/**
 * Creates a property delegate for a [ViewModel] instance of type `R`.
 *
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return The created [InjectedProperty].
 */
inline fun <reified R : ViewModel> injectViewModel(
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = injectLazy<Graph>().map { it.viewModel<R>(block = block) }

/**
 * Creates a property delegate for a [ViewModel] instance of type `R` that is owned by an activity.
 *
 * Useful in Fragments to create/retrieve shared view models.
 *
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return The created [InjectedProperty].
 */
inline fun <reified R : ViewModel> injectActivityViewModel(
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = injectLazy<Graph>().map { graph ->
    graph.viewModel<R>(graph.instance<ComponentActivity>().viewModelStore, block)
}

/**
 * Get a [ViewModel] instance of type `R` from this [Graph].
 * The [Graph] must provide [ViewModelStore] or a [ViewModelStore] must be provided via argument.
 *
 * @param viewModelStore The [ViewModelStore] to use (default: retrieved from this [Graph]).
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return R
 */
inline fun <reified R : ViewModel> Graph.viewModel(
    viewModelStore: ViewModelStore = instance(),
    noinline block: ComponentBuilderBlock? = null
): R = ViewModelProvider(viewModelStore, viewModelFactory(block)).get(R::class.java)

/**
 * Create a [ViewModelProvider.Factory] which retrieves the [ViewModel] instances from this [Graph].
 *
 * @param block An optional builder block to pass runtime dependencies to the view model.
 */
fun Graph.viewModelFactory(
    block: ComponentBuilderBlock? = null
): ViewModelProvider.Factory = WinterViewModelFactory(this, block)

internal class WinterViewModelFactory(
    private val graph: Graph,
    private val block: ComponentBuilderBlock? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        graph.instanceByKey(ClassTypeKey(modelClass), block)

}
