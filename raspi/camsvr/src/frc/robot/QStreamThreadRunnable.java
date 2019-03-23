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

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;

public class QStreamThreadRunnable implements Runnable {

	static final int BROWSER_FRAME_WIDTH = 320;
	static final int BROWSER_FRAME_HEIGHT = 240;
	static final int BROWSER_FRAME_RATE = 30;

	private final org.opencv.core.Size m_sizeBrowserFrame = new org.opencv.core.Size(BROWSER_FRAME_WIDTH, BROWSER_FRAME_HEIGHT);
	
	@Override
	public void run() {
		CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, BROWSER_FRAME_WIDTH,
				BROWSER_FRAME_HEIGHT, BROWSER_FRAME_RATE);

		MjpegServer cvStream = new MjpegServer("CV Image Stream", JTargetInfo.streamAnnotatedSourcePortOnRaspberryPi);
		cvStream.setSource(imageSource);

		while (!Main.isShuttingDown) {
			JVideoFrame frm = Main.myFrameQueue_WAIT_FOR_BROWSER_CLIENT.dropOlderAndRemoveHead();
			if (frm == null) {
				continue;
			}
			
			frm.m_targetAnnotation.drawAnnotation(frm.m_filteredFrame);
			if ((frm.m_targetInfo.nSequence % 5) == 0) {
				Main.m_testMonitor.saveFrameToJpeg(frm.m_filteredFrame);
			}

			Imgproc.resize( frm.m_filteredFrame, frm.m_resizedFrame, m_sizeBrowserFrame);
			imageSource.putFrame(frm.m_resizedFrame);

			Main.myFrameQueue_FREE.addTail(frm);
		}
	}

}
