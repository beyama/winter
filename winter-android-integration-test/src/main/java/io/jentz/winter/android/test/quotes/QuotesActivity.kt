package io.jentz.winter.android.test.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.jentz.winter.Injection
import io.jentz.winter.Injector
import io.jentz.winter.android.test.R
import io.jentz.winter.android.test.model.Quote
import io.jentz.winter.android.test.viewmodel.ViewModel
import io.jentz.winter.androidx.lifecycle.autoDisposeDependencyGraph
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_quotes.*

class QuotesActivity : AppCompatActivity() {

    private val injector = Injector()
    private val viewModel: ViewModel<QuotesViewState> by injector.instance(generics = true)
    private lateinit var adapter: Adapter
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Injection.createGraphAndInject(this, injector)
        autoDisposeDependencyGraph()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quotes)

        adapter = Adapter(layoutInflater)
        listView.adapter = adapter
        listView.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        disposable = viewModel.toFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::render)
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onDestroy() {
        Injection.disposeGraph(this)
        super.onDestroy()
    }

    private fun render(viewState: QuotesViewState) {
        progressIndicatorView.isVisible = viewState.isLoading
        adapter.list = viewState.quotes
    }

    private class ViewHolder(
            val quoteItemView: QuoteItemView
    ) : RecyclerView.ViewHolder(quoteItemView)

    private class Adapter(
            val inflater: LayoutInflater
    ) : RecyclerView.Adapter<ViewHolder>() {

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

}
