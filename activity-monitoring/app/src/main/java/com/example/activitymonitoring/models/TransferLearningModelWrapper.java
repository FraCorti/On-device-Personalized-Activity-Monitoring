package com.example.activitymonitoring.models;

/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.example.activitymonitoring.transfer_api.AssetModelLoader;
import com.example.activitymonitoring.transfer_api.TransferLearningModel;
import com.example.activitymonitoring.transfer_api.TransferLearningModel.LossConsumer;
import com.example.activitymonitoring.transfer_api.TransferLearningModel.Prediction;

/**
 * App-layer wrapper for {@link TransferLearningModel}.
 *
 * <p>This wrapper allows to run training continuously, using start/stop API, in contrast to
 * run-once API of {@link TransferLearningModel}.
 */

enum Mode {
    Data_Collection,
    Inference,
    Training
}

public class TransferLearningModelWrapper implements Closeable {

    Mode mode;

    private final TransferLearningModel model;

    private final ConditionVariable shouldTrain = new ConditionVariable();
    private volatile LossConsumer lossConsumer;

    TransferLearningModelWrapper(Context context) {
        model =
                new TransferLearningModel(
                        new AssetModelLoader(context, "model"), Arrays.asList("Sitting", "Standing", "Walking", "Walking Upstairs"));
        new Thread(() -> {
            while (!Thread.interrupted()) {
                shouldTrain.block();
                try {
                    model.train(1, lossConsumer).get();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception occurred during model training", e.getCause());
                } catch (InterruptedException e) {
                    // no-op
                }
            }
        }).start();
    }

    public boolean isTraining() {
        return mode == Mode.Training;
    }

    public boolean isCollecting() {
        return mode == Mode.Data_Collection;
    }

    public boolean isInference() {
        return mode == Mode.Inference;
    }

    public void setDataCollectionMode() {
        mode = Mode.Data_Collection;
    }

    public void setTrainingMode() {
        mode = Mode.Training;
    }

    public void setInferenceMode() {
        mode = Mode.Inference;
    }

    // Convert input in the correct Float format
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public void startTraining() {
        enableTraining((epoch, loss) -> {
            // Log.i("Epoch:", Integer.toString(epoch));
            Log.i("Loss:", Float.toString(loss));
        });
    }

    // This method is thread-safe.
    public Future<Void> addSample(List<Float> input, String className) {
        float[] input_array = toFloatArray(input);
        return model.addSample(input_array, className);
    }

    /**
     * Pass the values obtained by the acc sensor and predict the action
     *
     * @return
     */
    public float[] predict(List<Float> input) {
        Prediction[] modelOutput = model.predict(toFloatArray(input));
        float[] predictions = new float[4];
        assert modelOutput != null;
        predictions[0] = modelOutput[0].getConfidence();
        predictions[1] = modelOutput[1].getConfidence();
        predictions[2] = modelOutput[2].getConfidence();
        predictions[3] = modelOutput[3].getConfidence();
        return predictions;
    }


    // This method is thread-safe, but blocking.
    public Prediction[] predict(float[] input) {
        return model.predict(input);
    }

    public int getTrainBatchSize() {
        return model.getTrainBatchSize();
    }

    /**
     * Start training the model continuously until {@link #disableTraining() disableTraining} is
     * called.
     *
     * @param lossConsumer callback that the loss values will be passed to.
     */
    private void enableTraining(LossConsumer lossConsumer) {
        this.lossConsumer = lossConsumer;
        shouldTrain.open();
    }

    /**
     * Stops training the model.
     */
    public void disableTraining() {
        shouldTrain.close();
    }

    /**
     * Frees all model resources and shuts down all background threads.
     */
    public void close() {
        model.close();
    }

    public void saveModel(File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            GatheringByteChannel gather = out.getChannel();
            model.saveParameters(gather);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModel(File file) {
        try {
            FileInputStream inp = new FileInputStream(file);
            ScatteringByteChannel scatter = inp.getChannel();
            model.loadParameters(scatter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}