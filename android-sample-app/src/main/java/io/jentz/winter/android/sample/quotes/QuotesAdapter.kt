package io.jentz.winter.android.sample.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.jentz.winter.android.sample.R
import io.jentz.winter.android.sample.model.Quote
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.inject.InjectConstructor

@ActivityScope
@InjectConstructor
class QuotesAdapter(
    private val inflater: LayoutInflater
) : RecyclerView.Adapter<QuotesAdapter.ViewHolder>() {

    class ViewHolder(
        val quoteItemView: QuoteItemView
    ) : RecyclerView.ViewHolder(quoteItemView)

    var list: List<Quote>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = list?.count() ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(inflater.inflate(R.layout.quote_item_view, parent, false) as QuoteItemView)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quote = list?.get(position) ?: return
        holder.quoteItemView.render(quote)
    }

}