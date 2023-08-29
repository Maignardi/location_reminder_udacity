package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource : ReminderDataSource {

    private var reminderList = mutableListOf<ReminderDTO>()
    private var returnTypeError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (returnTypeError) {
            Result.Error("getReminders Error")
        } else {
            Result.Success(reminderList)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnTypeError) {
            return Result.Error("Error retrieving reminder")
        }
        return reminderList.find { it.id == id }
            ?.let { reminder ->
                Result.Success(reminder)
            } ?: Result.Error("Failed: Reminder with ID '$id' not found or does not exist!")
    }

    fun setReturnError(shouldReturnError: Boolean) {
        this.returnTypeError = shouldReturnError
    }

    override suspend fun deleteAllReminders() {
        reminderList.clear()
    }
}