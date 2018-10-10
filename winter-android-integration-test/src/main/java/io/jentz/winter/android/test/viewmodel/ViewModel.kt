package io.jentz.winter.android.test.viewmodel

import io.reactivex.Flowable

interface ViewModel<VS> {
    fun toFlowable(): Flowable<VS>
}