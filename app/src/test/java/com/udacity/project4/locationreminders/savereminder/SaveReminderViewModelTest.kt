package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var reminderRepo: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()
        reminderRepo = FakeDataSource()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            reminderRepo
        )
    }

    @Test
    fun `show error when location is null during reminder validation and save`() {
        // Arrange
        val reminderDataItem = ReminderDataItem(
            title = "Test location",
            description = "This is a test location",
            location = null,
            latitude = 35.6895,
            longitude = 139.6917
        )

        // Act
        viewModel.validateAndSaveReminder(reminderDataItem)

        // Assert
        val snackbarMessageResourceId = viewModel.showSnackBarInt.getOrAwaitValue()
        Assert.assertEquals(
            "Expected error message resource ID when location is null",
            R.string.err_select_location,
            snackbarMessageResourceId
        )
    }

    @Test
    fun `loading indicator is shown while saving a reminder and hidden afterwards`() {
        mainCoroutineRule.runBlockingTest {
            // Arrange
            val fakeReminder = ReminderDataItem("reminder", "desc", "location",
                100.00, 100.00, "1")
            mainCoroutineRule.pauseDispatcher()

            // Act
            viewModel.saveReminder(fakeReminder)

            // Assert
            Assert.assertTrue("Loading indicator should be visible while saving a reminder",
                viewModel.showLoading.getOrAwaitValue())

            // Act
            mainCoroutineRule.resumeDispatcher()

            // Assert
            Assert.assertFalse("Loading indicator should be hidden after saving a reminder",
                viewModel.showLoading.getOrAwaitValue())
        }
    }

    @Test
    fun `clearing ViewModel fields sets all reminder attributes to null`() {
        // Arrange
        viewModel.apply {
            reminderTitle.value = "Test Title"
            reminderDescription.value = "Test Description"
            reminderSelectedLocationStr.value = "Test Location"
            selectedPOI.value = PointOfInterest(LatLng(45.0, -75.0), "Ottawa", "Parliament Hill")
            latitude.value = 45.0
            longitude.value = -75.0
        }

        // Act
        viewModel.onClear()

        // Assert
        listOf(
            viewModel.reminderTitle,
            viewModel.reminderDescription,
            viewModel.reminderSelectedLocationStr,
            viewModel.selectedPOI,
            viewModel.latitude,
            viewModel.longitude
        ).forEach { field ->
            Assert.assertNull("Field ${field.value} should be null after onClear", field.value)
        }
    }
}