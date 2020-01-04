package io.jentz.winter.android.sample.quotes

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import io.jentz.winter.android.sample.model.Quote
import io.jentz.winter.android.sample.scope.ActivityScope
import javax.inject.Inject

@ActivityScope
class QuoteFormatter @Inject constructor() {

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