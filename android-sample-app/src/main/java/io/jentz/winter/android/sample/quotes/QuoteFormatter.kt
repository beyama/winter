package io.jentz.winter.android.sample.quotes

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import io.jentz.winter.android.sample.model.Quote
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.inject.InjectConstructor

@ActivityScope
@InjectConstructor
class QuoteFormatter {

    fun format(quote: Quote): Spannable = SpannableStringBuilder().let {
        it.bold { append("\"") }
        it.append(quote.quote)
        it.bold { append("\"") }
        it.append("\n")
        it.bold {
            append("- ")
            append(quote.originator)
        }
    }


}