package io.jentz.winter.androidx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.jentz.winter.GFactory
import io.jentz.winter.Graph

@PublishedApi
internal class WinterViewModelFactory(
    private val graph: Graph,
    private val factory: GFactory<ViewModel>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = factory.invoke(graph) as T

}
