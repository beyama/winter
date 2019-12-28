package io.jentz.winter.android.test.quotes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import io.jentz.winter.android.test.R
import io.jentz.winter.android.test.viewmodel.ViewModel
import io.jentz.winter.androidx.lifecycle.autoDisposeGraph
import io.jentz.winter.aware.WinterAware
import io.jentz.winter.aware.openGraphAndInject
import io.jentz.winter.aware.inject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_quotes.*

class QuotesActivity : AppCompatActivity(), WinterAware {

    private val viewModel: ViewModel<QuotesViewState> by inject(generics = true)
    private val adapter: QuotesAdapter by inject()

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        openGraphAndInject()
        autoDisposeGraph()

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
