package io.jentz.winter.android.sample.viewmodel

import io.reactivex.Flowable

interface ViewModel<VS> {
    fun toFlowable(): Flowable<VS>
}