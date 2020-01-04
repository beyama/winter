package io.jentz.winter.android.sample.quotes

import io.jentz.winter.android.sample.model.Quote

data class QuotesViewState(
        val isLoading: Boolean = false,
        val quotes: List<Quote> = emptyList()
)