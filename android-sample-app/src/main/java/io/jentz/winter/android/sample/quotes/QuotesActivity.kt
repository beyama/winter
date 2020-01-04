package io.jentz.winter.android.sample.quotes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import io.jentz.winter.Winter
import io.jentz.winter.android.sample.R
import io.jentz.winter.android.sample.viewmodel.ViewModel
import io.jentz.winter.delegate.inject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_quotes.*

class QuotesActivity : AppCompatActivity() {

    private val viewModel: ViewModel<QuotesViewState> by inject(generics = true)
    private val adapter: QuotesAdapter by inject()

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Winter.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quotes)

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

    private fun render(viewState: QuotesViewState) {
        progressIndicatorView.isVisible = viewState.isLoading
        adapter.list = viewState.quotes
    }

}
