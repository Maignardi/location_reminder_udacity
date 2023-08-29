package com.udacity.project4.locationreminders

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    try {
        afterObserve.invoke()

        if (!latch.await(time, timeUnit)) {
            throw TimeoutException("LiveData value was never set.")
        }

    } finally {
        this.removeObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}

 val mockRemindersForTest =
     mutableListOf(
         ReminderDTO("TitleOne", "DescriptionA", "PlaceAlpha", 101.01, 101.01, "ID-01"),
         ReminderDTO("TitleTwo", "DescriptionB", "PlaceBeta", 202.02, 202.02, "ID-02"),
         ReminderDTO("TitleThree", "DescriptionC", "PlaceGamma", 303.03, 303.03, "ID-03"),
         ReminderDTO("TitleFour", "DescriptionD", "PlaceDelta", 404.04, 404.04, "ID-04"),
         ReminderDTO("TitleFive", "DescriptionE", "PlaceEpsilon", 505.05, 505.05, "ID-05")
     )
