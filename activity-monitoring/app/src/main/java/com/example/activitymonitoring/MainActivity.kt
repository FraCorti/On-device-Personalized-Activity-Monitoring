package com.example.activitymonitoring

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.*
import com.example.activitymonitoring.button_functionalities.DbFunctionality
import com.example.activitymonitoring.button_functionalities.EstimationFunctionality

import com.example.activitymonitoring.button_functionalities.RecordingFunctionality
import com.example.activitymonitoring.database.AccelerometerDatabase
import com.example.activitymonitoring.database.data_database.AccelerometerDataDatabase
import com.example.activitymonitoring.enums.FourDataLabel
import com.example.activitymonitoring.enums.TwoDataLabel
import com.example.activitymonitoring.models.ModelsHandler
import com.example.activitymonitoring.sensors.SensorHandler
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentLinkedQueue

class MainActivity : Activity(), SensorEventListener {

    lateinit var sensorManager: SensorManager
    var activeLabel = ""
    var activePositionLabel = ""
    var activeTransferLabel = ""
    var recordingStatus = false
    var estimationStatus = false
    var collectionStatus = false

    var activePage = 0

    var mAcceleration: Sensor? = null

    lateinit var dbAcc: AccelerometerDatabase
    lateinit var databAcc: AccelerometerDataDatabase

    private var fifoAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()
    private var fifoTransferAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()

    val modelHandler = ModelsHandler(fifoAcc, fifoTransferAcc)
    val sharedAccDBLock = Semaphore(1)

    companion object {
        private var instance: MainActivity? = null
    }

    fun getInstance(): MainActivity {
        if (instance == null) {
            instance = this
        }
        return instance as MainActivity
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        generalInit()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        SensorHandler(fifoAcc, fifoTransferAcc).sensorChange(event)
    }

    private fun generalInit() {
        val mainRef = getInstance()
        actionBar?.title = Html.fromHtml("<font color='#FFFFFF'>Activity Monitoring</font>")

        val textStatus = findViewById<TextView>(R.id.textviewRecordingStatus)
        if(recordingStatus) {
            textStatus.setBackgroundResource(R.color.green)
        } else {
            textStatus.setBackgroundResource(R.color.red)
        }

        val estStatus = findViewById<TextView>(R.id.textviewEstimationStatus)
        if(estimationStatus) {
            estStatus.setBackgroundResource(R.color.green)
        } else {
            estStatus.setBackgroundResource(R.color.red)
        }

        val saveButton = findViewById<Button>(R.id.buttonWriteToFile)
        if (!DbFunctionality().isExternalStorageAvailable() ||
            DbFunctionality().isExternalStorageReadOnly()) {
            saveButton.isEnabled = false
        }

        initializeActivityDropDown()
        setSpinnerMain()

        mainRef.dbAcc = AccelerometerDatabase(applicationContext)

        mainRef.databAcc = mainRef.dbAcc.createDb()
        mainRef.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mainRef.mAcceleration = mainRef.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun initializeActivityDropDown() {
        val mainRef = getInstance()
        val dropdown = findViewById<Spinner>(R.id.spinnerActivityType)

        val items: MutableList<String> = arrayListOf()
        enumValues<TwoDataLabel>().forEach { items.add(it.text) }

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter

        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mainRef.activeLabel = enumValues<TwoDataLabel>()[position].text
                val textStatus = findViewById<TextView>(R.id.textviewRecordingStatus)
                textStatus.text = mainRef.activeLabel
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                mainRef.activeLabel = ""
                val textStatus = findViewById<TextView>(R.id.textviewRecordingStatus)
                textStatus.text = mainRef.activeLabel
            }
        }
    }

    private fun initializeActivityTransferDropDown() {
        val mainRef = getInstance()
        val dropdown = findViewById<Spinner>(R.id.spinnerActivityType2)

        val items: MutableList<String> = arrayListOf()
        enumValues<FourDataLabel>().forEach { items.add(it.text) }

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter

        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mainRef.activeTransferLabel = enumValues<FourDataLabel>()[position].text
                val textStatus = findViewById<TextView>(R.id.textviewDataCollectionStatus)
                textStatus.text = mainRef.activeTransferLabel
                mainRef.modelHandler.setActivityRecordedLabel(mainRef.activeTransferLabel)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                mainRef.activeTransferLabel = ""
                val textStatus = findViewById<TextView>(R.id.textviewDataCollectionStatus)
                textStatus.text = mainRef.activeTransferLabel
            }
        }
    }

    private fun setSpinnerMain() {
        val activitySpinner = this.findViewById<Spinner>(R.id.spinnerActivityType)
        activitySpinner.setSelection(getIndex(activitySpinner, this.activeLabel))
        if(recordingStatus) {
            activitySpinner.isClickable = false
            activitySpinner.isEnabled   = false
        }
    }

    private fun setSpinnerTransfer() {
        val activitySpinner = this.findViewById<Spinner>(R.id.spinnerActivityType2)
        activitySpinner.setSelection(getIndex(activitySpinner, this.activeTransferLabel))
    }

    private fun getIndex(spinner: Spinner, myString: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(myString, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }


    fun startRecordingActivity(view: View) {
        RecordingFunctionality().startRecording()
    }

    fun stopRecordingActivity(view: View) {
        RecordingFunctionality().stopRecording()
    }

    fun writeToFileActivity(view: View) {
        DbFunctionality().writeToFile()
    }

    fun deleteDbEntriesActivity(view: View) {
        DbFunctionality().deleteDbEntries(view)
    }

    fun startEstimationActivity(view: View) {
        EstimationFunctionality().startEstimation()
    }

    fun stopEstimationActivity(view: View) {
        EstimationFunctionality().stopEstimation()
    }

    fun changeToTransferLearningPage(view: View) {
        activePage = 1
        setContentView(R.layout.transfer_page)
        initializeActivityTransferDropDown()
        setSpinnerTransfer()
    }

    fun changeToMainPage(view: View) {
        activePage = 0
        setContentView(R.layout.activity_main)
        generalInit()
    }

    fun startTrainingActivity(view: View) {
        modelHandler.startTransferTraining()
    }

    fun stopTrainingActivity(view: View) {
        modelHandler.stopTransferTraining()
    }

    fun startInferenceActivity(view: View) {
        modelHandler.startTransferInference()
    }

    fun stopInferenceActivity(view: View) {
        modelHandler.stopTransferInference()
    }

    fun startDataCollectionActivity(view: View) {
        collectionStatus = true
        findViewById<TextView>(R.id.textviewDataCollectionStatus).setBackgroundResource(R.color.green)

        mAcceleration?.also { acceleration ->
            sensorManager.registerListener(
                this,
                acceleration,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        modelHandler.startTransferCollection(applicationContext)
    }

    fun stopDataCollectionActivity(view: View) {
        collectionStatus = false
        findViewById<TextView>(R.id.textviewDataCollectionStatus).setBackgroundResource(R.color.red)
        sensorManager.unregisterListener(MainActivity().getInstance())
        modelHandler.stopTransferCollecting()
    }

    fun saveTFModelActivity(view: View) {
        modelHandler.saveTFModel(applicationContext)
    }
}