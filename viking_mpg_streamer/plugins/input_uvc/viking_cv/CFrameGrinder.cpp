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

#include "CTargetInfo.h"
#include "CVideoFrame.h"
#include "CVideoFrameQueue.h"
#include "CConnectionServer.h"
#include "CGpioLed.h"
#include "CBlobDetector.h"
#include "CTestMonitor.h"
#include "CFrameGrinder.h"
#include "CSetting.h"
#include "CSettingList.h"
#include "dbgMsg.h"


// Global shutdown flag is set when user typed Ctrl-C
extern bool g_isShutdown;
extern CSettingList g_settings;

CFrameGrinder::CFrameGrinder()
{
    m_cam_queue_thread = -1;
    m_blob_detect_thread = -1;
    int myErr = pthread_mutexattr_settype(&m_mutexattrQueue, PTHREAD_MUTEX_ERRORCHECK);
    if (myErr != 0)
    {
        dbgMsg_d1("CFrameGrinder pthread_mutexattr_settype error: %d\n", myErr);
    }
    pthread_mutex_init(&m_mutexQueue, &m_mutexattrQueue);
}

CFrameGrinder::~CFrameGrinder()
{
    pthread_mutex_destroy(&m_mutexQueue);
}

void* cam_queue_thread(void* pVoid)
{
    CFrameGrinder* pFrameGrinder = (CFrameGrinder*) pVoid;

    CVideoFrame* pFrame = 0;
    CVideoFrame* pFrame2 = 0;
    while (!g_isShutdown)
    {
        if (pFrameGrinder->safeBlockingRemoveHead(&pFrame, CVideoFrame::FRAME_QUEUE_CAM1))
        {
            if (pFrameGrinder->safeRemoveHead(&pFrame2, CVideoFrame::FRAME_QUEUE_CAM2))
            {
                pFrame->m_pCameraVideoFrame2 = pFrame2;
                pFrame->m_pCameraVideoFrame2->m_pCameraVideoFrame2 = NULL;
                pFrameGrinder->m_testMonitor.m_nCountStereoFramesInThisInterval++;
                CTestMonitor::getTicks(&pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_CAM2]);
            }
            else
            {
                pFrame->m_pCameraVideoFrame2 = NULL;
                pFrameGrinder->m_testMonitor.m_nCountMonoFramesInThisInterval++;
            }
            pFrameGrinder->safeAddTailAndPurgeOlder(pFrame, CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT);
        }
    }
}

void* blob_detect_thread(void* pVoid)
{
    CFrameGrinder* pFrameGrinder = (CFrameGrinder*) pVoid;
    CVideoFrame* pFrame = 0;
    while (!g_isShutdown)
    {
        if (pFrameGrinder->safeBlockingRemoveHead(&pFrame, CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT))
        {
            try
            {
                int iSetting = g_settings.getSetting(CSetting::SETTING_OPERATING_MODE);
                switch (iSetting)
                {
                case CSetting::MODE_STEREO_CAMS:
                    if (!pFrame->m_frame.empty())
                    {
                        pFrameGrinder->m_blobDetector.detectBlobs(pFrame, pFrameGrinder);
                    }
                    break;
                case CSetting::MODE_RIGHT_CAM:
                    if (!pFrame->m_frame.empty())
                    {
                        pFrameGrinder->m_blobDetector.calibrateRightCam(pFrame, pFrameGrinder);
                    }
                    break;
                case CSetting::MODE_LEFT_CAM:
                    if (!pFrame->m_frame.empty())
                        if ((pFrame->m_pCameraVideoFrame2 != NULL) && (!pFrame->m_pCameraVideoFrame2->m_frame.empty()))
                        {
                            pFrameGrinder->m_blobDetector.calibrateLeftCam(pFrame, pFrameGrinder);
                        }
                    break;
                default:
                    dbgMsg_s("Unknown operating mode\n");
                    break;
                }
            }
            catch (...)
            {
                dbgMsg_s("Exception in blob_detect_thread\n");
            }

            struct timespec timeNow = {0};
            CTestMonitor::getTicks(&timeNow);
            int timeLatencyThisCameraFrameMilliseconds = (int) CTestMonitor::getDeltaTimeMilliseconds(
                                                                                                      pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT],
                                                                                                      timeNow);
            pFrame->m_targetInfo.updateLatency(timeLatencyThisCameraFrameMilliseconds);

            pFrameGrinder->safeAddTailAndPurgeOlder(pFrame, CVideoFrame::FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT);
            pFrameGrinder->m_testMonitor.m_nTasksDone[CTestMonitor::TASK_DONE_BLOB_DETECT]++;
        }
    }
}

void CFrameGrinder::init()
{
    dbgMsg_s("Start CFrameGrinder::init() \n");
    unsigned int i = 0;

    m_blobDetector.init();

    m_testMonitor.init();
    for (i = 0; i < CVideoFrame::NUMBER_OF_FRAME_QUEUES; i++)
    {
        m_frameQueueList[i].init((CVideoFrame::FRAME_QUEUE_TYPE)i);
    }
    for (i = 0; i < NUMBER_OF_FRAMES_FOR_QUEUE; i++)
    {
        m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].addTail(new CVideoFrame(), m_mutexQueue);
    }
    int iRet = pthread_create(&m_cam_queue_thread, NULL, cam_queue_thread, this);
    if (iRet != 0)
    {
        dbgMsg_s("We have an error (cam queue thread)\n");
    }

    iRet = pthread_create(&m_blob_detect_thread, NULL, blob_detect_thread, this);
    if (iRet != 0)
    {
        dbgMsg_s("We have an error\n");
    }

    int myErr = pthread_mutexattr_settype(&m_mutexattrQueue, PTHREAD_MUTEX_ERRORCHECK);
    if (myErr != 0)
    {
        dbgMsg_d1("CFrameGrinder.init pthread_mutexattr_settype error: %d\n", myErr);
    }
    pthread_mutex_init(&m_mutexQueue, &m_mutexattrQueue);

    m_connectionServer.init(this);

    usleep(2 * 1000 * 1000); // Allow 2 seconds for threads to start up and get to where they are waiting on queues
    dbgMsg_s("End CFrameGrinder::init() \n");
}

void CFrameGrinder::initVideo(int framesPerSec, unsigned int height, unsigned int width, int codec)
{
    m_testMonitor.initVideo(framesPerSec, height, width, codec);
}

bool CFrameGrinder::safeGetFreeFrame(CVideoFrame** ppFrame)
{
    int myErr = 0;
    if (!m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].removeHead(ppFrame, m_mutexQueue))
    {
        // Overrun condition - Get head from longest queue and count it as a dropped frame
        dbgMsg_s("safeGetFreeFrame:  OVERRUN CONDITION %%%%%%%%%%%, (FREE queue remove failed)\n");
        dbgMsg_s(CTestMonitor::displayQueueLengths(this));

        myErr = pthread_mutex_lock(&m_mutexQueue);
        if (myErr != 0)
        {
            dbgMsg_d1("safeGetFreeFrame pthread_mutex_lock error: %d\n", myErr);
        }
        unsigned int i = 0;
        unsigned int maxQueueLength = 0;
        CVideoFrame::FRAME_QUEUE_TYPE eLongestFrameQueue = CVideoFrame::FRAME_QUEUE_TYPE_UNKNOWN;
        for (i = 0; i < CVideoFrame::NUMBER_OF_FRAME_QUEUES; i++)
        {
            if (m_frameQueueList[i].size() > maxQueueLength)
            {
                maxQueueLength = m_frameQueueList[i].size();
                eLongestFrameQueue = (CVideoFrame::FRAME_QUEUE_TYPE) i;
            }
        }
        if (eLongestFrameQueue != CVideoFrame::FRAME_QUEUE_TYPE_UNKNOWN)
        {
            if (m_frameQueueList[(int) eLongestFrameQueue].nolockRemoveHead(ppFrame))
            {
                if (eLongestFrameQueue != CVideoFrame::FRAME_QUEUE_FREE)
                {
                    m_frameQueueList[(int) eLongestFrameQueue].m_droppedFrames++;
                }
            }
        }
        if (*ppFrame == 0)
        {
            dbgMsg_s("Longest queue got emptied - try for any free packet\n");
            for (i = 0; i < CVideoFrame::NUMBER_OF_FRAME_QUEUES; i++)
            {
                if (m_frameQueueList[i].nolockRemoveHead(ppFrame))
                {
                    if (eLongestFrameQueue != CVideoFrame::FRAME_QUEUE_FREE)
                    {
                        m_frameQueueList[(int) eLongestFrameQueue].m_droppedFrames++;
                    }
                    break;
                }
            }
        }

        myErr = pthread_mutex_unlock(&m_mutexQueue);
        if (myErr != 0)
        {
            dbgMsg_d1("safeGetFreeFrame1 pthread_mutex_unlock error: %d\n", myErr);
        }
    }
    if (*ppFrame == 0)
    {
        dbgMsg_s("Something very wrong here - no free frame buffers\n");
        return false;
    }
    (*ppFrame)->init();
    CTestMonitor::getTicks(&((*ppFrame)->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_FREE])); // init() clears the time stamp so have to get time again)
    return true;
}

void CFrameGrinder::safeAddTail(CVideoFrame* pFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType)
{
    m_frameQueueList[eFrameQueueType].addTail(pFrame, m_mutexQueue);
}

void CFrameGrinder::safeAddTailAndPurgeOlder(CVideoFrame* pFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType)
{
    m_frameQueueList[eFrameQueueType].addTail(pFrame, m_mutexQueue);
    while (m_frameQueueList[eFrameQueueType].size() > 1)
    {
        std::vector<CVideoFrame*> droppedFrames = m_frameQueueList[eFrameQueueType].dropOlderAndKeepHead(&pFrame, m_mutexQueue);
        while (droppedFrames.size() > 0)
        {
            CVideoFrame* pTempFrame = droppedFrames.back();
            droppedFrames.pop_back();

            m_testMonitor.monitorQueueTimesBeforeReturnToFreeQueue(pFrame, this);
            if (pTempFrame->m_pCameraVideoFrame2 != NULL)
            {
                m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].addTail(pTempFrame->m_pCameraVideoFrame2, m_mutexQueue);
                pTempFrame->m_pCameraVideoFrame2 = NULL;
            }
            m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].addTail(pTempFrame, m_mutexQueue);
        }
    }
}

bool CFrameGrinder::safeRemoveHead(CVideoFrame** ppFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType)
{
    if (eFrameQueueType != CVideoFrame::FRAME_QUEUE_FREE)
    {
        std::vector<CVideoFrame*> droppedFrames = m_frameQueueList[eFrameQueueType].dropOlderAndRemoveHead(ppFrame, m_mutexQueue);
        while (droppedFrames.size() > 0)
        {
            CVideoFrame* pTempFrame = droppedFrames.back();
            droppedFrames.pop_back();
            m_testMonitor.monitorQueueTimesBeforeReturnToFreeQueue(*ppFrame, this);
            if (pTempFrame->m_pCameraVideoFrame2 != NULL)
            {
                m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].addTail(pTempFrame->m_pCameraVideoFrame2, m_mutexQueue);
                pTempFrame->m_pCameraVideoFrame2 = NULL;
            }
            m_frameQueueList[CVideoFrame::FRAME_QUEUE_FREE].addTail(pTempFrame, m_mutexQueue);
        }
        return (*ppFrame != NULL);
    }
    return m_frameQueueList[eFrameQueueType].removeHead(ppFrame, m_mutexQueue);
}

bool CFrameGrinder::safeBlockingRemoveHead(CVideoFrame** ppFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType)
{
    return m_frameQueueList[eFrameQueueType].blockingRemoveHead(ppFrame, m_mutexQueue);
}
