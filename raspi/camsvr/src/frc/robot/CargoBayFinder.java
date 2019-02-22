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
import java.util.Arrays;
import java.util.Comparator;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

public class CargoBayFinder {
	public static final int MAX_CARGO_BAY = 12; // Up to 3 cargo side bays, 2 front bays, and a rocket bay behind
	public static final int MAX_VISION_TARGETS = 2 * MAX_CARGO_BAY; // 2 each

	RotatedRect m_largestRectangles[] = null;
	RotatedRect m_leftToRightRectangles[] = null;
	CargoBay m_foundCargoBays[] = null;

	int m_nValidRect = 0;
	int m_nValidCargoBay = 0;
	int m_idxNearestCenterX = 0;

	CargoBayFinder() {
		m_largestRectangles = new RotatedRect[MAX_VISION_TARGETS];
		m_leftToRightRectangles = new RotatedRect[MAX_VISION_TARGETS];
		int i=0;
		for(i=0; i<MAX_VISION_TARGETS; i++) {
			m_largestRectangles[i] = new RotatedRect();
			m_leftToRightRectangles[i] = new RotatedRect();
		}
				
		m_foundCargoBays = new CargoBay[MAX_CARGO_BAY];
		for(i=0; i<MAX_CARGO_BAY; i++) {
			m_foundCargoBays[i] = new CargoBay();
			m_foundCargoBays[i].init();
		}
	
		m_nValidRect = 0;
		m_nValidCargoBay = 0;
		m_idxNearestCenterX = 0;

		System.out.printf("End Constructor\n");
}

	public void init() {
		int i=0;
		for(i=0; i<MAX_VISION_TARGETS; i++) {
			initRotatedRect(m_largestRectangles[i]);
			initRotatedRect(m_leftToRightRectangles[i]);
		}
		for(i=0; i<MAX_CARGO_BAY; i++) {
			m_foundCargoBays[i].init();
		}

		m_nValidRect = 0;
		m_nValidCargoBay = 0;
		m_idxNearestCenterX = 0;
	}

	public void initFromContours(ArrayList<MatOfPoint> contours) {
		init();

		int i;
		for (i = 0; i < contours.size(); i++) {
			MatOfPoint2f myMat2f = new MatOfPoint2f(contours.get(i).toArray());
			RotatedRect rotRect = Imgproc.minAreaRect(myMat2f);
			m_leftToRightRectangles[m_nValidRect] = rotRect;
			m_largestRectangles[m_nValidRect++] = rotRect;
		}
		sortLeftToRight(m_leftToRightRectangles, m_nValidRect);
		sortLargestArea(m_largestRectangles, m_nValidRect);

		/*
		for (i = 0; i < m_nValidRect; i++) {
			System.out.printf("m_largestRectangles(%d).area = %f,     center x/y = %f / %f     tilt = %f\n", i, area(m_largestRectangles[i]),
					m_largestRectangles[i].center.x, m_largestRectangles[i].center.y, tilt(m_largestRectangles[i]));
		}

		System.out.printf("\n");
		for (i = 0; i < m_nValidRect; i++) {
			System.out.printf("m_leftToRightRectangles(%d).area = %f,     center x/y = %f / %f     tilt = %f\n", i, area(m_leftToRightRectangles[i]),
					m_leftToRightRectangles[i].center.x, m_leftToRightRectangles[i].center.y, tilt(m_leftToRightRectangles[i]));
		}
		System.out.printf("\n\n");
		*/

		// Now look for horizontally adjacent vision targets that ALSO tilt toward each
		// other
		for (i = 0; i < m_nValidRect-1; i++) {
			int idxNext = i + 1;
			
			// Skip if more than a factor of 4 difference in size
			if(area(m_leftToRightRectangles[idxNext]) == 0) {
				continue;
			}
			double ratio = Math.abs(area(m_leftToRightRectangles[i]) / area(m_leftToRightRectangles[idxNext]));
			if(ratio < 0.25) {
				continue;
			}
			if(ratio > 4.0) {
				continue;
			}
			
			// Skip if not tilted toward each other
			if( 0 < tilt(m_leftToRightRectangles[i])) {
				continue;
			}
			if( 0 > tilt(m_leftToRightRectangles[idxNext])) {
				continue;
			}
			
			m_foundCargoBays[m_nValidCargoBay++] = new CargoBay(m_leftToRightRectangles[i], m_leftToRightRectangles[idxNext]);
			i += 2; 
		}

		int minDistance = Main.FRAME_WIDTH;
		m_idxNearestCenterX = 0;
		for (i = 0; i < m_nValidCargoBay; i++) {
			int xDistance = Math.abs((int) m_foundCargoBays[i].centerX() - Main.FRAME_CENTER_PIXEL_X);
			if (minDistance > xDistance) {
				minDistance = xDistance;
				m_idxNearestCenterX = i;
			}
		}
	}

	public static void sortLeftToRight(RotatedRect[] a, int nValid) {
		Arrays.sort(a, 0, nValid, new Comparator<RotatedRect>() {
			public int compare(final RotatedRect a, final RotatedRect b) {
				Double c = Double.valueOf(a.center.x);
				Double d = Double.valueOf(b.center.x);
				return c.compareTo(d);
			}
		});
	}

	public static void sortLargestArea(RotatedRect[] a, int nValid) {
		Arrays.sort(a, 0, nValid, new Comparator<RotatedRect>() {
			public int compare(final RotatedRect a, final RotatedRect b) {
				Double c = Double.valueOf(a.size.width * a.size.height);
				Double d = Double.valueOf(b.size.width * b.size.height);
				return d.compareTo(c);
			}
		});
	}
	
	public static void initRotatedRect(RotatedRect rotRect) {
		rotRect.angle = 0.0;
		rotRect.center.x = Main.FRAME_WIDTH + 1;
		rotRect.center.y = 0.0;
		rotRect.size.height = 1.0;
		rotRect.size.width = 1.0;
	}
	
	public static double area(RotatedRect rotRect) {
		return rotRect.size.width * rotRect.size.height;
		}
		
	public static double tilt(RotatedRect rotRect) {
		// Have to look at the aspect ratio to determine tilt of the long side plus or minus around vertical zero degrees
		double myAngle = rotRect.angle;
		if(rotRect.size.width < rotRect.size.height) {
			myAngle += 90;
		}
		return myAngle;
	}

}
