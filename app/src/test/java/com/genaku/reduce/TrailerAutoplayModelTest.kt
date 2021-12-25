package com.genaku.reduce


import app.cash.turbine.test
import com.genaku.reduce.trailer.*
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class TrailerAutoplayModelTest : FreeSpec() {

    private val coroutineDispatcher = TestCoroutineDispatcher()

    private val eventTransmitter = object : TrailerEventTransmitter {

        var lastEvent: TrailerEvent? = null
            private set

        override fun sendEvent(event: TrailerEvent) {
            lastEvent = event
        }

        fun clear() {
            lastEvent = null
        }
    }

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        coroutineDispatcher.runBlockingTest {
            "engine" - {
                val previewPeriod = 2000L
                val bannerPeriod = 5000L
                val repo = mockk<TrailerRepository>()
                val engine = TrailerAutoplayModel(
                    repository = repo,
                    eventTransmitter = eventTransmitter,
                    previewPeriod = previewPeriod,
                    bannerPeriod = bannerPeriod,
                    dispatcher = coroutineDispatcher
                )
                eventTransmitter.clear()

                "initial state" - {
                    engine.start(this@runBlockingTest)
                    engine.state.test {
                        expectItem() shouldBe IdleState
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "happy trailer case" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } returns "url"
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        expectItem() shouldBe PreviewState("1", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("1", "url")
                        engine.stopBanner("1")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "trailer with exception on getting playUrl" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } throws IOException("501")
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayableState("1", "")
                        advanceTimeBy(bannerPeriod - previewPeriod)
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "repeating start banner while already started" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } returns "url"
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        engine.startBanner("1")
                        expectItem() shouldBe PreviewState("1", "url")
                        engine.startBanner("1")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("1", "url")
                        engine.startBanner("1")
                        engine.stopBanner("1")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "start next banner while another is in preview" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } returns "url"
                    every { repo.stopRequest(any()) } just runs
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        expectItem() shouldBe PreviewState("1", "url")
                        engine.startBanner("2")
                        expectItem() shouldBe PreviewState("2", "")
                        expectItem() shouldBe PreviewState("2", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("2", "url")
                        engine.stopBanner("2")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "start next banner while another is in playable state" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl("1") } throws IOException("501")
                    coEvery { repo.getTrailerUrl("2") } returns "url"
                    every { repo.stopRequest(any()) } just runs
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayableState("1", "")
                        engine.startBanner("2")
                        expectItem() shouldBe PreviewState("2", "")
                        expectItem() shouldBe PreviewState("2", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("2", "url")
                        engine.stopBanner("2")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }

                "start next banner while another is playing" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } returns "url"
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        expectItem() shouldBe PreviewState("1", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("1", "url")
                        engine.startBanner("2")
                        expectItem() shouldBe PreviewState("2", "")
                        eventTransmitter.lastEvent shouldBe StopTrailerEvent("1")
                        expectItem() shouldBe PreviewState("2", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("2", "url")
                        engine.stopBanner("2")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                    }
                    engine.stop()
                }

                "2 happy trailer cases" {
                    engine.start(this@runBlockingTest)
                    coEvery { repo.getTrailerUrl(any()) } returns "url"
                    engine.state.test {
                        engine.startBanner("1")
                        expectItem() shouldBe IdleState
                        expectItem() shouldBe PreviewState("1", "")
                        expectItem() shouldBe PreviewState("1", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("1", "url")
                        engine.stopBanner("1")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent

                        engine.startBanner("2")
                        expectItem() shouldBe PreviewState("2", "")
                        expectItem() shouldBe PreviewState("2", "url")
                        advanceTimeBy(previewPeriod)
                        expectItem() shouldBe PlayingState("2", "url")
                        engine.stopBanner("2")
                        expectItem() shouldBe IdleState
                        eventTransmitter.lastEvent shouldBe NextBannerEvent
                        cancelAndIgnoreRemainingEvents()
                    }
                    engine.stop()
                }
            }
        }
    }
}