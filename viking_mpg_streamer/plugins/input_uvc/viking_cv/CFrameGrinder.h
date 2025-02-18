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


#define NUMBER_OF_FRAMES_FOR_QUEUE 16

class CFrameGrinder
{
public:
    CFrameGrinder();
    ~CFrameGrinder();

    void init();
    void initVideo(int framesPerSec, unsigned int height, unsigned int width, int codec);

    // These are called 'safe' because only one thread at a time can add or remove from queues
    bool safeGetFreeFrame(CVideoFrame** ppFrame);
    void safeAddTail(CVideoFrame* pFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType);
    void safeAddTailAndPurgeOlder(CVideoFrame* pFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType);
    bool safeRemoveHead(CVideoFrame** ppFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType);
    bool safeBlockingRemoveHead(CVideoFrame** ppFrame, CVideoFrame::FRAME_QUEUE_TYPE eFrameQueueType);

    static void setGreenLED(bool turnLedOn);


public:
    CVideoFrameQueue m_frameQueueList[CVideoFrame::NUMBER_OF_FRAME_QUEUES];
    CConnectionServer m_connectionServer;
    CBlobDetector m_blobDetector;
    CTestMonitor m_testMonitor;

protected:
    pthread_mutex_t m_mutexQueue; // Allow only one thread at a time to add or remove from queues
    pthread_mutexattr_t m_mutexattrQueue;
    pthread_t m_cam_queue_thread;
    pthread_t m_blob_detect_thread;
};