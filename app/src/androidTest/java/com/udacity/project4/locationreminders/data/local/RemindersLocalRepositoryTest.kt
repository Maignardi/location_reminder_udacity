package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remiderDb: RemindersDatabase
    private lateinit var remiderRepo: RemindersLocalRepository

    @Before
    fun init() {
        remiderDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        remiderRepo =
            RemindersLocalRepository(
                remiderDb.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun closeDB() = remiderDb.close()

    @Test
    fun remiderInsertAndGetId() = runBlocking {
        val reminder = ReminderDTO(
            title = "Title",
            description = "Description",
            location = "Location",
            latitude = 126.3456,
            longitude = 635.4321
        )
        remiderRepo.saveReminder(reminder)

        val result = remiderRepo.getReminder(reminder.id) as? Result.Success

        assertNotNull("Result should be a success", result)
        assertEquals("Retrieved title should match the saved reminder's title", reminder.title, result?.data?.title)
        assertEquals("Retrieved description should match the saved reminder's description", reminder.description, result?.data?.description)
        assertEquals("Retrieved latitude should match the saved reminder's latitude", reminder.latitude, result?.data?.latitude)
        assertEquals("Retrieved longitude should match the saved reminder's longitude", reminder.longitude, result?.data?.longitude)
        assertEquals("Retrieved location should match the saved reminder's location", reminder.location, result?.data?.location)
    }

    @Test
    fun remiderSaveAndGet() = runTest {
        val reminder = ReminderDTO(
            title = "Title",
            description = "Description",
            location = "Location",
            latitude = 9.5456,
            longitude = 31.4341
        )
        remiderRepo.saveReminder(reminder)
        val retrievedList = remiderRepo.getReminders() as Result.Success
        assertNotNull("Retrieved reminders list should not be null", retrievedList)
        assertTrue("Retrieved reminders list should contain the saved reminder", retrievedList.data.contains(reminder))
        assertEquals("Retrieved reminders list should have the correct size", 1, retrievedList.data.size)
    }


    @Test
    fun deleteAllRemiders() = runBlocking {
        val reminder = ReminderDTO(
            title = "Title",
            description = "Description",
            location = "Location",
            latitude = 9.3456,
            longitude = 64.4321
        )
        remiderRepo.saveReminder(reminder)
        remiderRepo.deleteAllReminders()
        val result = remiderRepo.getReminder(reminder.id)
        assertTrue("Result should be an error", result is Result.Error)
        result as Result.Error
        assertEquals("Error message should indicate that the reminder was not found", "Reminder not found!", result.message)
    }


}