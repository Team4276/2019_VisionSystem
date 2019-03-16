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
	long m_nFramesDroppedAsOfLastBatch = 0;
	long m_avgTimeBetweenFrames = 0;
	long m_avgLatencyBeforeShipTextMessage = 0;
	
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
	
	void addStat(JVideoFrame frm) {
		m_avgTimeBetweenFrames += frm.m_targetInfo.timeSinceLastCameraFrameMilliseconds;
		m_avgLatencyBeforeShipTextMessage += frm.m_targetInfo.timeLatencyThisCameraFrameMilliseconds;
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
		
		int nFramesProcessed = NUMBER_OF_TIME_IN_TASK - Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames;
		
		long nDroppedThisBatch =  Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames - m_nFramesDroppedAsOfLastBatch;
		m_nFramesDroppedAsOfLastBatch = Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames;
		System.out.printf("In this batch of %d dropped %d frames\n", NUMBER_OF_TIME_IN_TASK, nDroppedThisBatch);
		
		long timeDeltaMillisecs = getDeltaTimeMilliseconds(m_timeStartBatch, System.nanoTime());
		long avgPerFrame = timeDeltaMillisecs / (NUMBER_OF_TIME_IN_TASK - nDroppedThisBatch);
		System.out.printf("timeThisBatch = %d ms.   timePerFrame = %d ms.\n", timeDeltaMillisecs, avgPerFrame);
		m_timeStartBatch = System.nanoTime();
		
		System.out.printf("avgTimeSinceLastCameraFrame = %d ms.   avgLatencyBeforeShipTextMessage = %d ms.\n", m_avgTimeBetweenFrames/(NUMBER_OF_TIME_IN_TASK*1000000), m_avgLatencyBeforeShipTextMessage/(NUMBER_OF_TIME_IN_TASK*1000000));		
		m_avgTimeBetweenFrames = 0;
		m_avgLatencyBeforeShipTextMessage = 0;
	}

	String padString(String str, int desiredLength)
	{
	    while (str.length() < desiredLength)
	    {	
	        str += " ";
	    }
	    return str;
	}
}
