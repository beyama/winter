package io.jentz.winter.android.test.quotes

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.jentz.winter.android.test.model.Quote
import io.jentz.winter.aware.WinterAware
import io.jentz.winter.aware.instance
import kotlinx.android.synthetic.main.quote_item_view.view.*

class QuoteItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), WinterAware {

    private val quoteFormatter: QuoteFormatter = instance()

    fun render(quote: Quote) {
        textView.text = quoteFormatter.format(quote)
    }

}