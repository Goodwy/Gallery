package com.goodwy.gallery.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        // The plugin runs this against the synthetic "nonMinifiedRelease" build type,
        // which doesn't carry the ".debug" applicationIdSuffix from the debug build type.
        packageName = "com.goodwy.gallery",
    ) {
        pressHome()
        startActivityAndWait()

        // Wait for the initial folder scan to settle, then open the first folder
        // (or "All folders") and scroll the media grid, since that's the hot path
        // this profile is meant to speed up: cold start -> folder list -> media grid.
        device.wait(Until.hasObject(By.scrollable(true)), 10_000)
        val firstFolder = device.findObject(By.scrollable(true))?.children?.firstOrNull()
        firstFolder?.click()
        device.waitForIdle()

        val grid = device.wait(Until.findObject(By.scrollable(true)), 5_000)
        grid?.let {
            it.fling(androidx.test.uiautomator.Direction.DOWN)
            device.waitForIdle()
            it.fling(androidx.test.uiautomator.Direction.UP)
            device.waitForIdle()
        }
    }
}
