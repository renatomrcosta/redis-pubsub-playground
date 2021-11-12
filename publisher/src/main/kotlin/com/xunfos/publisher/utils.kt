package com.xunfos.publisher

import io.micrometer.core.instrument.Metrics

inline fun <T> withCounter(
    metric: String,
    tags: List<String> = emptyList(),
    block: () -> T,
): T = try {
    block()
} finally {
    Metrics.counter(metric, *tags.toTypedArray()).increment()
}

