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

import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class QGripThreadRunnable implements Runnable {
	public boolean isShuttingDown = false;
	private static GripPipeline myGripPipeline = null;
	private static CargoBayFinder myCargoBayFinder = null;

	@Override
	public void run() {
		myGripPipeline = new GripPipeline();
		myCargoBayFinder = new CargoBayFinder();

		// All Mats and Lists should be stored outside the loop to avoid
		// allocations
		// as they are expensive to create

		double[] dMatrix = { 2.9482783765726424e+02, 0., 3.1480862269626300e+02, 0., 2.9482783765726424e+02,
				2.3886484081310755e+02, 0., 0., 1. };
		Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
		int row = 0, col = 0;
		cameraMatrix.put(row, col, dMatrix);

		double dDist[] = { -2.7415242407561496e-01, 6.0732740115875483e-02, 0., 0., -5.5934428233374665e-03 };
		Mat distCoeffs = new Mat(1, 5, CvType.CV_64FC1);
		distCoeffs.put(row, col, dDist);

		Scalar colorBlue = new Scalar(0, 0, 255);
		Point rect_points[] = new Point[4];

		while (!isShuttingDown) {
			
			// Why a one millisecond nap is needed in this thread
			//
			// The Linux scheduler runs very often, but will let CPU hungry threads like this one run for a long time without interruption.
			// (Because it is CPU expensive to change thread context)
			//
			// Processing of the camera is the main thread of this application, doing nothing but collecting from the camera.
			// Useful processing of the frame data is done on three other threads, each waiting on a different resource
			// * Camera thread wants USB (camera) resource and a little user level CPU
			// * GRIP/OpenCV processings wants lots of CPU resource
			// * Text message and stream threads want network resource and a tiny amount of user level CPU
			//
			// Camera, text, or stream threads very minor need for user level CPU might delay them for 10s or 100s of milliseconds waiting
			// for the GRIP thread to take a break.  With this 1 ms. sleep the scheduler will check to see if any other threads 
			// are ready to run at least once per frame
			//
			// This makes a difference in what the developer sees for an input overrun.  For example without this change if too much processing 
			// is attempted in the GRIP thread, the camera thread is starved for CPU and misses frames and does not count them
			// as dropped. None of the other threads drop packets so looks like all is well except frame rate is very low.
			// With this change the camera thread can collect incoming packets that might count as dropped if the GRIP thread can't keep up.
			// 
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			JVideoFrame frm = Main.myFrameQueue_WAIT_FOR_BLOB_DETECT.dropOlderAndRemoveHead();
			if (frm == null) {
				continue;
			}
			if (frm.m_frame == null)
			{
				frm.m_filteredFrame = new Mat();
			}
			if(frm.m_filteredFrame == null)
			{
				frm.m_filteredFrame = new Mat();
			}
			
			// Rasperry Pi not enough for undistort task
			// At 30FPS queued 13 more frames before finished processing one frame
			//Imgproc.undistort(frm.m_frame, frm.m_filteredFrame, cameraMatrix, distCoeffs);
			frm.m_filteredFrame = frm.m_frame;
			
		    frm.m_targetInfo.isCargoBayDetected = false;
			myGripPipeline.process(frm.m_frame);
			ArrayList<MatOfPoint> contours = myGripPipeline.findContoursOutput();
			
			if (!contours.isEmpty()) {
				myCargoBayFinder.initFromContours(contours);
				if(myCargoBayFinder.m_nValidCargoBay > 0)
				{
					frm.m_targetInfo.isCargoBayDetected = true;
					frm.m_targetInfo.visionPixelX = myCargoBayFinder.m_foundCargoBays[myCargoBayFinder.m_idxNearestCenterX].centerX();
				}
			}
			
			if(frm.m_targetInfo.isCargoBayDetected)
			{
				myCargoBayFinder.m_foundCargoBays[myCargoBayFinder.m_idxNearestCenterX].m_rectLeft.points(rect_points);
				int j=0;
				for( j = 0; j < 4; j++ )
				{
					Imgproc.line( frm.m_filteredFrame, rect_points[j], rect_points[(j+1)%4], colorBlue );					
				}
				
				myCargoBayFinder.m_foundCargoBays[myCargoBayFinder.m_idxNearestCenterX].m_rectRight.points(rect_points);
				for( j = 0; j < 4; j++ )
				{
					Imgproc.line( frm.m_filteredFrame, rect_points[j], rect_points[(j+1)%4], colorBlue );					
				}
			}
			
			if((frm.m_targetInfo.nSequence % 5) == 0) {
				Main.m_testMonitor.saveFrameToJpeg(frm.m_filteredFrame);
				Main.m_testMonitor.saveFrameToJpeg(frm.m_frame);
			}
			Main.myFrameQueue_WAIT_FOR_TEXT_CLIENT.addTail(frm);
		}
	}

}