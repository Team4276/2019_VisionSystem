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
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include "CTargetInfo.h"
#include "CVideoFrame.h"
#include "CBlobDetector.h"
#include "dbgMsg.h"
#include "CSetting.h"
#include "CSettingList.h"

CVideoFrame::CVideoFrame()
{
    init();
}

CVideoFrame::~CVideoFrame()
{
}

void CVideoFrame::init()
{
    m_targetInfo.init();
    memset(m_timeAddedToQueue, 0, sizeof (struct timespec)*CVideoFrame::NUMBER_OF_FRAME_QUEUES);
    memset(m_timeRemovedFromQueue, 0, sizeof (struct timespec)*CVideoFrame::NUMBER_OF_FRAME_QUEUES);
    m_outbuf.clear();
    m_params.clear();
    m_pCameraVideoFrame2 = NULL;
}

void CVideoFrame::annotate()
{
    const cv::Scalar colorGreen = cv::Scalar(0, 255, 0);
    const cv::Scalar colorOrange = cv::Scalar(0, 128, 255);

    if (m_targetInfo.isClosestObjectFound() != 0)
    {
        const cv::Scalar colorYellow = cv::Scalar(0, 255, 255);

    // Find midpoints of the 4 sides of the rectangle, and draw from those points to the center
        cv::Point2f pts[4];
        cv::Point2f pt5((pts[0].x + pts[1].x)/2,(pts[0].y + pts[1].y)/2 );
        cv::Point2f pt6((pts[1].x + pts[2].x)/2,(pts[1].y + pts[2].y)/2 );
        cv::Point2f pt7((pts[2].x + pts[3].x)/2,(pts[2].y + pts[3].y)/2 );
        cv::Point2f pt8((pts[3].x + pts[0].x)/2,(pts[3].y + pts[0].y)/2 );
    }
}

std::string CVideoFrame::queueTypeToText(FRAME_QUEUE_TYPE val)
{
    switch(val)
    {
        case FRAME_QUEUE_TYPE_UNKNOWN:
            return "FRAME_QUEUE_TYPE_UNKNOWN";
            
        case FRAME_QUEUE_CAM1:
            return "FRAME_QUEUE_CAM1";
            
        case FRAME_QUEUE_CAM2:
            return "FRAME_QUEUE_CAM2";
            
        case FRAME_QUEUE_WAIT_FOR_BLOB_DETECT:
            return "FRAME_QUEUE_WAIT_FOR_BLOB_DETECT";
            
        case FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT:
            return "FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT";
            
        case FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT:
            return "FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT";
           
        case FRAME_QUEUE_FREE:
            return "FRAME_QUEUE_FREE";
            
        default: 
            break;
    }
    return "***";
}

