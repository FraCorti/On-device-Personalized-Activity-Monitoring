package com.example.activitymonitoring.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.activitymonitoring.MainActivity
import com.example.activitymonitoring.R
import com.example.activitymonitoring.database.building_blocks.DataPacket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.util.concurrent.ConcurrentLinkedQueue

class SensorHandler(
    accQueue: ConcurrentLinkedQueue<List<Double>>,
    accTransferQueue: ConcurrentLinkedQueue<List<Double>>
) {

    private var fifoAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()
    private var fifoTransferAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()

    init {
        fifoAcc = accQueue
        fifoTransferAcc = accTransferQueue
    }

    private fun accSensorChange(event: SensorEvent) {
        val mainRef = MainActivity().getInstance()

        // apply high-pass filter to eliminate force of gravity
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        val alpha = 0.8
        val linearAcceleration = DoubleArray(3)
        val gravity = DoubleArray(3)

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // Remove the gravity contribution with the high-pass filter.
        linearAcceleration[0] = event.values[0] - gravity[0]
        linearAcceleration[1] = event.values[1] - gravity[1]
        linearAcceleration[2] = event.values[2] - gravity[2]

        val dataList = arrayListOf<Double>()
        dataList.add(linearAcceleration[0])
        dataList.add(linearAcceleration[1])
        dataList.add(linearAcceleration[2])
        if (mainRef.estimationStatus) {
            fifoAcc.add(dataList)
        }

        if(mainRef.collectionStatus) {
            fifoTransferAcc.add(dataList)
        }

        if (!mainRef.recordingStatus) {
            return
        }

        GlobalScope.launch {
            mainRef.sharedAccDBLock.acquire()
            val lastEntry = mainRef.dbAcc.getLastEntry(mainRef.databAcc)
            var counter: Long = 0

            if (lastEntry != null) { // Dont simplify this! It is not always true!!!
                counter = lastEntry.counter + 1
            }

            val testPacketAcc =
                DataPacket(
                    counter,
                    event.timestamp,
                    mainRef.activeLabel,
                    linearAcceleration[0],
                    linearAcceleration[1],
                    linearAcceleration[2],
                    mainRef.activePositionLabel
                )

            if (mainRef.dbAcc.addToDb(mainRef.databAcc, testPacketAcc) == -1) {
                println("Adding to the AccDatabase not successfull")
            }
            mainRef.sharedAccDBLock.release()
        }
    }

    fun sensorChange(event: SensorEvent) {
        val mainRef = MainActivity().getInstance()

        if (mainRef.estimationStatus && mainRef.activePage == 0) {
            val sittingEstimation = mainRef.findViewById<TextView>(R.id.textviewSittingEstimation)
            val standingEstimation = mainRef.findViewById<TextView>(R.id.textviewStandingEstimation)
            sittingEstimation.text = mainRef.modelHandler.likelihoodSitting.toString()
            standingEstimation.text = mainRef.modelHandler.likelihoodStanding.toString()

            val estimationStatus = mainRef.findViewById<TextView>(R.id.textviewEstimationStatus)
            if (mainRef.modelHandler.likelihoodSitting > mainRef.modelHandler.likelihoodStanding) {
                estimationStatus.text = "Sitting"
            } else {
                estimationStatus.text = "Standing"
            }
        }

        if(mainRef.estimationStatus && mainRef.activePage == 1) {
            val m1ml = mainRef.findViewById<TextView>(R.id.textviewM1FirstPlaceEstimate)
            if(mainRef.modelHandler.m1ml != "") {
                m1ml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m1ml.setBackgroundResource(R.color.green)
            }
            m1ml.text = mainRef.modelHandler.m1ml

            val m1sml = mainRef.findViewById<TextView>(R.id.textviewM1SecondPlaceEstimate)
            if(mainRef.modelHandler.m1sml != "") {
                m1sml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m1sml.setBackgroundResource(R.color.green)
            }
            m1sml.text = mainRef.modelHandler.m1sml

            val m2ml = mainRef.findViewById<TextView>(R.id.textviewM2FirstPlaceEstimate)
            if(mainRef.modelHandler.m2ml != "") {
                m2ml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m2ml.setBackgroundResource(R.color.green)
            }
            m2ml.text = mainRef.modelHandler.m2ml

            val m2sml = mainRef.findViewById<TextView>(R.id.textviewM2SecondPlaceEstimate)
            if(mainRef.modelHandler.m2sml != "") {
                m2sml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m2sml.setBackgroundResource(R.color.green)
            }
            m2sml.text = mainRef.modelHandler.m2sml

            val m3ml = mainRef.findViewById<TextView>(R.id.textviewM3FirstPlaceEstimate)
            if(mainRef.modelHandler.m3ml != "") {
                m3ml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m3ml.setBackgroundResource(R.color.green)
            }
            m3ml.text = mainRef.modelHandler.m3ml

            val m3sml = mainRef.findViewById<TextView>(R.id.textviewM3SecondPlaceEstimate)
            if(mainRef.modelHandler.m3sml != "") {
                m3sml.setTextColor(ContextCompat.getColor(mainRef.applicationContext, R.color.white))
                m3sml.setBackgroundResource(R.color.green)
            }
            m3sml.text = mainRef.modelHandler.m3sml
        }

        if (mainRef.recordingStatus || mainRef.estimationStatus || mainRef.collectionStatus) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accSensorChange(event)
                }
            }
        }
    }
}