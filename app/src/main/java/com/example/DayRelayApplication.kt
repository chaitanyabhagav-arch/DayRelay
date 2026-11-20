package com.example

import android.app.Application
import com.example.data.local.BridgePreferencesDataStore
import com.example.data.local.DayRelayDatabase
import com.example.data.repository.PlannerRepository
import com.example.network.LaptopPushClient
import com.example.scheduler.ExactAlarmScheduler
import com.example.scheduler.NotificationHelper

class DayRelayApplication : Application() {

    lateinit var database: DayRelayDatabase
        private set

    lateinit var bridgePrefs: BridgePreferencesDataStore
        private set

    lateinit var pushClient: LaptopPushClient
        private set

    lateinit var scheduler: ExactAlarmScheduler
        private set

    lateinit var repository: PlannerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        database = DayRelayDatabase.getInstance(this)
        bridgePrefs = BridgePreferencesDataStore(this)
        pushClient = LaptopPushClient()
        scheduler = ExactAlarmScheduler(this)
        repository = PlannerRepository(
            dao = database.plannerDao(),
            bridgePrefs = bridgePrefs,
            pushClient = pushClient,
            scheduler = scheduler
        )
    }
}
