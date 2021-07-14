package com.example.activitymonitoring.models

import android.content.Context
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.example.activitymonitoring.MainActivity
import com.example.activitymonitoring.R
import com.example.activitymonitoring.ml.GenericModel
import com.example.activitymonitoring.ml.Model
import kotlinx.android.synthetic.main.transfer_page.view.*
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class ModelsHandler(
    accQueue: ConcurrentLinkedQueue<List<Double>>,
    transferAccQueue: ConcurrentLinkedQueue<List<Double>>
) {
    private val windowSize = 200
    private val waitTimeMilliseconds: Long = 1000

    private var model: Model? = null
    private var genericModel: GenericModel? = null

    private var transferLearningModel: TransferLearningModelWrapper? = null

    private var fifoAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()
    private var transFifoAcc: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()

    private var modelsResult: LinkedList<HashMap<String, Float>> = LinkedList()

    private lateinit var estimationLambda: Deferred<Unit>
    private lateinit var transferCollectingLambda: Deferred<Unit>

    private var activityRecordedLabel: String = ""

    init {
        fifoAcc = accQueue
        transFifoAcc = transferAccQueue
    }

    var likelihoodSitting: Float = 0F
    var likelihoodStanding: Float = 0F
    var m1ml = ""
    var m1sml = ""
    var m2ml = ""
    var m2sml = ""
    var m3ml = ""
    var m3sml = ""

    fun setActivityRecordedLabel(activityLabel :String) {
        activityRecordedLabel = activityLabel
    }

    /** Add the machine learning models to the class and start the inference process
     *
     */
    fun startEstimate(context: Context) {

        if (model == null && genericModel == null) {
            model = Model.newInstance(context)
            genericModel = GenericModel.newInstance(context)

        }

        estimationLambda = GlobalScope.async {
            try {
                while (true) {
                    delay(waitTimeMilliseconds)
                    Log.i("Queue Acc: ", (fifoAcc.size).toString())

                    if (fifoAcc.size > windowSize) {
                        val accQueue: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()
                        accQueue.addAll(fifoAcc)
                        fifoAcc.clear()

                        firstModelPreprocess(context, accQueue)
                        genericModelPreprocess(context, accQueue)

                        if (transferLearningModel != null && transferLearningModel!!.isInference) {
                            transferLearningModelEstimation(context, accQueue)
                        }
                    }
                }
            } finally {

            }
        }
    }

    fun startTransferTraining() {
        if (transferLearningModel == null) {
            transferLearningModel = TransferLearningModelWrapper(MainActivity().applicationContext)
        }
        transferLearningModel!!.setTrainingMode()
        transferLearningModel!!.startTraining()
    }

    fun stopTransferTraining() {
        if (transferLearningModel == null || !transferLearningModel!!.isTraining) {
            return
        }
        transferLearningModel!!.disableTraining()
        transferLearningModel!!.setInferenceMode()
    }

    fun startTransferCollection(context: Context) {

        if (transferLearningModel == null) {
            transferLearningModel = TransferLearningModelWrapper(context)
            transferLearningModel!!.setDataCollectionMode()
        }

        transferCollectingLambda = GlobalScope.async {
            try {
                while (true) {
                    delay(waitTimeMilliseconds)
                    Log.i("Trans Queue Acc: ", (transFifoAcc.size).toString())

                    if (transFifoAcc.size > windowSize) {
                        val accQueue: ConcurrentLinkedQueue<List<Double>> = ConcurrentLinkedQueue()
                        accQueue.addAll(transFifoAcc)
                        transFifoAcc.clear()

                        // retrieve last 600 values
                        val normalizedAccValues = divideByMaximum(accQueue)
                        var valuesList: List<Float> = LinkedList()
                        valuesList = valuesList.toMutableList()

                        var itemInserted = 0
                        if (normalizedAccValues != null) {
                            for (item in accQueue) {

                                if (itemInserted >= 600) {
                                    break
                                }
                                valuesList.add(item[0].toFloat())
                                valuesList.add(item[1].toFloat())
                                valuesList.add(item[2].toFloat())
                                itemInserted += 3
                            }
                        }
                        transferLearningModel!!.addSample(valuesList, activityRecordedLabel)
                        Log.i("New sample: ", activityRecordedLabel);
                    }
                }
            } finally {

            }
        }
    }


    fun stopTransferCollecting() {
        if (!transferLearningModel?.isCollecting()!!) {
            return
        }
        transFifoAcc.clear()
        transferCollectingLambda.cancel()
    }

    fun startTransferInference() {
        transferLearningModel?.setInferenceMode()
    }

    fun stopTransferInference() {}

    /** Interrupt the inference coroutine and clear the queues containing the sensors data
     *
     */
    fun stopEstimate() {
        estimationLambda.cancel()
        fifoAcc.clear()
    }

    private fun setModelResult(newModelsResult: LinkedList<HashMap<String, Float>>) {
        modelsResult = newModelsResult
    }

    /** Compute the features and pass them to the inference method
     *
     */
    private fun firstModelPreprocess(context: Context, accQueue: Queue<List<Double>>) {

        val f024 = getMean(accQueue)
        val f135 = calculateSD(accQueue)
        if (f024 == null ||
            f135 == null
        ) {
            return
        }

        val f0 = f024[0]
        val f1 = f135[0]
        val f2 = f024[1]
        val f3 = f135[1]
        val f4 = f024[2]
        val f5 = f135[2]

        // Double takes 8 Bytes | Int takes 4.
        // 18 Features * 8 Bytes Double = 144
        // 6 Features * 4 Bytes Int = 24
        // 72 is the input size needed for the model so either only 9 Features are needed
        // or the Data is 4 Bytes long.
        val bufferFirstModel = ByteBuffer.allocate(24)

        bufferFirstModel.putFloat(f0.toFloat())
        bufferFirstModel.putFloat(f1.toFloat())
        bufferFirstModel.putFloat(f2.toFloat())
        bufferFirstModel.putFloat(f3.toFloat())
        bufferFirstModel.putFloat(f4.toFloat())
        bufferFirstModel.putFloat(f5.toFloat())

        firstModelEstimation(context, bufferFirstModel)
    }

    private fun divideByMaximum(queue: Queue<List<Double>>): Queue<List<Double>>? {
        if (queue.isEmpty()) {
            return null
        }

        val normalizedQueue: Queue<List<Double>> = LinkedList()

        var maxX = -10000000.0
        var maxY = -10000000.0
        var maxZ = -10000000.0

        for (element in queue) {
            if (element[0] > maxX)
                maxX = element[0]
            if (element[1] > maxY)
                maxY = element[1]
            if (element[2] > maxZ)
                maxZ = element[2]
        }

        for (element in queue) {
            val arrayEl = element.toMutableList()
            arrayEl[0] = arrayEl[0] / maxX
            arrayEl[1] = arrayEl[1] / maxY
            arrayEl[2] = arrayEl[2] / maxZ
            normalizedQueue.add(arrayEl)
        }
        return normalizedQueue
    }

    private fun transferLearningModelEstimation(context: Context, accQueue: Queue<List<Double>>) {

        // retrieve last 600 values
        val normalizedAccValues = divideByMaximum(accQueue)
        var valuesList: List<Float> = LinkedList()
        valuesList = valuesList.toMutableList()

        var itemInserted = 0
        if (normalizedAccValues != null) {
            for (item in accQueue) {

                if (itemInserted >= 600) {
                    break
                }
                valuesList.add(item[0].toFloat())
                valuesList.add(item[1].toFloat())
                valuesList.add(item[2].toFloat())
                itemInserted += 3
            }
        }

        val modelOutput: FloatArray? = transferLearningModel?.predict(valuesList)

        if (modelOutput != null) {
            Log.i("---------------- Inference TRANSFER LEARNING model ------------------", "-")
            Log.i("SITTING prob:", (modelOutput[0].toString()))
            Log.i("STANDING prob:", (modelOutput[1].toString()))
            Log.i("WALKING prob:", (modelOutput[2].toString()))
            Log.i("WALKING UPSTAIRS prob:", (modelOutput[3].toString()))

            val valmap = LinkedHashMap<String, Float>()
            valmap["Sitting"] = modelOutput[0]
            valmap["Standing"] = modelOutput[1]
            valmap["Walking"] = modelOutput[2]
            valmap["Walking Upstairs"] = modelOutput[3]

            val sortedMap = valmap.toList().sortedBy { (_, v) -> v }.reversed()
            m3ml = sortedMap[0].first
            m3sml = sortedMap[1].first
        }
    }

    private fun genericModelPreprocess(context: Context, accQueue: Queue<List<Double>>) {

        // retrieve last 600 values
        val normalizedAccValues = divideByMaximum(accQueue)

        val bufferGenericModel = ByteBuffer.allocate(2400)

        var itemInsert = 0
        if (normalizedAccValues != null) {
            for (item in normalizedAccValues) {

                if (itemInsert >= 600) {
                    break
                }
                bufferGenericModel.putFloat(item[0].toFloat())
                bufferGenericModel.putFloat(item[1].toFloat())
                bufferGenericModel.putFloat(item[2].toFloat())
                itemInsert += 3
            }
        }
        genericModelEstimation(context, bufferGenericModel)
    }


    private fun firstModelEstimation(context: Context, buffer: ByteBuffer) {

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 6), DataType.FLOAT32)
        inputFeature0.loadBuffer(buffer)

        // Runs model inference and gets result.
        val outputs = model?.process(inputFeature0)
        val outputFeature0 = outputs?.outputFeature0AsTensorBuffer

        if (outputFeature0 != null) {
            Log.i("---------------- Inference SCRATCH model ------------------", "-")
            Log.i("SITTING prob:", (outputFeature0.getFloatValue(0)).toString())
            Log.i("STANDING prob:", (outputFeature0.getFloatValue(1)).toString())
            likelihoodSitting = outputFeature0.getFloatValue(0)
            likelihoodStanding = outputFeature0.getFloatValue(1)

            if(likelihoodSitting > likelihoodStanding) {
                m1ml = "Sitting"
                m1sml = "Standing"
            } else {
                m1ml = "Standing"
                m1sml = "Sitting"
            }
        }
    }

    private fun genericModelEstimation(context: Context, buffer: ByteBuffer) {

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 600), DataType.FLOAT32)
        inputFeature0.loadBuffer(buffer)

        // Runs model inference and gets result.
        val outputs = genericModel?.process(inputFeature0)
        val outputFeature0 = outputs?.outputFeature0AsTensorBuffer

        if (outputFeature0 != null) {
            Log.i("---------------- Inference GENERIC model ------------------", "-")
            Log.i("Walking prob: ", (outputFeature0.getFloatValue(0)).toString())
            Log.i("WALKING_UPSTAIRS prob: ", (outputFeature0.getFloatValue(1)).toString())
            Log.i("WALKING_DOWNSTAIRS prob: ", (outputFeature0.getFloatValue(2)).toString())
            Log.i("SITTING prob: ", (outputFeature0.getFloatValue(3)).toString())
            Log.i("STANDING prob: ", (outputFeature0.getFloatValue(4)).toString())
            Log.i("LAYING prob: ", (outputFeature0.getFloatValue(5)).toString())
            Log.i("STAND_TO_SIT prob: ", (outputFeature0.getFloatValue(6)).toString())
            Log.i("SIT_TO_STAND prob: ", (outputFeature0.getFloatValue(7)).toString())
            Log.i("SIT_TO_LIE prob: ", (outputFeature0.getFloatValue(8)).toString())
            Log.i("LIE_TO_SIT prob: ", (outputFeature0.getFloatValue(10)).toString())
            Log.i("STAND_TO_LIE prob: ", (outputFeature0.getFloatValue(11)).toString())

            val valmap = LinkedHashMap<String, Float>()
            valmap["Walking"] = outputFeature0.getFloatValue(0)
            valmap["Walking Upstairs"] = outputFeature0.getFloatValue(1)
            //valmap["Walking Downstairs"] = outputFeature0.getFloatValue(2)
            valmap["Sitting"] = outputFeature0.getFloatValue(3)
            valmap["Standing"] = outputFeature0.getFloatValue(4)
            //valmap["Laying"] = outputFeature0.getFloatValue(5)
            //valmap["Stand to Sit"] = outputFeature0.getFloatValue(6)
            //valmap["Sit to Stand"] = outputFeature0.getFloatValue(7)
            //valmap["Sit to Lie"] = outputFeature0.getFloatValue(8)
            //valmap["Lie to Sit"] = outputFeature0.getFloatValue(10)
            //valmap["Stand to Lie"] = outputFeature0.getFloatValue(11)

            val sortedMap = valmap.toList().sortedBy { (_, v) -> v }.reversed()
            m2ml = sortedMap[0].first
            m2sml = sortedMap[1].first
        }
    }

    fun getMean(queue: Queue<List<Double>>): List<Double>? {
        if (queue.isEmpty()) {
            return null
        }
        var meanX = 0.0
        var meanY = 0.0
        var meanZ = 0.0
        for (element in queue) {
            meanX += element[0]
            meanY += element[1]
            meanZ += element[2]
        }
        meanX /= queue.size
        meanY /= queue.size
        meanZ /= queue.size

        val retVal = arrayListOf<Double>()
        retVal.add(meanX)
        retVal.add(meanY)
        retVal.add(meanZ)
        return retVal
    }

    fun calculateSD(queue: Queue<List<Double>>): List<Double>? {
        if (queue.isEmpty()) {
            return null
        }

        var meanX = 0.0
        var meanY = 0.0
        var meanZ = 0.0

        var standardDeviationX = 0.0
        var standardDeviationY = 0.0
        var standardDeviationZ = 0.0

        for (element in queue) {
            meanX += element[0]
            meanY += element[1]
            meanZ += element[2]
        }
        meanX /= queue.size
        meanY /= queue.size
        meanZ /= queue.size

        for (element in queue) {
            standardDeviationX += Math.pow(element[0] - meanX, 2.0)
            standardDeviationY += Math.pow(element[1] - meanY, 2.0)
            standardDeviationZ += Math.pow(element[2] - meanZ, 2.0)
        }

        val retVal = arrayListOf<Double>()
        retVal.add(Math.sqrt(standardDeviationX / queue.size))
        retVal.add(Math.sqrt(standardDeviationY / queue.size))
        retVal.add(Math.sqrt(standardDeviationZ / queue.size))
        return retVal
    }

    fun saveTFModel(appContext: Context) {
        val fileName = "tflite_model.tflite"
        val myExternalFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        transferLearningModel!!.saveModel(myExternalFile)
    }
}