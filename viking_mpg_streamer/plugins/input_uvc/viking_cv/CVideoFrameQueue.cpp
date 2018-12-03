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
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>

#include "CTargetInfo.h"
#include "CVideoFrame.h"
#include "CVideoFrameQueue.h"
#include "CConnectionServer.h"
#include "CGpioLed.h"
#include "CBlobDetector.h"
#include "CTestMonitor.h"
#include "CFrameGrinder.h"
#include "dbgMsg.h"

CVideoFrameQueue::CVideoFrameQueue()
{
    m_eQueueType = CVideoFrame::FRAME_QUEUE_TYPE_UNKNOWN;
    memset(&m_condThread, 0, sizeof (pthread_cond_t));
}

CVideoFrameQueue::~CVideoFrameQueue()
{
    while (m_queue.size() != 0)
    {
        CVideoFrame* pFrame = m_queue.back();
        m_queue.pop_back();
        delete pFrame;
    }
}

void CVideoFrameQueue::init(CVideoFrame::FRAME_QUEUE_TYPE eQueueType)
{
    m_eQueueType = eQueueType;
}

void CVideoFrameQueue::addTail(CVideoFrame* pFrame, pthread_mutex_t& mutexQueue)
{
    int myErr = pthread_mutex_lock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("addTail pthread_mutex_lock error: %d\n", myErr);
    }
    nolockAddTail(pFrame);
    myErr = pthread_mutex_unlock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("addTail pthread_mutex_unlock error: %d\n", myErr);
    }
    //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":    about to issue pthread_cond_broadcast: \n");
    int rc = pthread_cond_broadcast(&m_condThread);
    if (rc != 0)
    {
        std::string sMsg = "addTail() ret from pthread_cond_broadcast: " + numberToText(rc) + "\n";
        dbgMsg_s(sMsg);
    }
}

bool CVideoFrameQueue::removeHead(CVideoFrame** ppFrame, pthread_mutex_t& mutexQueue)
{
    int myErr = pthread_mutex_lock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("removeHead pthread_mutex_lock error: %d\n", myErr);
    }
    bool bRet = nolockRemoveHead(ppFrame); // *ppFrame == NULL if no frame removed
    myErr = pthread_mutex_unlock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("removeHead pthread_mutex_unlock error: %d\n", myErr);
    }
    if (bRet)
    {
        //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   after successful removeHead size = " + numberToText(m_queue.size()) + "\n");
    }
    else
    {
        //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   after failed removeHead size = " + numberToText(m_queue.size()) + "\n");
    }
    return bRet;
}

std::vector<CVideoFrame*> CVideoFrameQueue::dropOlderAndRemoveHead(CVideoFrame** ppFrame, pthread_mutex_t& mutexQueue)
{
    int myErr = pthread_mutex_lock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("dropOlderAndRemoveHead pthread_mutex_lock error: %d\n", myErr);
    }
    std::vector<CVideoFrame*> droppedFrames = nolockDropOlderFrames();
    nolockRemoveHead(ppFrame); // *ppFrame == NULL if no frame removed
    myErr = pthread_mutex_unlock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("dropOlderAndRemoveHead pthread_mutex_unlock error: %d\n", myErr);
    }
    return droppedFrames;
}

std::vector<CVideoFrame*> CVideoFrameQueue::dropOlderAndKeepHead(CVideoFrame** ppFrame, pthread_mutex_t& mutexQueue)
{
    int myErr = pthread_mutex_lock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("dropOlderAndKeepHead pthread_mutex_lock error: %d\n", myErr);
    }
    std::vector<CVideoFrame*> droppedFrames = nolockDropOlderFrames();
    myErr = pthread_mutex_unlock(&mutexQueue);
    if (myErr != 0)
    {
        dbgMsg_d1("dropOlderAndKeepHead pthread_mutex_unlock error: %d\n", myErr);
    }
    return droppedFrames;
}

std::vector<CVideoFrame*> CVideoFrameQueue::nolockDropOlderFrames()
{
    std::vector<CVideoFrame*> retVal;
    // Drop frames older that what we are holding - we want to provide the freshest possible data
    CVideoFrame* pTempFrame = NULL;
    while (size() > 1)
    {
        if (nolockRemoveHead(&pTempFrame))
        {
            //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   nolockDropOlderFrames dropped 1 frame\n");
            m_droppedFrames++;
            retVal.push_back(pTempFrame);
        }
    }
    //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   after nolockDropOlderFrames size = " + numberToText(m_queue.size()) + "\n");
    return retVal;
}

std::string CVideoFrameQueue::numberToText(unsigned int n)
{
    char buf[128];
    sprintf(buf, "%d", n);
    return buf;
}

bool CVideoFrameQueue::blockingRemoveHead(CVideoFrame** ppFrame, pthread_mutex_t& mutexQueue)
{
    if (!removeHead(ppFrame, mutexQueue))
    {
        int myErr = pthread_mutex_lock(&m_mutexThread);
        if (myErr != 0)
        {
            dbgMsg_d1("blockingRemoveHead pthread_mutex_lock error: %d\n", myErr);
        }
        //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   Before pthread_cond_wait\n");
        int rc = pthread_cond_wait(&m_condThread, &m_mutexThread);
        if (rc != 0)
        {
            std::string sMsg = "blockingRemoveHead() ret from pthread_cond_wait: " + numberToText(rc) + "\n";
            dbgMsg_s(sMsg);
        }
        //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   AFTER pthread_cond_wait\n");
        bool isRemoveOK = nolockRemoveHead(ppFrame);
        myErr = pthread_mutex_unlock(&m_mutexThread);
        if (myErr != 0)
        {
            dbgMsg_d1("blockingRemoveHead pthread_mutex_lock error: %d\n", myErr);
        }
        if (!isRemoveOK)
        {
            //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   Woke up after blocking remove, but nothing in the queue\n");
            *ppFrame = 0;
            return false;
        }
    }
    return true;
}

void CVideoFrameQueue::nolockAddTail(CVideoFrame* pFrame)
{
    CTestMonitor::getTicks(&pFrame->m_timeAddedToQueue[(int) m_eQueueType]);
    if (m_eQueueType == CVideoFrame::FRAME_QUEUE_FREE)
    {
        if (pFrame->m_pCameraVideoFrame2 != NULL)
        {
            m_queue.push_back(pFrame->m_pCameraVideoFrame2);
            pFrame->m_pCameraVideoFrame2 = NULL;
            //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   after return m_pCameraVideoFrame2 to FREE queue, size = " + numberToText(m_queue.size()) + "\n");
        }
    }
    m_queue.push_back(pFrame);
    //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   after nolockAddTail size = " + numberToText(m_queue.size()) + "\n");
}

bool CVideoFrameQueue::nolockRemoveHead(CVideoFrame** ppFrame)
{
    if (m_queue.size() == 0)
    {
        //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   nolockRemoveHead returned NULL (queue is empty)\n");
        *ppFrame = 0;
        return false;
    }
    *ppFrame = m_queue.back();
    if (m_eQueueType == CVideoFrame::FRAME_QUEUE_FREE)
    {
        if ((*ppFrame)->m_pCameraVideoFrame2 != NULL)
        {
            dbgMsg_s("FREE queue remove found non-null pCameraVideoFrame2");
            (*ppFrame)->m_pCameraVideoFrame2 = NULL;
        }
    }
    m_queue.pop_back();
    CTestMonitor::getTicks(&((*ppFrame)->m_timeRemovedFromQueue[(int) m_eQueueType]));
    //dbgMsg_s(CVideoFrame::queueTypeToText(m_eQueueType) + ":   nolockRemoveHead returned a frame OK\n");
    return true;
}
