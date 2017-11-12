package io.jentz.winter.android

import android.content.Context
import android.view.View
import io.jentz.winter.Injector

fun Injector.inject(context: Context) = AndroidInjection.inject(context, this)

fun Injector.inject(view: View) = AndroidInjection.inject(view, this)