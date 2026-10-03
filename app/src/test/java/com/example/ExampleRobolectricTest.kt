package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getAppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StudyTrack", appName)
    }

    @Test
    fun `task completion toggle status`() {
        val originalTask = TaskItem(
            id = 1,
            title = "Study Room Database",
            description = "Complete task list component",
            isCompleted = false,
            priority = TaskPriority.URGENT,
            category = TaskCategory.STUDIES
        )
        assertFalse(originalTask.isCompleted)

        val toggledTask = originalTask.copy(isCompleted = !originalTask.isCompleted)
        assertTrue(toggledTask.isCompleted)
    }

    @Test
    fun `all 7 languages are supported and have valid translations`() {
        val supportedLanguages = listOf(
            AppLanguage.ENGLISH,
            AppLanguage.ARABIC,
            AppLanguage.FRENCH,
            AppLanguage.RUSSIAN,
            AppLanguage.GERMAN,
            AppLanguage.CHINESE,
            AppLanguage.JAPANESE
        )

        assertEquals(7, supportedLanguages.size)
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.FRENCH.isRtl)

        // Verify Arabic translations
        val arStrings = getAppStrings(AppLanguage.ARABIC)
        assertEquals("إدارة المهام 📝", arStrings.tasksTitle)
        assertEquals("قائمة المهام", arStrings.taskList)
        assertEquals("مكتملة", arStrings.completed)

        // Verify French translations
        val frStrings = getAppStrings(AppLanguage.FRENCH)
        assertEquals("Gestionnaire de tâches 📝", frStrings.tasksTitle)
        assertEquals("Liste des tâches", frStrings.taskList)

        // Verify Russian translations
        val ruStrings = getAppStrings(AppLanguage.RUSSIAN)
        assertEquals("Менеджер задач 📝", ruStrings.tasksTitle)
        assertEquals("Список задач", ruStrings.taskList)

        // Verify German translations
        val deStrings = getAppStrings(AppLanguage.GERMAN)
        assertEquals("Aufgaben-Manager 📝", deStrings.tasksTitle)
        assertEquals("Aufgabenliste", deStrings.taskList)

        // Verify Chinese translations
        val zhStrings = getAppStrings(AppLanguage.CHINESE)
        assertEquals("任务管理器 📝", zhStrings.tasksTitle)
        assertEquals("任务列表", zhStrings.taskList)

        // Verify Japanese translations
        val jaStrings = getAppStrings(AppLanguage.JAPANESE)
        assertEquals("タスク管理 📝", jaStrings.tasksTitle)
        assertEquals("タスク一覧", jaStrings.taskList)
    }
}
