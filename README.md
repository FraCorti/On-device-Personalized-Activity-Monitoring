# On-device-Personalized-Activity-Monitoring

For the Mobile Computing [Laboratory](https://online.tugraz.at/tug_online/wbLv.wbShowLVDetail?pStpSpNr=216713) we developed an application able to predict what kind of activity the user is currently doing. This was achieved by deploying multiple machine learning models (feedforward neural networks) by exploiting the [Tensorflow Lite](https://www.tensorflow.org/lite/guide) library. More information about the project and the algorithms we implemented can be found in the [report](https://github.com/FraCorti/On-device-Personalized-Activity-Monitoring/blob/main/Mobile_Computing_Laboratory___report.pdf).


The two activities that can be predicted with our first model
are Sitting and Standing. The four activities which can be predicted from the second and the third model are Sitting, Standing, Walking and
Walking Down the Stairs.
These activities cover a wide variety of movements and can
be therefore predicted correctly in everyday situations. As the
estimation process requires a model to be able to
predict, training data, as well as verification data, needs to be
gathered, which the application supports.
Furthermore, the application can train and use a
personalized transfer learning model. This makes it possible
for the user to predict his movements with the smartphone
in different positions not covered in the training set. This is
done without having to retrain the whole model but by only
updating the weights of the last part, called head.
The main implemented techniques are gradient descent with backpropagation, k-fold cross-validation and grid-search (parallel and sequential).