package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OmaniCurriculumData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and verify Omani curriculum stories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("قريتي كتابي الصغير", appName)

        assertEquals(5, OmaniCurriculumData.curriculumStories.size)
        assertEquals(5, OmaniCurriculumData.preDiagnosticStory.questions.size)
        assertEquals(5, OmaniCurriculumData.postDiagnosticStory.questions.size)
        assertTrue(OmaniCurriculumData.getAllGlossaryWords().isNotEmpty())
    }
}
