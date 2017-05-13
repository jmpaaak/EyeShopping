//
// Created by jongmin on 2017-05-14.
//
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;

extern "C" {
JNIEXPORT void JNICALL
Java_com_team_formal_eyeshopping_MainActivity_ConvertRGBtoGray(JNIEnv *env,
                                                                    jobject instance,
                                                                    jlong matAddrInput,
                                                                    jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cvtColor(matInput, matResult, CV_RGBA2GRAY);

}
}
