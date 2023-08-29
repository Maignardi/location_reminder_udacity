package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest {

    private lateinit var remiderRepo: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var appContext: Application

    @Before
    fun init(){
        stopKoin()
        appContext = getApplicationContext()
        val fakeModule = module {
            viewModel{
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        startKoin {
            modules(listOf(fakeModule))
        }
        remiderRepo = appContext.get()
        runBlocking { remiderRepo.deleteAllReminders() }
    }

    @After
    fun cleanUp(){
        stopKoin()
    }

    @Test
    fun remiderNullDataListDisplayed(){
        launchFragmentInContainer <ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun remiderDataListDisplayed(): Unit = runBlocking {
        val reminderA = ReminderDTO("Title A",
            "description A",
            "location A",
            235.23,
            2344.12)
        val reminderB = ReminderDTO("Title B",
            "description B",
            "location B",
            12341234.23,
            3334.12)
        remiderRepo.saveReminder(reminderA)
        remiderRepo.saveReminder(reminderB)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
    }
}