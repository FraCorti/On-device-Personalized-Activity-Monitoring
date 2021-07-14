package com.example.activitymonitoring.database

import android.content.Context
import android.os.Environment
import androidx.room.Room
import com.example.activitymonitoring.database.building_blocks.DataPacket
import com.example.activitymonitoring.database.data_database.AccelerometerDataDatabase
import com.example.activitymonitoring.enums.FourDataLabel
import java.io.File
import java.io.FileOutputStream

class AccelerometerDatabase(context: Context) {
    private var appContext = context

    fun createDb(): AccelerometerDataDatabase {
        return Room.databaseBuilder(
            appContext,
            AccelerometerDataDatabase::class.java, "accelerometer-database"
        ).build()
    }

    fun addToDb(db: AccelerometerDataDatabase, packet: DataPacket): Int {

        if (!checkLabel(packet.label)) {
            println("Accelerometer Label not valid!")
            return -1
        }

        db.dataDao().insert(packet)
        return 0
    }

    private fun checkLabel(label_to_check: String?): Boolean {
        return enumValues<FourDataLabel>().any { it.text == label_to_check }
    }

    fun deleteDBEntries(db: AccelerometerDataDatabase): Boolean {
        return try {
            db.dataDao().deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAllDBEntries(db: AccelerometerDataDatabase): List<DataPacket> {
        return db.dataDao().getAll()
    }

    fun getLastEntry(db: AccelerometerDataDatabase): DataPacket {
        return db.dataDao().getLastEntry()
    }

    fun storeToFile(db: AccelerometerDataDatabase, timestamp: Long): Boolean {
        try {
            val fileName = "acc_data_$timestamp.txt"
            val myExternalFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fileOutputStream = FileOutputStream(myExternalFile)

            val entries = db.dataDao().getAll()

            for(entry in entries) {
                var entryString = "Accelerometer,"
                entryString += entry.counter.toString() + ","
                entryString += entry.label + ","
                entryString += entry.timestamp.toString() + ","
                entryString += entry.x.toString() + ","
                entryString += entry.y.toString() + ","
                entryString += entry.z.toString() + ","
                entryString += entry.position
                entryString += '\n'
                fileOutputStream.write(entryString.toByteArray())
            }
            fileOutputStream.close()

        } catch (e: Exception) {
            println("Couldnt write to file!")
            return false
        }
        return true
    }
}