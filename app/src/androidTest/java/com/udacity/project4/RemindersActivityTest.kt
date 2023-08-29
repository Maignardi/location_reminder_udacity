package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.junit5.AutoCloseKoinTest
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private var dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var activityRule = ActivityScenarioRule(RemindersActivity::class.java)

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
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
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResources() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun snackbarTestWithTitleNull() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Navigate to add reminder screen
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        TimeUnit.SECONDS.sleep(3) // Wait for the screen transition

        // Try to save a reminder without entering a title
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify that the Snackbar is displayed with the correct error message
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        // Close the activity scenario
        activityScenario.close()
    }

    @Test
    fun snackbarTestWithLocationNull() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Navigate to the add reminder screen
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Enter mock data for the reminder title and description
        onView(withId(R.id.reminderTitle)).perform(
            ViewActions.typeText("Mock test location"),
            ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("Mock description"),
            ViewActions.closeSoftKeyboard()
        )

        TimeUnit.SECONDS.sleep(3) // Wait for user interaction (e.g., location selection)

        // Try to save a reminder without selecting a location
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify that the Snackbar is displayed with the correct error message for location
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        // Close the activity scenario
        activityScenario.close()
    }

    @Test
    fun toastTestForSavedLocation() = runBlocking {
        // Launch the RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the add reminder FAB (Floating Action Button)
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Enter mock data for the reminder title and description
        onView(withId(R.id.reminderTitle)).perform(
            ViewActions.typeText("Mock Title"),
            ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("Mock description"),
            ViewActions.closeSoftKeyboard()
        )

        // Navigate to the location selection screen
        onView(withId(R.id.selectLocation)).perform(ViewActions.click())

        // Simulate a location selection on the map
        onView(withId(R.id.map)).perform(ViewActions.click())
        TimeUnit.SECONDS.sleep(3) // Wait for the location to be selected
        onView(withId(R.id.saveButton)).perform(ViewActions.click()) // Save the selected location

        TimeUnit.SECONDS.sleep(3) // Wait for the reminder to be saved
        onView(withId(R.id.saveReminder)).perform(ViewActions.click()) // Click on the save reminder button

        // Check that the 'reminder saved' toast message is displayed
        onView(withText(R.string.reminder_saved)).inRoot(ToastMatcher())
            .check(matches(ViewMatchers.isDisplayed()))

        // Close the activity scenario after the test
        activityScenario.close()
    }
}
