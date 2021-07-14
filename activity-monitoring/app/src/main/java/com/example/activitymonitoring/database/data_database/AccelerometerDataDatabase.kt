package com.example.activitymonitoring.database.data_database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.activitymonitoring.database.building_blocks.DataDao
import com.example.activitymonitoring.database.building_blocks.DataPacket

@Database(entities = [DataPacket::class], version = 1)
abstract class AccelerometerDataDatabase : RoomDatabase() {
    abstract fun dataDao(): DataDao
}