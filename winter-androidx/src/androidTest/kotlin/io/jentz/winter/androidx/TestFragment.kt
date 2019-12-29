package io.jentz.winter.androidx

import android.content.Context
import androidx.fragment.app.Fragment
import io.jentz.winter.Winter
import io.jentz.winter.delegate.inject

class TestFragment : Fragment() {

    private val title: String by inject()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Winter.inject(this)
    }

}
