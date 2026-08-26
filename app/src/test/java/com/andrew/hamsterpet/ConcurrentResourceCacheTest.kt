package com.andrew.hamsterpet

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcurrentResourceCacheTest {
    @Test
    fun concurrent_requests_for_one_resource_load_it_once() {
        val cache = ConcurrentResourceCache<String>()
        val loadCount = AtomicInteger()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        val results = (1..16).map {
            executor.submit<String> {
                start.await()
                cache.get(7) {
                    loadCount.incrementAndGet()
                    "atlas"
                }
            }
        }
        start.countDown()

        assertEquals(List(16) { "atlas" }, results.map { it.get(5, TimeUnit.SECONDS) })
        assertEquals(1, loadCount.get())
        executor.shutdownNow()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
    }

    @Test
    fun different_resources_are_cached_independently() {
        val cache = ConcurrentResourceCache<String>()
        var loadCount = 0

        assertEquals("adult", cache.get(1) { loadCount++; "adult" })
        assertEquals("baby", cache.get(2) { loadCount++; "baby" })
        assertEquals("adult", cache.get(1) { loadCount++; "replacement" })

        assertEquals(2, loadCount)
    }
}
