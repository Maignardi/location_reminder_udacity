package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
@SmallTest
class RemindersDaoTest {

//    TODO: Add testing implementation to the RemindersDao.kt

    private lateinit var remiderDd: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initializeDatabase() {
        remiderDd = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDB() = remiderDd.close()

    @Test
    fun insertRemiderAndRetrive() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "Title",
            description = "Description",
            location = "Location",
            latitude = 123.45,
            longitude = 678.90
        )

        remiderDd.reminderDao().saveReminder(reminder)
        val retrievedReminder = remiderDd.reminderDao().getReminderById(reminder.id)

        assertNotNull("Retrieved reminder should not be null", retrievedReminder)
        assertEquals("Retrieved ID should match the inserted reminder's ID", reminder.id, retrievedReminder?.id)
        assertEquals("Retrieved title should match the inserted reminder's title", reminder.title, retrievedReminder?.title)
        assertEquals("Retrieved description should match the inserted reminder's description", reminder.description, retrievedReminder?.description)
        assertEquals("Retrieved location should match the inserted reminder's location", reminder.location, retrievedReminder?.location)
        assertEquals("Retrieved latitude should match the inserted reminder's latitude", reminder.latitude, retrievedReminder?.latitude)
        assertEquals("Retrieved longitude should match the inserted reminder's longitude", reminder.longitude, retrievedReminder?.longitude)
    }


    @Test
    fun getAllSavedRemiders() = runBlockingTest {
        val reminder1 = ReminderDTO("Title A", "Description A", "Location A", 35.6895, 139.6917)
        val reminder2 = ReminderDTO("Title B", "Description B", "Location B", 48.8566, 2.3522)
        remiderDd.reminderDao().apply {
            saveReminder(reminder1)
            saveReminder(reminder2)
        }

        val reminders = remiderDd.reminderDao().getReminders()

        assertNotNull("Retrieved reminders list should not be null", reminders)
        assertEquals("Retrieved reminders list should contain all saved reminders", 2, reminders.size)
    }

    @Test
    fun deleteRemiders() = runTest {
        val remider = ReminderDTO("Title", "Description", "Location", 35.6895, 139.6917)
        remiderDd.reminderDao().apply {
            saveReminder(remider)
        }
        remiderDd.reminderDao().deleteAllReminders()
        val remindersList = remiderDd.reminderDao().getReminders()
        assertTrue("Database should be empty after deleting all reminders", remindersList.isEmpty())
    }

}