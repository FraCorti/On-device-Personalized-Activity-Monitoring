package com.example.activitymonitoring.database.building_blocks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DataDao {
    @Insert
    fun insertAll(vararg packets: DataPacket)

    @Insert
    fun insert(vararg packet: DataPacket)

    @Query("SELECT * FROM DataPacket")
    fun getAll(): List<DataPacket>

    @Query("DELETE FROM DataPacket")
    fun deleteAll()

    @Query("SELECT * FROM DataPacket ORDER BY counter DESC LIMIT 1\n")
    fun getLastEntry(): DataPacket
}