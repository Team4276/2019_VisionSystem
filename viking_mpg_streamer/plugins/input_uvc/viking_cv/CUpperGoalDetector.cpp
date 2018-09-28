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

#include "CUpperGoalRectangle.h"
#include "CTargetInfo.h"
#include "CVideoFrame.h"
#include "CVideoFrameQueue.h"
#include "CConnectionServer.h"
#include "CGpioLed.h"
#include "CUpperGoalDetector.h"
#include "CTestMonitor.h"
#include "CFrameGrinder.h"
#include "dbgMsg.h"
#include "viking_cv_version.h"
#include "CSetting.h"
#include "CSettingList.h"

extern CSettingList g_settings;

CUpperGoalDetector::CUpperGoalDetector()
{
}

CUpperGoalDetector::CUpperGoalDetector(const CUpperGoalDetector& orig)
{
}

CUpperGoalDetector::~CUpperGoalDetector()
{
}

void CUpperGoalDetector::init()
{
    m_tolerancePercentForRadius = 0.20;
}

double CUpperGoalDetector::estimateDistanceInches(const CUpperGoalRectangle& goalRect)
{
    double listArea[] =     {1308.0, 1134.0, 894.0,  800.0,  695.0,  457.0,   361.0,   260.0,   0.0};
    double listDistance[] = {5.0*12, 6.0*12, 7.0*12, 8.0*12, 9.0*12, 13.0*12, 16.0*12, 21.0*12, 0.0}; 
    int nSamples = 8;
    
    double area = goalRect.size.height * goalRect.size.width;
    if( (area > listArea[0]) || (area < listArea[nSamples-1]) )
    {
        return -1;
    }
    int iLow=0;
    int iHigh=0;
    for(int i=0; i<nSamples-1; i++)
    {
        iLow = i;
        iHigh = i+1;
        if( (area < listArea[i]) && (area > listArea[i+1]) )
        {
            break;
        }
    }
    double areaLow = listArea[iLow];
    double areaHigh = listArea[iHigh];
    double areaDiff = listArea[iHigh] - listArea[iLow];
    
    double distLow = listDistance[iLow];
    double distHigh = listDistance[iHigh];
    double distDiff = listDistance[iHigh] - listDistance[iLow];
    
    double partial = ((area - areaLow) / areaDiff);
    
    double retDist = distLow + (partial * distDiff);
    return retDist;
}

double CUpperGoalDetector::estimateAzimuthAngle(double distance, double pixelX)
{
    // This calculation would be a lot easier if the camera did not have such a wide field of view
    // Up close to the goal, turning the root plus/minus 20 degrees makes the goal appear to follow a U
    // shaped path that leaves the top of the screen 25 pixels offset from center at azimuth 25 degrees.
    
    double listCenterXAtDistanceInches[][2] = {
        {195, 4*12}, {202.0, 5*12}, {210.0, 6*12}, {212.0, 7*12}, {212.0, 8*12}, {212.0, 9*12}, {212.0, 13*12}, {212.0, 16*12}, {212.0, 21*12}, {0.0, 0.0}
    };
   
    double list20DegreeXAtDistanceInches[][2] = {
        {220.0, 4*12}, {190.0, 5*12}, {180.0, 6*12}, {164.0, 7*12}, {133.0, 8*12}, {108.0, 9*12}, {164.0, 13*12}, {220.0, 16*12}, {212.0, 21*12}, {0.0, 0.0}
    };
    int nSamples = 8;
    
    int iLow=0;
    int iHigh=0;
    for(int i=0; i<nSamples-1; i++)
    {
        iLow = i;
        iHigh = i+1;
        if( (distance < listCenterXAtDistanceInches[i][2]) && (distance > listCenterXAtDistanceInches[i+1][2]) )
        {
            break;
        }
    }
    double distLow = listCenterXAtDistanceInches[iLow][1];
    double distHigh = listCenterXAtDistanceInches[iHigh][1];
    double distDiff = listCenterXAtDistanceInches[iHigh][1] - listCenterXAtDistanceInches[iLow][1];
    
    double centerLow = listCenterXAtDistanceInches[iLow][2];
    double centerHigh = listCenterXAtDistanceInches[iHigh][2];
    double centerDiff = listCenterXAtDistanceInches[iHigh][2] - listCenterXAtDistanceInches[iLow][2];
   
    double deg20Low = list20DegreeXAtDistanceInches[iLow][2];
    double deg20High = list20DegreeXAtDistanceInches[iHigh][2];
    double deg20Diff = list20DegreeXAtDistanceInches[iLow][2] - list20DegreeXAtDistanceInches[iHigh][2];
   
    double partial = ((distance - distLow) / distDiff);
    
    double centerX = centerLow + (partial * centerDiff);
    double pixelsPerDegree = deg20Low + (partial * deg20Diff);
    pixelsPerDegree /= 20.0;
    return (pixelX - centerX) * (1.0/pixelsPerDegree);  
}

void CUpperGoalDetector::detectBlobs(CVideoFrame * pFrame, CFrameGrinder* pFrameGrinder)
{
    try
    {
        static struct timespec timeLastCameraFrame = {0};
        static struct timespec timeNow = {0};
        static cv::Scalar lowerBounds = cv::Scalar(79,0,150);
	static cv::Scalar upperBounds = cv::Scalar(96,255,250);
        
        cv::Mat img_hsv, img_blur, goal_blob;
        static int iCount = 0;
        
        int timeSinceLastCameraFrameMilliseconds = (int) CTestMonitor::getDeltaTimeMilliseconds(
                timeLastCameraFrame,
                pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT]);
        timeLastCameraFrame = pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT];

        /*
        // RBG is flawed as a way to filter based on color because the brightness is combined 
        // with the color info. 
        // Not so with HSV, where Hue and Saturation are maintained separately
        // OpenCV has a handy conversion from RGB to HSV
        cv::cvtColor(pFrame->m_frame, img_hsv, CV_BGR2HSV);
        
        cv::GaussianBlur(img_hsv, img_blur, cv::Size(5,5),1.5);

       // Look for the green hue we are emitting from the LED halo 
        if(g_settings.isDynamicSettingsEnabled())
        {
             g_settings.getValueFromFile(CSetting::SETTING_FILTER_HUE_LOWER_BOUND);
             g_settings.getValueFromFile(CSetting::SETTING_FILTER_HUE_UPPER_BOUND);
        }
        if(g_settings.isValueChanged(CSetting::SETTING_FILTER_HUE_LOWER_BOUND))
        {
            lowerBounds = cv::Scalar(g_settings.getSetting(CSetting::SETTING_FILTER_HUE_LOWER_BOUND),0,150);
        }
        if(g_settings.isValueChanged(CSetting::SETTING_FILTER_HUE_UPPER_BOUND))
        {
            upperBounds = cv::Scalar(g_settings.getSetting(CSetting::SETTING_FILTER_HUE_UPPER_BOUND),255,250);
        }

        // Find the bright response from the retro-reflective tape
        cv::inRange(img_blur, lowerBounds, upperBounds, goal_blob);
        pFrame->m_filteredFrame = goal_blob.clone();
            
        iCount++;
        if ((iCount % 17) == 0)
        {
            pFrameGrinder->m_testMonitor.saveFrameToJpeg(pFrame->m_filteredFrame);
        }

        //Find the contours. Use the contourOutput Mat so the original image doesn't get overwritten
        std::vector<std::vector<cv::Point> > goalContours;
        cv::findContours(goal_blob, goalContours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
       */
        
        CUpperGoalRectangle upperGoalRectangle;
        float upperGoalAzimuthDegrees = 0.0;
        float distanceToUpperGoalInches = 0.0;
        bool isUpperGoalFound = false;
        //isUpperGoalFound = filterContours(goalContours, pFrame->m_frame.rows, pFrame->m_frame.cols,
        //        upperGoalRectangle, upperGoalAzimuthDegrees, distanceToUpperGoalInches);
        
        CTestMonitor::getTicks(&timeNow);
        int timeLatencyThisCameraFrameMilliseconds = (int) CTestMonitor::getDeltaTimeMilliseconds(
                pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT],
                timeNow);

        pFrame->m_targetInfo.updateTargetInfo(
                timeSinceLastCameraFrameMilliseconds, timeLatencyThisCameraFrameMilliseconds, 
                isUpperGoalFound, upperGoalAzimuthDegrees, distanceToUpperGoalInches, upperGoalRectangle.center.x);

        pFrame->updateAnnotationInfo(upperGoalRectangle);

        //m_gpioLed.setGreenLED(isUpperGoalFound, pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT]);
    }
    catch (...)
    {
    }
}

double CUpperGoalDetector::normalize360(double angle)
{
    while(angle >= 360.0)
    {
        angle -= 360.0;
    }
    while(angle < 0.0)
    {
        angle += 360.0;
    }
    return angle;
}

bool CUpperGoalDetector::filterContours(
        const std::vector<std::vector<cv::Point> >& listContours,
        int originalMatHeight, int originalMatWidth,
        CUpperGoalRectangle& upperGoalRectangle,
        float& upperGoalAzimuthDegrees,
        float& distanceToUpperGoalInches)
{
// Constants for known variables
// the height to the top of the target in first stronghold is 97 inches	
    static int TOP_TARGET_HEIGHT = 97;
//	the physical height of the camera lens
    static int TOP_CAMERA_HEIGHT = 16;

//	camera details, can usually be found on the data sheets of the camera
    static double VERTICAL_FOV  = 44;
    static double HORIZONTAL_FOV  = 78;
    static double CAMERA_ANGLE = 0;    
    
    static double PI = 3.14159265358979323846264;
    
    static double areaPixelsAtDistanceInches[][2] = 
    {
        {1625.0, 69.0},
        {0.0, 0.0}
    };
   
    bool isUpperGoalFound = false;
    upperGoalRectangle.init();
    upperGoalAzimuthDegrees = -999.0;
    distanceToUpperGoalInches = -1.0;
    float area = 0.0;
    float area2 = 0.0;
   
    cv::RotatedRect tempRect;
    std::vector<cv::RotatedRect> listFilteredRect;
    for(int i = 0; i < listContours.size(); i++)
    {
        if (listContours.at(i).size() > 4 )  // ==2 contour  is a straight line, == 3 triangle, == 4 '4 sided' (still can't be a "U" shape)
        {
            std::vector<cv::Vec4i>  convexityDefectsSet;
            std::vector<int> hull;
            cv::convexHull(listContours.at(i), hull, false );
            if (hull.size() > 4 )  // ==2 hull is a straight line, == 3 triangle, == 4 '4 sided' (still can't be a "U" shape)
            {
                cv::convexityDefects(listContours.at(i), hull, convexityDefectsSet);
                tempRect = cv::minAreaRect(cv::Mat(listContours.at(i)));
                float aspect = (float)tempRect.size.width/(float)tempRect.size.height;
                int shorterSide = tempRect.size.height;
                int longerSide = tempRect.size.width;
                if(aspect < 1.0)
                {
                    aspect = 1/aspect;
                    shorterSide = tempRect.size.width;
                    longerSide = tempRect.size.height;
                }
                if(   (shorterSide >= 10) 
                   && (longerSide >= 18)  
                   && (aspect < 2.5)  )
                {
                    bool bFound = false;
                    for(int j=0; j<convexityDefectsSet.size(); j++)
                    {    
                        // defect(start_index, end_index, farthest_pt_index, fixpt_depth)
                        //     start_index, end_index, farthest_pt_index are 0-based indices in the original contour of the convexity defect beginning, end and the farthest point
                        //     fixpt_depth is fixed-point approximation (with 8 fractional bits) of the distance between the farthest contour point and the hull. That is, to get the floating-point value of the depth will be fixpt_depth/256.0
                        //
                        // So... we look for a dent in the surrounding countour that is more than half the height
                        double depth = convexityDefectsSet[j][3]/256.0;
                        //printf("w,h [%f,%f]   convexityDefectsSet[%d] (depth) = [ %d %d %d %d ]  (%f) \n", tempRect.size.width, tempRect.size.height, j, convexityDefectsSet[j][0], convexityDefectsSet[j][1], convexityDefectsSet[j][2], convexityDefectsSet[j][3], depth);
                        if(depth > (shorterSide/2))
                        {
                            // Contour is concave (hoping for a "U")
                            bFound = true;
                        }
                    }
                    if(bFound) 
                    {
                        area = (float)tempRect.size.width * (float)tempRect.size.height;
                        //printf(" *** FOUND ***   area1 %f  w,h,a,d = %f\t%f\t%f\n", area, tempRect.size.width, tempRect.size.height, aspect);
                        listFilteredRect.push_back(tempRect);                    
                    }
                }
            }
        }
    }
    isUpperGoalFound = (listFilteredRect.size() > 0);
    if(isUpperGoalFound)
    {
        for(int k=0; k<listFilteredRect.size(); k++)
        {
            area = (float)upperGoalRectangle.size.width * (float)upperGoalRectangle.size.height;
            area2 = (float)listFilteredRect[k].size.width * (float)listFilteredRect[k].size.height;
            if(area < area2)
            {
                upperGoalRectangle = CUpperGoalRectangle(listFilteredRect[k]);               
            }
        }
        area = (float)upperGoalRectangle.size.width * (float)upperGoalRectangle.size.height;
        //printf("goal (x, y) area  (%f, %f) %f", upperGoalRectangle.center.x, upperGoalRectangle.center.y, area);
        
        distanceToUpperGoalInches = estimateDistanceInches(upperGoalRectangle);

        upperGoalAzimuthDegrees = estimateAzimuthAngle(distanceToUpperGoalInches, upperGoalRectangle.center.x);
    }
    return isUpperGoalFound;
}
