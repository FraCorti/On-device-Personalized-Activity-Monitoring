package com.example.activitymonitoring.database.building_blocks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Database Structure:
// Accelerometer: UID(Timestamp),label,x,y,z
// Gyroscope: UID(Timestamp),label,x,y,z
// Compass: UID(Timestamp),label,x,y,z
@Entity
data class DataPacket(
    @PrimaryKey val counter: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "activity label") val label: String?,
    @ColumnInfo(name = "x-axis") val x: Double?,
    @ColumnInfo(name = "y-axis") val y: Double?,
    @ColumnInfo(name = "z-axis") val z: Double?,
    @ColumnInfo(name = "position") val position: String?
)