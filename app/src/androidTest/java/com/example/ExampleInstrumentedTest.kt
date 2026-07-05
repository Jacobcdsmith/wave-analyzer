package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

  @Test
  fun useAppContext() {
    val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.aistudio.waveanalyzer.abcdz", context.packageName)
  }
}
