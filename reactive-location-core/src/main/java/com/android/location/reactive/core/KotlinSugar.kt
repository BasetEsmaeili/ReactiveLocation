package com.android.location.reactive.core

fun <T> lazyFast(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)