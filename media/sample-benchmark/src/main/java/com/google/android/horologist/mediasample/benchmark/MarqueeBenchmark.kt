/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMetricApi::class)

package com.google.android.horologist.mediasample.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.test.filters.LargeTest
import com.google.android.horologist.media.benchmark.MediaApp
import com.google.android.horologist.media.benchmark.MediaControllerHelper
import com.google.android.horologist.media.benchmark.MediaItems.buildMediaItem
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@LargeTest
class MarqueeBenchmark {
    private val includePower: Boolean = false

    val mediaApp: MediaApp = TestMedia.MediaSampleApp

    @get:Rule
    public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

    public lateinit var mediaControllerFuture: ListenableFuture<MediaBrowser>

    @Test
    public fun marquee() {
        val intro = buildMediaItem(
            "1",
            "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3",
            "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
            "Intro - The Way Of Waking Up (feat. Alan Watts)",
            "The Kyoto Connection"
        )

        measurePlayerScreen(intro)
    }

    @Test
    public fun noMarquee() {
        val intro = buildMediaItem(
            "1",
            "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3",
            "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
            "Intro",
            "The Kyoto Connection"
        )

        measurePlayerScreen(intro)
    }

    private fun measurePlayerScreen(intro: MediaItem) {
        benchmarkRule.measureRepeated(
            packageName = mediaApp.packageName,
            metrics = metrics(),
            compilationMode = CompilationMode.Partial(),
            iterations = 3,
            startupMode = StartupMode.WARM,
            setupBlock = {
                mediaControllerFuture = MediaControllerHelper.lookupController(
                    mediaApp.playerComponentName
                )

                // Wait for service
                mediaControllerFuture.get()
            }
        ) {
            startActivityAndWait()

            val mediaController = mediaControllerFuture.get()

            runBlocking {
                withContext(Dispatchers.Main) {
                    mediaController.setMediaItems(List(10) { intro })
                    mediaController.volume = 0.1f

                    delay(10.seconds)

                    mediaController.prepare()
                    mediaController.play()

                    while (mediaController.currentPosition < 10_000) {
                        delay(1.seconds)
                    }

                    mediaController.seekToNextMediaItem()

                    while (mediaController.currentPosition < 15_000) {
                        delay(1.seconds)
                    }
                }
            }
        }
    }

    public open fun metrics(): List<Metric> = listOfNotNull(
        StartupTimingMetric(),
        FrameTimingMetric(),
        if (includePower) PowerMetric(type = PowerMetric.Type.Battery()) else null
    )
}