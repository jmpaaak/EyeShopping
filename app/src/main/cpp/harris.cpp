//
// Created by jongmin on 2017-05-18.
//
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG    "NDK_TEST"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

/// Global variables
Mat src, src_gray;
Mat dst, dst_norm, dst_norm_scaled;
int thresh = 200;
int max_thresh = 255;

char * source_window = "Source image";
char * corners_window = "Corners detected";
const char * nPath;

extern "C" {
JNIEXPORT Mat JNICALL
/** @function main */
/*
int main(int argc, char **argv) {
    /// Load source image and convert it to gray
    src = imread(argv[1], 1);
    cvtColor(src, src_gray, CV_BGR2GRAY);

    /// Create a window and a trackbar
    namedWindow(source_window, CV_WINDOW_AUTOSIZE);
    createTrackbar("Threshold: ", source_window, &thresh, max_thresh, cornerHarris_demo);
    imshow(source_window, src);

    cornerHarris_demo(0, 0);

    waitKey(0);
    return (0);
}
*/
Java_com_team_formal_eyeshopping_MainActivity_CornerHarrisDemo(JNIEnv *env, jobject instance,
                                                                long addrInputImage) {


    //nPath = env->GetStringUTFChars(filename, NULL);
    //path_size=env->GetArrayLength(path);

    //LOGD("path: %s \n", nPath);


    /// Load source image and convert it to gray
    //src = imread(nPath, 1);
    Mat * pInputImage = (Mat*)addrInputImage;
    cvtColor((*pInputImage), src_gray, CV_BGR2GRAY);

    LOGD("src_Gray_data: %s", src_gray.data);


//    /// Create a window and a trackbar
//    namedWindow(source_window, CV_WINDOW_AUTOSIZE);
//    createTrackbar("Threshold: ", source_window, &thresh, max_thresh, cornerHarris_demo);
//    imshow(source_window, src);


    dst = Mat::zeros((*pInputImage).size(), CV_32FC1);

    /// Detector parameters
    int blockSize = 2;
    int apertureSize = 3;
    double k = 0.04;

    /// Detecting corners
    // TODO: dst data 눈으로 확인하는 법!
    cornerHarris(src_gray, dst, blockSize, apertureSize, k, BORDER_DEFAULT);
    /// Normalizing
    normalize(dst, dst_norm, 0, 255, NORM_MINMAX, CV_32FC1, Mat());
    convertScaleAbs(dst_norm, dst_norm_scaled);

    /// Drawing a circle around corners
    for (int j = 0; j < dst_norm.rows; j++) {
        for (int i = 0; i < dst_norm.cols; i++) {
            if ((int) dst_norm.at<float>(j, i) > thresh) {
                circle(dst_norm_scaled, Point(i, j), 5, Scalar(0), 2, 8, 0);
            }
        }
    }

//    /// Showing the result
//    namedWindow(corners_window, CV_WINDOW_AUTOSIZE);
//    imshow(corners_window, dst_norm_scaled);


    waitKey(0);

    return dst_norm_scaled;

}
}