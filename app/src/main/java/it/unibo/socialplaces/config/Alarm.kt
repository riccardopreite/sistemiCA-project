package it.unibo.socialplaces.config

import android.app.AlarmManager
import android.os.SystemClock
import kotlinx.datetime.Clock

object Alarm {
    var isDevelopment: Boolean = true

    /**
     * Returns [AlarmManager.RTC_WAKEUP] in development mode, [AlarmManager.ELAPSED_REALTIME_WAKEUP] in production mode.
     */
    fun alarmType(): Int = if(isDevelopment) AlarmManager.RTC_WAKEUP else AlarmManager.ELAPSED_REALTIME_WAKEUP

    /**
     * Returns the current time + 10 seconds in milliseconds in development mode, 15 minutes after the system boot in production mode.
     */
    fun firstRunMillis(): Long = if(isDevelopment) Clock.System.now().toEpochMilliseconds() + 10000 else SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES

    /**
     * Returns the time windows which after that the alarm is triggered.
     */
    fun repeatingRunTimeWindow(): Long = if(isDevelopment) AlarmManager.INTERVAL_FIFTEEN_MINUTES else AlarmManager.INTERVAL_HOUR * 3
}