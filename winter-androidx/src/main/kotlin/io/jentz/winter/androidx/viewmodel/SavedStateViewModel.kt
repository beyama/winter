package io.jentz.winter.androidx.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import io.jentz.winter.ClassTypeKey
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.delegate.InjectedProperty
import io.jentz.winter.delegate.injectLazy

/**
 * Creates a property delegate for a [ViewModel] instance of type `R`.
 * This will provide a [SavedStateHandle] dependency to the view model factory block.
 *
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return The created [InjectedProperty].
 */
inline fun <reified R : ViewModel> injectSavedStateViewModel(
    defaultArgs: Bundle? = null,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = injectLazy<Graph>().map { graph ->
    graph.savedStateViewModel<R>(defaultArgs = defaultArgs, block = block)
}

/**
 * Creates a property delegate for a [ViewModel] instance of type `R` that is owned by an activity.
 * This will provide a [SavedStateHandle] dependency to the view model factory block.
 *
 * Useful in Fragments to create/retrieve shared view models.
 *
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return The created [InjectedProperty].
 */
inline fun <reified R : ViewModel> injectActivitySavedStateViewModel(
    defaultArgs: Bundle? = null,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = injectLazy<Graph>().map { graph ->
    val activity: ComponentActivity = graph.instance()
    graph.savedStateViewModel<R>(activity.viewModelStore, activity, defaultArgs, block)
}

/**
 * Get a [ViewModel] instance of type `R` from this [Graph].
 * This will provide a [SavedStateHandle] dependency to the view model factory block.
 *
 * The [Graph] must provide [ViewModelStore] or a [ViewModelStore] must be provided via argument.
 * Also the [Graph] must provide [SavedStateRegistryOwner] or a [SavedStateRegistryOwner] must be
 * provided via argument.
 *
 * @param viewModelStore The [ViewModelStore] to use (default: retrieved from this [Graph]).
 * @param savedStateRegistryOwner The [SavedStateRegistryOwner] to use (default: retrieved from
 * this [Graph]).
 * @param defaultArgs A default argument bundle for the saved state handle.
 * @param block An optional builder block to pass runtime dependencies to the view model.
 *
 * @return R
 */
inline fun <reified R : ViewModel> Graph.savedStateViewModel(
    viewModelStore: ViewModelStore = instance(),
    savedStateRegistryOwner: SavedStateRegistryOwner = instance(),
    defaultArgs: Bundle? = null,
    noinline block: ComponentBuilderBlock? = null
): R = ViewModelProvider(
    viewModelStore,
    savedStateViewModelFactory(savedStateRegistryOwner, defaultArgs, block)
).get(R::class.java)

/**
 * Create a [ViewModelProvider.Factory] which retrieves the [ViewModel] instances from this [Graph].
 * The factory will provide a [SavedStateHandle] dependency to the view model factory block.
 *
 * @param savedStateRegistryOwner The [SavedStateRegistryOwner] to use (default: retrieved from
 * this [Graph]).
 * @param defaultArgs A default argument bundle for the saved state handle.
 * @param block An optional builder block to pass runtime dependencies to the view model.
 */
fun Graph.savedStateViewModelFactory(
    savedStateRegistryOwner: SavedStateRegistryOwner = instance(),
    defaultArgs: Bundle? = null,
    block: ComponentBuilderBlock? = null
): ViewModelProvider.Factory =
    WinterSavedStateViewModelFactory(this, savedStateRegistryOwner, defaultArgs, block)

internal class WinterSavedStateViewModelFactory(
    private val graph: Graph,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
    private val block: ComponentBuilderBlock? = null
) : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val typeKey = ClassTypeKey(modelClass)
        return graph.instanceByKey(typeKey) {
            block?.invoke(this)
            constant(handle)
        }
    }

}
