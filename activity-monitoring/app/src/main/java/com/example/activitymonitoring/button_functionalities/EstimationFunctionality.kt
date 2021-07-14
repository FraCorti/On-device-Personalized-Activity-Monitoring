package com.example.activitymonitoring.button_functionalities

import android.hardware.SensorManager
import android.widget.TextView
import com.example.activitymonitoring.MainActivity
import com.example.activitymonitoring.R

class EstimationFunctionality {
    fun startEstimation() {
        val mainRef = MainActivity().getInstance()

        if (mainRef.estimationStatus) {
            return
        }

        mainRef.estimationStatus = true
        mainRef.modelHandler.startEstimate(MainActivity().getInstance().applicationContext)
        mainRef.findViewById<TextView>(R.id.textviewEstimationStatus)?.setBackgroundResource(R.color.green)

        mainRef.mAcceleration?.also { acceleration ->
            mainRef.sensorManager.registerListener(
                mainRef,
                acceleration,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }


     fun stopEstimation() {
        val mainRef = MainActivity().getInstance()

        if (!mainRef.estimationStatus) {
            return
        }

        mainRef.estimationStatus = false
        mainRef.modelHandler.stopEstimate()
        mainRef.findViewById<TextView>(R.id.textviewEstimationStatus)?.setBackgroundResource(R.color.red)
        mainRef.findViewById<TextView>(R.id.textviewEstimationStatus)?.text = ""
        mainRef.findViewById<TextView>(R.id.textviewSittingEstimation)?.text = ""
        mainRef.findViewById<TextView>(R.id.textviewStandingEstimation)?.text = ""

        val m1ml = mainRef.findViewById<TextView>(R.id.textviewM1FirstPlaceEstimate)
        m1ml?.text= ""
        m1ml?.setBackgroundResource(R.color.white)
        val m1sml = mainRef.findViewById<TextView>(R.id.textviewM1SecondPlaceEstimate)
        m1sml?.text= ""
        m1sml?.setBackgroundResource(R.color.white)
        val m2ml = mainRef.findViewById<TextView>(R.id.textviewM2FirstPlaceEstimate)
        m2ml?.text= ""
        m2ml?.setBackgroundResource(R.color.white)
        val m2sml = mainRef.findViewById<TextView>(R.id.textviewM2SecondPlaceEstimate)
        m2sml?.text= ""
        m2sml?.setBackgroundResource(R.color.white)
        val m3ml = mainRef.findViewById<TextView>(R.id.textviewM3FirstPlaceEstimate)
        m3ml?.text= ""
        m3ml?.setBackgroundResource(R.color.white)
        val m3sml = mainRef.findViewById<TextView>(R.id.textviewM3SecondPlaceEstimate)
        m3sml?.text= ""
        m3sml?.setBackgroundResource(R.color.white)

        mainRef.sensorManager.unregisterListener(MainActivity().getInstance())
    }
}