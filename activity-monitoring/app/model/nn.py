import pandas as pd
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras import backend as K
from tensorflow.keras import activations
from tensorflow.keras.layers import Conv1D, Dropout, Dense, Flatten, MaxPooling1D, BatchNormalization, \
    GlobalMaxPooling1D, AveragePooling1D
from tensorflow.keras import Sequential
from keras.utils import to_categorical
import matplotlib as mpl
import matplotlib.pyplot as plt
from sklearn.utils import class_weight
from numpy.random import seed
import math
import sklearn.metrics
from sklearn.utils import shuffle
import scipy.stats as stats
import datetime
from sklearn.model_selection import train_test_split
import json
from sklearn.metrics import confusion_matrix, roc_curve, auc, roc_auc_score, f1_score
from sklearn.model_selection import train_test_split
from sklearn import metrics
from sklearn.metrics import confusion_matrix
from pylab import savefig
from mpl_toolkits.axes_grid1 import make_axes_locatable
from keras import optimizers
from keras.models import Sequential
from sklearn.metrics import plot_confusion_matrix
from keras.layers import Dense
from keras.layers import Dropout
import seaborn as sn


def feature_normalize(dataset):
    mu = np.mean(dataset, axis=0)
    sigma = np.std(dataset, axis=0)
    return (dataset - mu) / sigma


label_value = {
    "Sitting": 0,
    "Standing": 1
}

SEED = 83

"""
"""


def extract_windows_feature(label, accelerometer_data):
    # create empty dataframe to store feature values
    feature_dataset_labeled = pd.DataFrame(columns=range(0, 5))

    # empty dataframe to store normalized values
    normalized_accelerometer = pd.DataFrame(columns=range(0, 3))

    pd.options.mode.chained_assignment = None

    # normalize the features dividing by the max
    # normalized_accelerometer.loc[:, 0] = accelerometer_data.iloc[:, 0] / accelerometer_data.iloc[:, 0].max()
    # normalized_accelerometer.loc[:, 1] = accelerometer_data.iloc[:, 1] / accelerometer_data.iloc[:, 1].max()
    # normalized_accelerometer.loc[:, 2] = accelerometer_data.iloc[:, 2] / accelerometer_data.iloc[:, 2].max()

    # extract features from accelerometer
    feature_dataset_labeled.loc[:, 0] = accelerometer_data.iloc[:, 0].rolling(window=200).mean()
    feature_dataset_labeled.loc[:, 1] = accelerometer_data.iloc[:, 0].rolling(window=200).std()
    feature_dataset_labeled.loc[:, 2] = accelerometer_data.iloc[:, 1].rolling(window=200).mean()
    feature_dataset_labeled.loc[:, 3] = accelerometer_data.iloc[:, 1].rolling(window=200).std()
    feature_dataset_labeled.loc[:, 4] = accelerometer_data.iloc[:, 2].rolling(window=200).mean()
    feature_dataset_labeled.loc[:, 5] = accelerometer_data.iloc[:, 2].rolling(window=200).std()

    "https://www.analyticsvidhya.com/blog/2019/12/6-powerful-feature-engineering-techniques-time-series/"

    # add label
    label = np.tile(label, len(feature_dataset_labeled))
    feature_dataset_labeled.insert(len(feature_dataset_labeled.columns), "", label, True)

    # drop rows with Nan values
    feature_dataset_labeled = feature_dataset_labeled.dropna()

    return feature_dataset_labeled


def get_train_test(sensors_dataset):
    sensors_dataset = shuffle(sensors_dataset)

    # Split dataset into training and test set
    label = sensors_dataset.pop(sensors_dataset.columns[-1])
    X_train, X_test, y_train, y_test = train_test_split(sensors_dataset, label, test_size=0.2)

    y_train = to_categorical(y_train)
    y_test = to_categorical(y_test)

    return X_train, X_test, y_train, y_test


def train_model(X_train, X_test, y_train, y_test):
    classifier = Sequential()

    reg_d = 0.05  # hyperparameter
    epoch = 60
    batch_size = X_train.shape[0]

    classifier.add(Dense(units=10, activation='relu', input_dim=6, kernel_regularizer=tf.keras.regularizers.l2(reg_d)))
    classifier.add(Dense(units=2, activation='softmax', kernel_regularizer=tf.keras.regularizers.l2(reg_d)))

    opt = optimizers.Adam(learning_rate=0.05)
    classifier.compile(optimizer=opt,
                       loss='categorical_crossentropy',
                       metrics='categorical_accuracy')
    classifier.fit(X_train, y_train, shuffle=True, validation_data=(X_test, y_test), batch_size=batch_size,
                   epochs=epoch)

    # plot loss
    plt.plot(range(epoch), classifier.history.history['loss'], classifier.history.history['val_loss'])
    plt.title('model loss', fontsize=15)
    plt.ylabel('loss', fontsize=15)
    plt.xlabel('epoch', fontsize=15)
    plt.legend(['train', 'test'], loc='upper right', fontsize=13)
    plt.grid(True)
    plt.show()

    # plot accuracy
    plt.plot(range(epoch), classifier.history.history['categorical_accuracy'],
             classifier.history.history['val_categorical_accuracy'])
    plt.title('model accuracy', fontsize=15)
    plt.legend(['train', 'validation'], loc='lower right', fontsize=15)
    plt.ylabel('accuracy', fontsize=15)
    plt.xlabel('epoch', fontsize=15)
    plt.grid(True)
    plt.show()

    # plot confusion matrix
    rounded_predictions = np.argmax(classifier.predict(X_test), axis=-1)
    rounded_labels = np.argmax(y_test, axis=1)
    cm = confusion_matrix(rounded_labels, rounded_predictions)
    print(cm)
    mpl.style.use('seaborn')
    conf_arr = np.array([[cm[1][0], cm[1][1]], [cm[0][0], cm[0][1]]])

    df_cm = pd.DataFrame(conf_arr,
                         index=['Sitting', 'Standing'],
                         columns=['Standing', 'Sitting'])

    fig = plt.figure()
    plt.clf()

    ax = fig.add_subplot(111)
    ax.set_aspect(1)

    res = sn.heatmap(df_cm, annot=True, vmin=0, vmax=80, fmt='d', cmap='Blues', annot_kws={"size": 11}, cbar=False,
                     linewidths=0.05, linecolor='black')

    res.invert_yaxis()
    plt.ylabel('True label', fontsize=13)
    plt.xlabel('Predicted label', fontsize=13)
    plt.yticks([0.5, 1.5], ['Sitting', 'Standing'], va='center')

    plt.title('Test Confusion Matrix', fontsize=13)

    plt.savefig('test_confusion_matrix.png', dpi=100, bbox_inches='tight')
    return classifier


def convert_save_model(model):
    # Convert the model in Tensorflow lite version
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    with open('model.tflite', 'wb') as f:
        f.write(tflite_model)


# working only with the same number of sensor data for each sensors !
def load_sensor_dataset(activity, sensor_name, path):  # , path_compass, path_gyroscope
    # Read the .csv files
    sensor_data = pd.read_csv(path)
    sensor_data.drop(columns=[sensor_name, activity], inplace=True, axis=1)
    sensor_data.drop(sensor_data.columns[[0, 1]], inplace=True, axis=1)  # drop timestamp column
    return sensor_data


if __name__ == '__main__':
    accelerometer_sitting = load_sensor_dataset(activity='Sitting', sensor_name='Accelerometer',
                                                path='data/Sitting/Accelerometer_Sitting.csv')

    sitting_feature_dataset = extract_windows_feature(label=label_value['Sitting'],
                                                      accelerometer_data=accelerometer_sitting)

    accelerometer_standing = load_sensor_dataset(activity='Standing', sensor_name='Accelerometer',
                                                 path='data/Standing/Accelerometer_Standing.csv')

    standing_feature_dataset = extract_windows_feature(label=label_value['Standing'],
                                                       accelerometer_data=accelerometer_standing)

    X_train, X_test, y_train, y_test = get_train_test(standing_feature_dataset.append(sitting_feature_dataset))

    model = train_model(X_train, X_test, y_train, y_test)
    convert_save_model(model)
