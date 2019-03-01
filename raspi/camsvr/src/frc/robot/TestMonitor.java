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

package frc.robot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class TestMonitor {
		
	static final String HOME_NAME = "pi";
	static final int NUMBER_OF_TIME_IN_TASK = 50;

	long m_avgTimeElapsed[];
	String m_sBaseFileName;
	String m_sLogFolder;
	String m_sLogVideoFolder;
	boolean m_isVideoRecording = false;
	int m_nNextFile = 0;
	int m_nMaxFileNumber = 10000;
	long m_timeStartBatch = System.nanoTime();
	
	TestMonitor()
	{
	    init();
	}
	
	void init()
	{
	    Date today = new Date();
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-hhmmss");
	    m_sBaseFileName = formatter.format(today); 
	    	
	    m_sLogFolder = "/home/";
	    m_sLogFolder += HOME_NAME;
	    m_sLogFolder += "/log";
	    m_sLogVideoFolder = "/home/";
	    m_sLogVideoFolder += HOME_NAME;
	    m_sLogVideoFolder += "/logVideo";
	    m_nNextFile = 0;
	}
	
	void initVideo(int framesPerSec, int height, int width, int codec)
	{
	    //m_isVideoRecording = enableVideoCollection(true, framesPerSec, height, width, codec);
	}
	
	String numberToText(int n)
	{
		Integer iTemp = new Integer(n);
		return iTemp.toString();
	}
	
	String numberToText00(int n)
	{
		return "00" + numberToText(n);
	}
	
	String numberToText0000(int n)
	{
		return "00" + numberToText00(n);
	}
	
	String getNextFilePath(String sFolderPath)
	{
	    String sRet = sFolderPath;
	    sRet += "/";
	    sRet += m_sBaseFileName;

	    sRet += numberToText0000(m_nNextFile++);
		if (m_nNextFile >= m_nMaxFileNumber)
	    {
	        m_nNextFile = 0;
	    }
	    return sRet;
	}
	
	String getLogFilePath()
	{
	    String sRet = m_sLogFolder;
	    sRet += "/log-";
	    sRet += m_sBaseFileName;
	    sRet += ".txt";
	    return sRet;
	}
	
	void deleteFileByNumberIfExists(int nFile, String sFolderPath)
	{
	    String sCmd = sFolderPath;
	    sCmd += "/";
	    sCmd += m_sBaseFileName;
	    sCmd += numberToText0000(nFile);
	    sCmd += "*.*";
		File f = new File(sCmd);
		f.delete();
	}
	
	boolean logWrite(String sLine)
	{
	    System.out.printf(sLine);	
	    return true;
	}
	/*
	boolean enableVideoCollection(boolean bEnable, int framesPerSec, int height, int width, int codec)
	{
	    int nColor = 1;
	    Size mySize = Size(width, height);
	
	    String sFilePath = getNextFilePath(m_sLogVideoFolder);
	    sFilePath += ".avi";
	
	    //make output video file  
	    m_outVideo.open(sFilePath, codec, framesPerSec, mySize, nColor);
	    if (!m_outVideo.isOpened())
	    {
	        return false;
	    }
	    return true;
	}
	
	boolean saveVideoFrame(Mat frame)
	{
	    if (m_isMonitorEnabled && m_isVideoRecording)
	    {
	        m_outVideo << frame;
	    }
	}
	*/
	
	boolean saveFrameToJpeg(Mat frame)
	{
	    deleteFileByNumberIfExists(m_nNextFile, m_sLogFolder);
	    String sFileName = getNextFilePath(m_sLogFolder) + ".jpg";
	    //System.out.printf("Saving %s\n", sFileName);	
	    return Imgcodecs.imwrite(sFileName, frame);
	}
	
	long getDeltaTimeSeconds(long timeStart, long timeEnd)
	{
	    long dTemp = getDeltaTimeMilliseconds(timeStart, timeEnd);
	    dTemp /= 1000.0;
	    return dTemp;
	}
	
	long getDeltaTimeMilliseconds(long timeStart, long timeEnd)
	{
		return (timeEnd - timeStart) / (1000*1000);
	}
	
	void displayQueueLengths()
	{
		System.out
		.printf("\n\nFrame Queues --> FREE: %d   BLOB: %d  TEXT: %d  BROWSER: %d\n",
				Main.myFrameQueue_FREE.size(),
				Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.size(),
				Main.myFrameQueue_WAIT_FOR_TEXT_CLIENT.size(),
				Main.myFrameQueue_WAIT_FOR_BROWSER_CLIENT
						.size());
		System.out
		.printf("     Dropped --> FREE: %d   BLOB: %d  TEXT: %d  BROWSER: %d\n",
				Main.myFrameQueue_FREE.m_droppedFrames,
				Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames,
				Main.myFrameQueue_WAIT_FOR_TEXT_CLIENT.m_droppedFrames,
				Main.myFrameQueue_WAIT_FOR_BROWSER_CLIENT.m_droppedFrames);
		int iTotalDropped = Main.myFrameQueue_FREE.m_droppedFrames 
		+ Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames
		+ Main.myFrameQueue_WAIT_FOR_TEXT_CLIENT.m_droppedFrames
		+ Main.myFrameQueue_WAIT_FOR_BROWSER_CLIENT.m_droppedFrames;
		
		long timeDeltaMillisecs = getDeltaTimeMilliseconds(m_timeStartBatch, System.nanoTime());
		System.out.printf("timeThisBatch = %d ms.   timePerFrame = %d ms.\n", timeDeltaMillisecs, timeDeltaMillisecs);
		m_timeStartBatch = System.nanoTime();
	}
	
	/*
	void monitorQueueTimesBeforeReturnToFreeQueue(CVideoFrame* pFrame, CFrameGrinder* pFrameGrinder)
	{
	    String sLine;
	    static struct timespec timeAtStartOfInterval = {0};
	    String sMsg;
	    int i = 0;
	    double timeIntervalSeconds = 0.0;
	    getTicks(&pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_FREE]);
	    if (m_nTasksDone[TASK_DONE_CAM1] >= NUMBER_OF_SAMPLES_PER_TEST_INTERVAL)
	    {
	        timeIntervalSeconds = getDeltaTimeSeconds(
	                                                  timeAtStartOfInterval,
	                                                  pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_FREE]);
	        timeAtStartOfInterval = pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_FREE];
	        if (m_nIntervalisClosestObjectFound == 0)
	        {
	            sLine += "Closest object not found,     ";
	        }
	        else
	        {
	            sLine += "Closest object (";
	            sLine += numberToText(m_nIntervalisClosestObjectFound);
	            sLine += " in this interval) avg ";
	        }
	        if (m_nCountStereoFramesInThisInterval > 0)
	        {
	            sLine += "      Stereo frames in this interval: ";
	            sLine += numberToText(m_nCountStereoFramesInThisInterval);
	        }
	        if (m_nCountMonoFramesInThisInterval > 0)
	        {
	            sLine += "      Mono frames in this interval: ";
	            sLine += numberToText(m_nCountMonoFramesInThisInterval);
	        }
	        sLine += "\n";
	        m_nCountStereoFramesInThisInterval = 0;
	        m_nCountMonoFramesInThisInterval = 0;
	
	        sLine += displayQueueLengths(pFrameGrinder);
	
	        for (i = 0; i < NUMBER_OF_TIME_IN_TASK; i++)
	        {
	            m_avgElapsedSeconds[i] /= NUMBER_OF_SAMPLES_PER_TEST_INTERVAL;
	        }
	        m_avgTimeBetweenCameraFramesMilliseconds[0] = m_sumTimeBetweenCameraFramesMilliseconds[0] / NUMBER_OF_SAMPLES_PER_TEST_INTERVAL;
	        m_avgTimeBetweenCameraFramesMilliseconds[1] = m_sumTimeBetweenCameraFramesMilliseconds[1] / NUMBER_OF_SAMPLES_PER_TEST_INTERVAL;
	        m_sumTimeBetweenCameraFramesMilliseconds[0] = 0;
	        m_sumTimeBetweenCameraFramesMilliseconds[1] = 0;
	        m_avgLatencyForProcessingFrameMilliseconds /= NUMBER_OF_SAMPLES_PER_TEST_INTERVAL;
	        memcpy(&m_savedElapsedSeconds, &m_avgElapsedSeconds, sizeof (double)*NUMBER_OF_TIME_IN_TASK);
	        sLine += displayQueueTimes(timeIntervalSeconds, pFrameGrinder);
	
	        logWrite(sLine);
	        sLine = "";
	
	        for (i = 0; i < NUMBER_OF_TASK_DONE_TYPES; i++)
	        {
	            m_nTasksDone[i] = 0;
	        }
	
	        m_nIntervalisClosestObjectFound = 0;
	
	        memset(&m_avgElapsedSeconds, 0, sizeof (int)*NUMBER_OF_TIME_IN_TASK);
	        m_avgTimeBetweenCameraFramesMilliseconds[0] = 0;
	        m_avgTimeBetweenCameraFramesMilliseconds[1] = 0;
	        m_avgLatencyForProcessingFrameMilliseconds = 0;
	
	        // 'true' is returned when the end of the interval is reached
	        for (i = 0; i < CVideoFrame::NUMBER_OF_FRAME_QUEUES; i++)
	        {
	            pFrameGrinder->m_frameQueueList[i].m_droppedFrames = 0;
	        }
	    }
	    if (pFrame->m_targetInfo.isClosestObjectFound())
	    {
	        m_nIntervalisClosestObjectFound++;
	    }
	
	    m_avgElapsedSeconds[TIME_IN_TASK_PLACEHOLDER1] = 0.0;
	    m_avgElapsedSeconds[TIME_IN_TASK_PLACEHOLDER2] = 0.0;
	
	    m_avgElapsedSeconds[TIME_IN_TASK_CAMERA] += getDeltaTimeSeconds(
	                                                                    pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_FREE], // earlier time
	                                                                    pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT]); // later time
	
	    m_avgElapsedSeconds[TIME_IN_TASK_CAMERA_2] = m_avgElapsedSeconds[TIME_IN_TASK_CAMERA];
	
	    m_avgElapsedSeconds[TIME_IN_TASK_WAIT_FOR_BLOB_DETECT] += getDeltaTimeSeconds(
	                                                                                  pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT],
	                                                                                  pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT]);
	
	    m_avgElapsedSeconds[TIME_IN_TASK_BLOB_DETECT] += getDeltaTimeSeconds(
	                                                                         pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT],
	                                                                         pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT]);
	
	    m_avgElapsedSeconds[TIME_IN_TASK_WAIT_FOR_TEXT_CLIENT] += getDeltaTimeSeconds(
	                                                                                  pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT],
	                                                                                  pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT]);
	
	    m_avgElapsedSeconds[TIME_IN_TASK_TEXT_CLIENT] += getDeltaTimeSeconds(
	                                                                         pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_TEXT_CLIENT],
	                                                                         pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT]);
	
	    m_avgElapsedSeconds[TIME_IN_TASK_WAIT_FOR_BROWSER_CLIENT] += getDeltaTimeSeconds(
	                                                                                     pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT],
	                                                                                     pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT]);
	
	    m_avgElapsedSeconds[TIME_IN_TASK_BROWSER_CLIENT] += getDeltaTimeSeconds(
	                                                                            pFrame->m_timeRemovedFromQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT],
	                                                                            pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_FREE]);
	
	    m_avgElapsedSeconds[TIME_TOTAL_CAMDONE_TO_TEXTDONE] += getDeltaTimeSeconds(
	                                                                               pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT],
	                                                                               pFrame->m_timeAddedToQueue[(int) CVideoFrame::FRAME_QUEUE_WAIT_FOR_BROWSER_CLIENT]);
	    m_sumTimeBetweenCameraFramesMilliseconds[0] += getTimeSinceLastCameraFrame(0);
	    m_sumTimeBetweenCameraFramesMilliseconds[1] += getTimeSinceLastCameraFrame(1);
	    m_avgLatencyForProcessingFrameMilliseconds += pFrame->m_targetInfo.getTimeLatencyThisCameraFrameMilliseconds();
	}
	
	void updateTimeSinceLastCameraFrame(int idCamera)
	{
	    struct timespec timeNow = {0};
	    getTicks(&timeNow);
	
	    int idx = 0;
	    if (0 != idCamera)
	    {
	        idx = 1;
	    }   
	    m_timeSinceLastCameraFrame[idx] = (int) getDeltaTimeMilliseconds(m_timeLastCameraFrame[idx], timeNow);
	    m_timeLastCameraFrame[idx] = timeNow;
	}
	*/
	
	String padString(String str, int desiredLength)
	{
	    while (str.length() < desiredLength)
	    {
	
	        str += " ";
	    }
	    return str;
	}
	
	/*
	String displayQueueTimes(double timeIntervalSeconds, CFrameGrinder* pFrameGrinder) 
	{
	    double dTemp;
	    char buf[256];
	    static String initTitles[] = {"CAM1", "CAM2", "BlobDetect", "Text", "Browser", "Total", " "};
	    String sLine, sRet;
	
	    sLine = "";
	    int i = 0;
	    int nColSize = 20;
	    while (0 != initTitles[i].compare(" "))
	    {
	        padString(sLine, i * nColSize);
	        sLine += initTitles[i];
	        i++;
	    }
	    sRet += sLine;
	    sRet += "\n";
	
	    sLine = "";
	    nColSize = 10;
	    for (i = 0; i < NUMBER_OF_TIME_IN_TASK; i++)
	    {
	        padString(sLine, i * nColSize);
	        dTemp = m_savedElapsedSeconds[i];
	        dTemp *= 1000.0;
	        sprintf(buf, "%0.2fms", dTemp);
	        sLine += buf;
	    }
	    sRet += sLine;
	    sRet += "\n";
	
	    sLine = "Done: ";
	    nColSize = 20;
	    for (i = 0; i < NUMBER_OF_TASK_DONE_TYPES; i++)
	    {
	        padString(sLine, 10 + (i * nColSize));
	        sprintf(buf, "%d", m_nTasksDone[i]);
	        sLine += buf;
	    }
	    sRet += sLine;
	    sRet += "\n";
	
	    sLine = "Drop: ";
	    nColSize = 20;
	    for (i = 1; i < CVideoFrame::NUMBER_OF_FRAME_QUEUES - 1; i++)
	    {
	        padString(sLine, 10 + ((i - 1) * nColSize));
	        sprintf(buf, "%d", pFrameGrinder->m_frameQueueList[i].m_droppedFrames);
	        sLine += buf;
	    }
	    sRet += sLine;
	    sRet += "\n";
	
	    int framesPerSecTextComplete = 0;
	    if (timeIntervalSeconds > 0)
	    {
	        framesPerSecTextComplete = m_nTasksDone[TASK_DONE_TEXT] / timeIntervalSeconds;
	    }
	    sprintf(buf, "viking_cv Version %d.%d.%d    Interval:  %02f sec", VERSION_YEAR, VERSION_INTERFACE, VERSION_BUILD, timeIntervalSeconds);
	    sRet += buf;
	    sRet += "\n";
	    sRet += "avgTimeBetweenCameraFrames: ";
	    dTemp = m_avgTimeBetweenCameraFramesMilliseconds[0];
	    sprintf(buf, "%0.2f", dTemp);
	    sRet += buf;
	    sRet += "ms/";
	    dTemp = m_avgTimeBetweenCameraFramesMilliseconds[1];
	    sprintf(buf, "%0.2f", dTemp);
	    sRet += buf;
	    sRet += "ms    avgLatencyForProcessingFrame: ";
	    sprintf(buf, "a%0.2f", m_avgLatencyForProcessingFrameMilliseconds);
	    sRet += buf;
	    sRet += "ms\n\n";
	
	    return sRet;
	}
	*/
}
