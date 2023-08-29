package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var reminderRepo: FakeDataSource

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()
        reminderRepo = FakeDataSource()
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            reminderRepo
        )
    }

    // you can use `` to write test names
    @Test
    fun `when database is empty then showNoData is true and remindersList is empty`() = mainCoroutineRule.runBlockingTest {
        // Arrange
        reminderRepo.deleteAllReminders()

        // Act
        viewModel.loadReminders()
        val isDataEmpty = viewModel.showNoData.getOrAwaitValue()
        val remindersList = viewModel.remindersList.getOrAwaitValue()

        // Assert
        Assert.assertTrue("showNoData should be true when the database is empty", isDataEmpty)
        Assert.assertTrue("remindersList should be empty when the database is empty", remindersList.isEmpty())
    }

    // you can use `` to write test names
    @Test
    fun `when an error occurs in loading reminders, then showSnackBar displays the error message`() = mainCoroutineRule.runBlockingTest {
        // Arrange
        reminderRepo.setReturnError(true)

        // Act
        viewModel.loadReminders()

        // Assert
        val snackBarMessage = viewModel.showSnackBar.getOrAwaitValue()
        Assert.assertEquals("Snackbar should display the error message 'Exception getReminder'", "getReminders Error", snackBarMessage)
    }

    // you can use `` to write test names
    @Test
    fun `when getReminders encounters an error then showSnackBar displays appropriate error message`() {
        // Arrange
        reminderRepo.setReturnError(true)

        // Act
        viewModel.loadReminders()
        val snackBarMessage = viewModel.showSnackBar.getOrAwaitValue()

        // Assert
        Assert.assertEquals("Error message in showSnackBar should match expected", "getReminders Error", snackBarMessage)
    }

    @Test
    fun `loading indicator is shown while loading reminders and hidden afterwards`() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()
        reminderRepo.deleteAllReminders()

        viewModel.loadReminders()

        Assert.assertTrue("Loading indicator should be visible", viewModel.showLoading.getOrAwaitValue())

        mainCoroutineRule.resumeDispatcher()

        Assert.assertFalse("Loading indicator should be hidden", viewModel.showLoading.getOrAwaitValue())
    }
}