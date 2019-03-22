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

public class JVideoFrame {
	Mat m_frame;
	Mat m_filteredFrame;
	Mat m_resizedFrame;
	long[] m_timeAddedToQueue = new long[JFrameQueueType.values().length];
	long[] m_timeRemovedFromQueue = new long[JFrameQueueType.values().length];
	JTargetInfo m_targetInfo;
	JTargetAnnotation m_targetAnnotation;

	JVideoFrame() {
		init();
	}

	void init() {
		if(m_targetInfo != null)
		{
			m_targetInfo = null;
		}
		m_targetInfo = new JTargetInfo();
		m_targetInfo.init();

		if(m_targetAnnotation != null)
		{
			m_targetAnnotation = null;
		}
		m_targetAnnotation = new JTargetAnnotation();
		m_targetAnnotation.init();

		for (int i = 0; i < JFrameQueueType.values().length; i++) {
			m_timeAddedToQueue[i] = 0;
			m_timeRemovedFromQueue[i] = 0;
		}
	}
}
