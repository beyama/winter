package io.jentz.winter.android.test.quotes

import io.jentz.winter.android.test.model.Quote

data class QuotesViewState(
        val isLoading: Boolean = false,
        val quotes: List<Quote> = emptyList()
)