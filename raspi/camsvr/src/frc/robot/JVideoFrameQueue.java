package frc.robot;
import java.util.concurrent.LinkedBlockingDeque;

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


public class JVideoFrameQueue 
{
    public JFrameQueueType m_eQueueType;
    public int m_droppedFrames;

    private LinkedBlockingDeque<JVideoFrame> m_queue;

	JVideoFrameQueue(JFrameQueueType eQueueType)
	{
	    m_eQueueType = eQueueType;
	    m_queue = new LinkedBlockingDeque<JVideoFrame>();
	}
	
	public int size()
	{
		return m_queue.size();
	}
	
	boolean addTail(JVideoFrame frm)
	{
		if(m_queue.add(frm))
		{
			frm.m_timeAddedToQueue[m_eQueueType.toInt()] = System.nanoTime();
			return true;
		}
		if(m_eQueueType != JFrameQueueType.FREE)
		{
			Main.myFrameQueue_FREE.addTail(frm);
		}
		else
		{
			System.out.printf("JVideoFrameQueue RESOURCE LEAK --> Unable to return frame to the free queue\n");			
		}
		return false;
	}
	
	JVideoFrame removeHead()
	{
		if(m_eQueueType == JFrameQueueType.FREE)
		{
			if(m_queue.isEmpty())
			{
				return null;
			}
		}


		try 
		{
			// Waits for a frame if none in the queue
			JVideoFrame frm = m_queue.take();
			frm.m_timeAddedToQueue[m_eQueueType.toInt()] = System.nanoTime();
			return frm; 
		}
		catch(Exception e)
		{
			System.out.printf("JVideoFrameQueue remove exception  %s\n", e.getMessage());
		}
		return null;
	}
	
	JVideoFrame dropOlderAndRemoveHead()
	{
		while( (!Main.isShuttingDown) && (m_queue.size() > 0) )
		{
			if(m_queue.size() == 1)
			{
				return removeHead();				
			}
			JVideoFrame frm = removeHead();
			m_droppedFrames++;
			if(m_eQueueType != JFrameQueueType.FREE)
			{
				Main.myFrameQueue_FREE.addTail(frm);
			}
			else
			{
				System.out.printf("[2] JVideoFrameQueue RESOURCE LEAK --> Unable to return frame to the free queue\n");			
			}
		}
		return removeHead();				
	}
}
