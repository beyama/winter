package io.jentz.winter.android.sample.quotes

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.jentz.winter.Winter
import io.jentz.winter.android.sample.model.Quote
import kotlinx.android.synthetic.main.quote_item_view.view.*
import javax.inject.Inject

class QuoteItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var quoteFormatter: QuoteFormatter

    init {
        Winter.inject(this)
    }

    fun render(quote: Quote) {
        textView.text = quoteFormatter.format(quote)
    }

}