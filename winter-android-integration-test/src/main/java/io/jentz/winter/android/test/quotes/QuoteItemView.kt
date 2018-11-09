package io.jentz.winter.android.test.quotes

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.jentz.winter.android.instance
import io.jentz.winter.android.test.model.Quote
import kotlinx.android.synthetic.main.quote_item_view.view.*

class QuoteItemView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val quoteFormater: QuoteFormater = instance()

    fun render(quote: Quote) {
        textView.text = quoteFormater.format(quote)
    }

}