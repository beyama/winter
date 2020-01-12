package io.jentz.winter.android.sample.model

import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.inject.InjectConstructor
import io.reactivex.Single

@ApplicationScope
@InjectConstructor
class QuoteRepository {

    companion object {
        val quotes: List<Quote> = listOf(
                Quote(
                        "Dr. Seuss",
                        "Don't cry because it's over, smile because it happened."
                ),
                Quote(
                        "Marilyn Monroe",
                        "I'm selfish, impatient and a little insecure. I make mistakes, " +
                                "I am out of control and at times hard to handle. But if you can't " +
                                "handle me at my worst, then you sure as hell don't deserve me at my " +
                                "best."
                ),
                Quote(
                        "Oscar Wilde",
                        "Be yourself; everyone else is already taken."
                ),
                Quote(
                        "Albert Einstein",
                        "Two things are infinite: the universe and human stupidity; " +
                                "and I'm not sure about the universe."
                )
        )
    }

    fun getQuotes(): Single<List<Quote>> = Single.just(quotes)

}