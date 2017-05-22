#include <opencv2/features2d.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/opencv.hpp>
#include <vector>
#include <iostream>
#include <jni.h>
#include <android/log.h>

const float nn_match_ratio = 0.95f;   // Nearest neighbor matching ratio

#define  LOG_TAG    "NDK_TEST"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

extern "C" {
JNIEXPORT jint JNICALL
Java_com_team_formal_eyeshopping_CVMainActivity_AkazeFeatureMatching(JNIEnv *env, jobject instance,
                                                               jlong addrSelectedImage,
                                                               jlong addrSearchedImage) {
    Mat selMat = *(Mat*) addrSelectedImage;
    Mat schMat = *(Mat*) addrSearchedImage;

    vector<KeyPoint> kptVecSel, kptVecSch;
    Mat descMatSel, descMatSch;

    /**** Find Features & Compute Descriptors ****/
    Ptr<AKAZE> akaze = AKAZE::create();
    akaze->detectAndCompute(selMat, noArray(), kptVecSel, descMatSel);
    akaze->detectAndCompute(schMat, noArray(), kptVecSch, descMatSch);

    BFMatcher matcher(NORM_HAMMING); // TODO FLANN matcher 사용가능?

    /**** Feature Matching ****/
    vector<vector<DMatch>> nn_matches; // 2차원 DMatch 변수 선언
    matcher.knnMatch(descMatSel, descMatSch, nn_matches, 2); // DMatch 배열에 feature 별로 k개씩 매칭시켜 저장

    vector<KeyPoint> matchedSel, matchedSch;
    vector<DMatch> matches;
    for(size_t i = 0; i < nn_matches.size(); i++) { // TODO RANSAC 사용가능?
        DMatch first = nn_matches[i][0];
        float dist1 = nn_matches[i][0].distance;
        float dist2 = nn_matches[i][1].distance;
        if(dist1 < nn_match_ratio * dist2) { // ratio가 dist1/dist2 보다 작을 때: 2-NN match
            int new_i = (int) matchedSel.size();
            matchedSel.push_back(kptVecSel[first.queryIdx]);
            matchedSch.push_back(kptVecSch[first.trainIdx]);
            matches.push_back(DMatch(new_i, new_i, 0));
        }
    }

    /**** Estimating Homography matrix ****/
    vector<Point2f> selPts, prdPts;
    for( int i = 0; i < matches.size(); i++ ) {
        selPts.push_back( matchedSel[matches[i].queryIdx].pt );
        prdPts.push_back( matchedSch[matches[i].trainIdx].pt );
    }

    Mat hMat = findHomography( selPts, prdPts, CV_RANSAC, 1 );

    Mat res;
    if(!hMat.empty()) { // Detecting !!!!
        // drawMatches(selMat, matchedSel, schMat, matchedSch, matches, res);

        LOGI("A-KAZE Matching Results\n");
        LOGI("*******************************\n");
        LOGI("# Keypoints UserSel:              \t%d\n", (int)kptVecSel.size());
        LOGI("# Keypoints NaverPR:              \t%d\n", (int)kptVecSch.size());
        LOGI("# Matches:                        \t%d\n\n", (int) matches.size());
        return 1;
    } else {
        LOGI("NOT Detected !\n");
        LOGI("NOT Detected !\n");
        LOGI("NOT Detected !\n");

        LOGI("A-KAZE Matching Results\n");
        LOGI("*******************************\n");
        LOGI("# Keypoints UserSel:              \t%d\n", (int)kptVecSel.size());
        LOGI("# Keypoints NaverPR:              \t%d\n", (int)kptVecSch.size());
        LOGI("# Matches:                        \t%d\n\n", (int) matches.size());
        return 0;
    }

    // outputMat = res;
}
}