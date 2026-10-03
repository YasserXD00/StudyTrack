package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.StudyTrackRepository
import com.example.ui.viewmodel.TaskStatusFilter
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: StudyTrackRepository
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudyTrackRepository(db)
        viewModel = TaskViewModel(application, repository, SharingStarted.Eagerly)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `adding task persists to room database`() = runTest(testDispatcher) {
        val initialCount = repository.tasks.first().size

        // Add a new task through ViewModel business logic
        viewModel.addTask(
            title = "Prepare Android Project",
            description = "Test TaskViewModel business logic",
            priority = TaskPriority.URGENT,
            category = TaskCategory.STUDIES
        ).join()

        // Verify task is added to Room database
        val updatedTasks = repository.tasks.first()
        assertEquals(initialCount + 1, updatedTasks.size)

        val added = updatedTasks.find { it.title == "Prepare Android Project" }
        assertNotNull(added)
        assertEquals("Test TaskViewModel business logic", added!!.description)
        assertEquals(TaskPriority.URGENT, added.priority)
        assertEquals(TaskCategory.STUDIES, added.category)
        assertFalse(added.isCompleted)
    }

    @Test
    fun `blank task title validation prevents insertion`() = runTest(testDispatcher) {
        val countBefore = repository.tasks.first().size

        // Attempt to add with blank title
        viewModel.addTask(title = "   ", description = "Invalid").join()

        // Assert no task was inserted in Room database
        val countAfter = repository.tasks.first().size
        assertEquals(countBefore, countAfter)
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `updating task in room database modifies fields`() = runTest(testDispatcher) {
        // Add task
        viewModel.addTask(
            title = "Original Title",
            description = "Original Description"
        ).join()

        val task = repository.tasks.first().first { it.title == "Original Title" }

        // Update task through ViewModel
        viewModel.updateTask(
            task.copy(
                title = "Updated Title",
                description = "Updated Description",
                priority = TaskPriority.IMPORTANT
            )
        ).join()

        // Verify update in Room database
        val tasksAfterUpdate = repository.tasks.first()
        val updated = tasksAfterUpdate.first { it.id == task.id }
        assertEquals("Updated Title", updated.title)
        assertEquals("Updated Description", updated.description)
        assertEquals(TaskPriority.IMPORTANT, updated.priority)
    }

    @Test
    fun `toggling completion status updates room database`() = runTest(testDispatcher) {
        // Add task
        viewModel.addTask(
            title = "Toggle Task",
            description = "Check status"
        ).join()

        val task = repository.tasks.first().first { it.title == "Toggle Task" }
        assertFalse(task.isCompleted)

        // Toggle to completed through ViewModel
        viewModel.toggleTaskCompletion(task).join()

        val completedTask = repository.tasks.first().first { it.id == task.id }
        assertTrue(completedTask.isCompleted)

        // Toggle back to incomplete
        viewModel.toggleTaskCompletion(completedTask).join()

        val incompleteAgain = repository.tasks.first().first { it.id == task.id }
        assertFalse(incompleteAgain.isCompleted)
    }

    @Test
    fun `status filter and search work correctly on tasks`() = runTest(testDispatcher) {
        viewModel.addTask("Math Homework", "Calculus exercises").join()
        viewModel.addTask("History Reading", "Chapter 4").join()

        val allTasks = repository.tasks.first()
        assertEquals(2, allTasks.size)

        // Test search query filtering
        viewModel.setSearchQuery("Calculus")
        assertEquals("Calculus", viewModel.searchQuery.value)

        // Test status filtering
        viewModel.setStatusFilter(TaskStatusFilter.PENDING)
        assertEquals(TaskStatusFilter.PENDING, viewModel.statusFilter.value)

        viewModel.resetFilters()
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(TaskStatusFilter.ALL, viewModel.statusFilter.value)
    }
}
