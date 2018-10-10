package io.jentz.winter.android.test.viewmodel

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class TestViewModel<VS> : ViewModel<VS>, Disposable {

    private var isDisposed = false

    val downstream: Subject<VS> = BehaviorSubject.create()

    override fun toFlowable(): Flowable<VS> = downstream.toFlowable(BackpressureStrategy.LATEST)

    override fun isDisposed(): Boolean = isDisposed

    override fun dispose() {
        isDisposed = true
    }
}