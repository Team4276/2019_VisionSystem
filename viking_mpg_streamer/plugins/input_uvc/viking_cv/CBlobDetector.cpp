/*******************************************************************************************/
/* The MIT License (MIT)                                                                   */
/*                                                                                         */
/* Copyright (c) 2014 - Marina High School FIRST Robotics Team 4276 (Huntington Beach, CA) */
/*                                                                                         */
/* Permission is hereby granted, free of charge, to any person obtaining a copy            */
/* of this software and associated documentation files (the "Software"), to deal           */
/* in the Software without restriction, including without limitation the rights            */
/* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell               */
/* copies of the Software, and to permit persons to whom the Software is                   */
/* furnished to do so, subject to the following conditions:                                */
/*                                                                                         */
/* The above copyright notice and this permission notice shall be included in              */
/* all copies or substantial portions of the Software.                                     */
/*                                                                                         */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR              */
/* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,                */

/* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE             */
/* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER                  */
/* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,           */
/* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN               */
/* THE SOFTWARE.                                                                           */
/*******************************************************************************************/

/*******************************************************************************************/
/* We are a high school robotics team and always in need of financial support.             */
/* If you use this software for commercial purposes please return the favor and donate     */
/* (tax free) to "Marina High School Educational Foundation"  (Huntington Beach, CA)       */
/*******************************************************************************************/

#include <stdio.h>
#include <string>
#include <signal.h>
#include <unistd.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/features2d/features2d.hpp>
#include "opencv2/calib3d/calib3d.hpp"

#include "CTargetInfo.h"
#include "CVideoFrame.h"
#include "CVideoFrameQueue.h"
#include "CConnectionServer.h"
#include "CGpioLed.h"
#include "CBlobDetector.h"
#include "CTestMonitor.h"
#include "CFrameGrinder.h"
#include "dbgMsg.h"
#include "viking_cv_version.h"
#include "CSetting.h"
#include "CSettingList.h"

extern CSettingList g_settings;

// Stereo BM stands for block matching algorithm. 
static cv::Ptr<cv::StereoBM> static_sbm;

// Stereo SGBM stands for semi block matching algorithm.
static cv::Ptr<cv::StereoSGBM> static_sgbm;

static cv::Ptr<cv::Feature2D> static_f2d;
static cv::Ptr<cv::Feature2D> static_fd;

CBlobDetector::CBlobDetector()
{
    cv::Ptr<cv::Feature2D> static_f2d = new cv::Feature2D();
    cv::Ptr<cv::Feature2D> static_fd = new cv::Feature2D();

    int ndisparities = 96; /**< Range of disparity */
    int SADWindowSize = 7;
    static_sbm = cv::StereoBM::create(ndisparities, SADWindowSize);

    static_sgbm = cv::StereoSGBM::create(-3, //int minDisparity
                                         96, //int numDisparities
                                         7, //int SADWindowSize
                                         60, //int P1 = 0
                                         2400, //int P2 = 0
                                         90, //int disp12MaxDiff = 0
                                         16, //int preFilterCap = 0
                                         1, //int uniquenessRatio = 0
                                         60, //int speckleWindowSize = 0
                                         20, //int speckleRange = 0
                                         true); //bool fullDP = false
}

CBlobDetector::CBlobDetector(const CBlobDetector& orig)
{
}

CBlobDetector::~CBlobDetector()
{
}

void CBlobDetector::init()
{
    m_tolerancePercentForRadius = 0.20;
}

void CBlobDetector::detectBlobs(CVideoFrame * pFrame, CFrameGrinder* pFrameGrinder)
{
    try
    {
        static cv::Scalar lowerBounds = cv::Scalar(79, 0, 150);
        static cv::Scalar upperBounds = cv::Scalar(96, 255, 250);

        cv::Mat frame0gray, frame1gray, dispbm, dispsgbm, dispnorm_bm, dispnorm_sgbm, falseColorsMap;
        cv::Mat sfalseColorsMap, descriptors_1, descriptors_2, img_matches, disparity, falsemap, disparity1, H1, H2;
        std::vector<cv::KeyPoint> keypoints_1, keypoints_2;
        std::vector< cv::DMatch > matches, good_matches;
        std::vector<cv::Point2f>imgpts1, imgpts2;
        std::vector<uchar> status;

        double minVal;
        double maxVal;
        double max_dist = 0;
        double min_dist = 100;

        static int iCount = 0;

        cv::BFMatcher matcher(cv::NORM_L2, true);

        if (pFrame->m_pCameraVideoFrame2 == NULL)
        {
            // Ignore mono frames
            pFrameGrinder->m_testMonitor.monitorQueueTimesBeforeReturnToFreeQueue(pFrame, pFrameGrinder);
            pFrameGrinder->safeAddTail(pFrame, CVideoFrame::FRAME_QUEUE_FREE);
            return;
        }
        dbgMsg_s("Start:  detectBlobs\n");

        // Convert to grayscale
        cv::cvtColor(pFrame->m_frame, frame0gray, CV_BGR2GRAY);
        cv::cvtColor(pFrame->m_pCameraVideoFrame2->m_frame, frame1gray, CV_BGR2GRAY);
        dbgMsg_s("detectBlobs 01a\n");

        static_sbm->compute(frame0gray, frame1gray, dispbm);
        cv::minMaxLoc(dispbm, &minVal, &maxVal);
        dispbm.convertTo(dispnorm_bm, CV_8UC1, 255 / (maxVal - minVal));
        dbgMsg_s("detectBlobs 01b\n");

        static_sgbm->compute(frame0gray, frame1gray, dispsgbm);
        dbgMsg_s("detectBlobs 01b1\n");
        cv::minMaxLoc(dispsgbm, &minVal, &maxVal);
        dbgMsg_s("detectBlobs 01b2\n");
        dispsgbm.convertTo(dispnorm_sgbm, CV_8UC1, 255 / (maxVal - minVal));
        dbgMsg_s("detectBlobs 01b3\n");
        cv::applyColorMap(dispnorm_bm, falseColorsMap, cv::COLORMAP_JET);
        dbgMsg_s("detectBlobs 01b4\n");
        cv::applyColorMap(dispnorm_sgbm, sfalseColorsMap, cv::COLORMAP_JET);
        dbgMsg_s("detectBlobs 01c\n");

        static_f2d->detect(frame0gray, keypoints_1);
        static_f2d->detect(frame1gray, keypoints_2);
        dbgMsg_s("detectBlobs 01d\n");

        //-- Step 2: Calculate descriptors (feature vectors)
        static_fd->compute(frame0gray, keypoints_1, descriptors_1);
        static_fd->compute(frame1gray, keypoints_2, descriptors_2);
        dbgMsg_s("detectBlobs 01e\n");

        //-- Step 3: Matching descriptor vectors with a brute force matcher

        matcher.match(descriptors_1, descriptors_2, matches);
        cv::drawMatches(frame0gray, keypoints_1, frame1gray, keypoints_2, matches, img_matches);
        dbgMsg_s("detectBlobs 01f\n");

        //-- Quick calculation of max and min distances between keypoints
        for (int i = 0; i < matches.size(); i++)
        {
            double dist = matches[i].distance;
            if (dist < min_dist) min_dist = dist;
            if (dist > max_dist) max_dist = dist;
        }
        dbgMsg_s("detectBlobs 01\n");

        for (int i = 0; i < matches.size(); i++)
        {
            if (matches[i].distance <= cv::max(4.5 * min_dist, 0.02))
            {
                good_matches.push_back(matches[i]);
                imgpts1.push_back(keypoints_1[matches[i].queryIdx].pt);
                imgpts2.push_back(keypoints_2[matches[i].trainIdx].pt);
            }

        }
        dbgMsg_s("detectBlobs 03\n");

        cv::Mat F = cv::findFundamentalMat(imgpts1, imgpts2, cv::FM_RANSAC, 3., 0.99, status); //FM_RANSAC
        cv::stereoRectifyUncalibrated(imgpts1, imgpts1, F, frame0gray.size(), H1, H2);
        cv::Mat rectified1(frame0gray.size(), frame0gray.type());
        cv::warpPerspective(frame0gray, rectified1, H1, frame0gray.size());

        cv::Mat rectified2(frame1gray.size(), frame1gray.type());
        cv::warpPerspective(frame1gray, rectified2, H2, frame1gray.size());

        static_sgbm->compute(rectified1, rectified2, disparity);
        cv::minMaxLoc(disparity, &minVal, &maxVal);
        disparity.convertTo(disparity1, CV_8UC1, 255 / (maxVal - minVal));
        cv::applyColorMap(disparity1, falsemap, cv::COLORMAP_JET);
        dbgMsg_s("detectBlobs 04\n");

        pFrame->m_filteredFrame = falsemap; // disparity_rectified_color
        //pFrame->m_filteredFrame = falseColorsMap;     // BN
        //pFrame->m_filteredFrame = sfalseColorsMap;    // CSGBM

        iCount++;
        if ((iCount % 17) == 0)
        {
            pFrameGrinder->m_testMonitor.saveFrameToJpeg(pFrame->m_filteredFrame);
        }

        // TODO:  Find closest object, determine its width, and the avoidance path to the space on either side 
        //        If possible find the general direction in farther distance that offers the longest unobstructed path
        bool isClosestObjectFound = false;
        double distanceToClosestObjectInches = 0.0;
        int xPixelCenterOfClosestObject;
        int xPixelAvoidClosestObjectRightPath = 0;
        int xPixelAvoidClosestObjectLeftPath = 0;
        dbgMsg_s("detectBlobs 05\n");

        pFrame->m_targetInfo.updateTargetInfo(
                                              isClosestObjectFound, distanceToClosestObjectInches, xPixelCenterOfClosestObject);

        dbgMsg_s("detectBlobs 07\n");
    }
    catch (...)
    {
    }
    dbgMsg_s("End:  detectBlobs\n");
}

void CBlobDetector::calibrateRightCam(CVideoFrame* pFrame, CFrameGrinder* pFrameGrinder)
{

}

void CBlobDetector::calibrateLeftCam(CVideoFrame* pFrame, CFrameGrinder* pFrameGrinder)
{

}

double CBlobDetector::normalize360(double angle)
{
    while (angle >= 360.0)
    {
        angle -= 360.0;
    }
    while (angle < 0.0)
    {
        angle += 360.0;
    }
    return angle;
}

