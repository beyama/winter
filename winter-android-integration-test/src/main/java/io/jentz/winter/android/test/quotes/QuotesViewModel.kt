package io.jentz.winter.android.test.quotes

import io.jentz.winter.android.test.scope.PresentationScope
import io.jentz.winter.android.test.model.Quote
import io.jentz.winter.android.test.model.QuoteRepository
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@PresentationScope
class QuotesViewModel @Inject constructor(repository: QuoteRepository): Disposable {

    private sealed class Result {
        object IsLoading : Result()
        data class Quotes(val quotes: List<Quote>) : Result()
    }

    private var disposable: Disposable? = null

    val viewStates: Flowable<QuotesViewState> = repository
            .getQuotes()
            .delay(FAKE_NETWORK_DELAY, TimeUnit.MILLISECONDS)
            .map<Result> { Result.Quotes(it) }
            .toFlowable()
            .startWith(Result.IsLoading)
            .scan(QuotesViewState()) { state, result ->
                when (result) {
                    is Result.IsLoading -> state.copy(isLoading = true)
                    is Result.Quotes -> state.copy(isLoading = false, quotes = result.quotes)
                }
            }
            .replay(1)
            .autoConnect(1) { disposable = it }

    override fun isDisposed(): Boolean = disposable?.isDisposed ?: false

    override fun dispose() {
        disposable?.dispose()
    }

    companion object {
        const val FAKE_NETWORK_DELAY = 1000L
    }
}