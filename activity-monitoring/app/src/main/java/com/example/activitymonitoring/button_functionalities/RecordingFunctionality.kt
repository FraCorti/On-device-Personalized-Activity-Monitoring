package com.example.activitymonitoring.button_functionalities

import android.hardware.SensorManager
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import com.example.activitymonitoring.MainActivity
import com.example.activitymonitoring.R

class RecordingFunctionality {
    fun startRecording() {
        val mainRef = MainActivity().getInstance()
        if(mainRef.recordingStatus) {
            return
        }

        val activitySpinner = mainRef.findViewById<Spinner>(R.id.spinnerActivityType)
        activitySpinner.isClickable = false
        activitySpinner.isEnabled = false


        val textStatus = mainRef.findViewById<TextView>(R.id.textviewRecordingStatus)

        mainRef.mAcceleration?.also { acceleration ->
            mainRef.sensorManager.registerListener(
                mainRef,
                acceleration,
                SensorManager.SENSOR_DELAY_GAME)
        }

        mainRef.recordingStatus = true
        textStatus.setBackgroundResource(R.color.green)
    }

    fun stopRecording() {
        val mainRef = MainActivity().getInstance()
        if(!mainRef.recordingStatus) {
            return
        }

        val activitySpinner = mainRef.findViewById<Spinner>(R.id.spinnerActivityType)
        activitySpinner.isClickable = true
        activitySpinner.isEnabled = true

        val textStatus = mainRef.findViewById<TextView>(R.id.textviewRecordingStatus)
        textStatus.setBackgroundResource(R.color.red)
        mainRef.recordingStatus = false
        mainRef.sensorManager.unregisterListener(MainActivity().getInstance())
    }
}