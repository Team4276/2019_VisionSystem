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
import org.opencv.core.Point;
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
	
	private static MatOfPoint2f myMat2f = new MatOfPoint2f();
	
	private static Comparator<RotatedRect> myAreaComparator = new Comparator<RotatedRect>() {
		public int compare(final RotatedRect a, final RotatedRect b) {
			Double c = Double.valueOf(a.size.width * a.size.height);
			Double d = Double.valueOf(b.size.width * b.size.height);
			return d.compareTo(c);
		}
	};

	CargoBayFinder() {
		m_largestRectangles = new RotatedRect[MAX_VISION_TARGETS];
		m_leftToRightRectangles = new RotatedRect[MAX_VISION_TARGETS];
		int i = 0;
		for (i = 0; i < MAX_VISION_TARGETS; i++) {
			m_largestRectangles[i] = new RotatedRect();
			m_leftToRightRectangles[i] = new RotatedRect();
		}

		m_foundCargoBays = new CargoBay[MAX_CARGO_BAY];
		for (i = 0; i < MAX_CARGO_BAY; i++) {
			m_foundCargoBays[i] = new CargoBay();
			m_foundCargoBays[i].init();
		}

		m_nValidRect = 0;
		m_nValidCargoBay = 0;
		m_idxNearestCenterX = 0;

		System.out.printf("End Constructor\n");
	}

	public void init() {
		int i = 0;
		for (i = 0; i < MAX_VISION_TARGETS; i++) {
			initRotatedRect(m_largestRectangles[i]);
			initRotatedRect(m_leftToRightRectangles[i]);
		}
		for (i = 0; i < MAX_CARGO_BAY; i++) {
			m_foundCargoBays[i].init();
		}

		m_nValidRect = 0;
		m_nValidCargoBay = 0;
		m_idxNearestCenterX = 0;
	}

	private void insertKeepingLargest(RotatedRect rotRect) {
		
		if(rotRect.center.y < Main.IGNORE_ABOVE_THIS_Y_PIXEL)
		{
			// Zero Y pixel is at the top of the frame, e.g. (0,0) is upper left corner
			return;
		}
		if (area(rotRect) <= 9) {
			return;
		}
		if (m_nValidRect < MAX_VISION_TARGETS) {
			m_largestRectangles[m_nValidRect++] = rotRect;
			return;
		}

		double smallestArea = 640 * 480;
		int idxSmallest = -1;
		for (int i = 0; i < m_nValidRect; i++) {
			if (smallestArea > area(m_largestRectangles[i])) {
				smallestArea = area(m_largestRectangles[i]);
				idxSmallest = i;
			}
		}
		double myArea = area(rotRect);
		if (smallestArea < myArea) {
			m_largestRectangles[idxSmallest] = rotRect;
		}

	}

	public JTargetAnnotation initFromContours(ArrayList<MatOfPoint> contours) {
		init();

		JTargetAnnotation retVal = new JTargetAnnotation();
		int i;
		for (i = 0; i < contours.size(); i++) {
			myMat2f.fromArray(contours.get(i).toArray());
			RotatedRect rotRect = Imgproc.minAreaRect(myMat2f);
			insertKeepingLargest(rotRect);
		}
		if (m_nValidRect < 2) {
			//System.out.printf("Need at least 2 rectangles, m_nValidRect = %d, nContours = %d\n", m_nValidRect, contours.size());
			return retVal;
		}
		sortLargestArea(m_largestRectangles, m_nValidRect);

		if (m_nValidRect > 1) {
			for (i = 0; i < m_nValidRect - 1; i++) {
				int idxNext = i + 1;

				// Look for pairs within 70% of the same area
				double area1 = area(m_largestRectangles[i]);
				double area2 = area(m_largestRectangles[idxNext]);
				if (area2 > 0) {
					double ratio = area1 / area2;
					if (ratio > (10.0 / 7.0)) {
						retVal.m_tooBigRectangles[retVal.m_nTooBigRect++] = m_largestRectangles[i];
						continue;
					}
					if (ratio < (0.7)) {
						retVal.m_tooSmallRectangles[retVal.m_nTooSmallRect++] = m_largestRectangles[i];
						continue;
					}
				}
				
				// Skip if too far apart
				double dist = len(m_largestRectangles[i].center, m_largestRectangles[idxNext].center);
				double ht = Math.max(m_largestRectangles[i].size.height, m_largestRectangles[idxNext].size.height);
				if(dist > (5*ht))
				{
					retVal.m_tooFarRectangles[retVal.m_nTooFarRect++] = m_largestRectangles[i];
					continue;
				}
				
				if(!areRectsTiltedTowardsEachOther(m_largestRectangles[i], m_largestRectangles[idxNext]))
				{
					retVal.m_badTiltRectangles[retVal.m_nBadTiltRect++] = m_largestRectangles[i];
					continue;
				}				
				retVal.m_cargoBayRectangles[retVal.m_nCargoBayRect++] = m_largestRectangles[i];
				retVal.m_cargoBayRectangles[retVal.m_nCargoBayRect++] = m_largestRectangles[idxNext];
				m_foundCargoBays[m_nValidCargoBay++].set(m_largestRectangles[i], m_largestRectangles[idxNext]);
				
			}
		}
		return retVal;
	}

	public void displayText() {
		if (m_nValidCargoBay > 0) {
			System.out.printf("DETECTED");
		} else {
			System.out.printf("Not detected");
		}
		System.out.printf("nValidRect = %d   nValidBays = %d\n", m_nValidRect, m_nValidCargoBay);
		if (m_nValidRect > 0) {
			System.out.printf("Rectangles:\n");
			double distNext = 0.0;
			int i = 0;
			for (i = 0; i < m_nValidRect; i++) {
				int idxNext = i + 1;
				if (idxNext < (m_nValidRect - 1)) {
					distNext = len(m_largestRectangles[i].center, m_largestRectangles[idxNext].center);
				}
				System.out.printf("    X, area, height, distNext, tilt = %d,%d,%d,%d,%d\n",
						(int) m_largestRectangles[i].center.x, (int) area(m_largestRectangles[i]),
						(int) m_largestRectangles[i].size.height, (int) distNext, (int) tilt(m_largestRectangles[i]));
			}
		}
		if (m_nValidCargoBay > 0) {
			System.out.printf("Cargo Bays:\n");
			int i = 0;
			for (i = 0; i < m_nValidCargoBay; i++) {
				System.out.printf("    Left(X, Y, area, tilt) = %d,%d,%d,%d    Right(X, Y, area, tilt) = %d,%d,%d,%d\n",
						(int) m_foundCargoBays[i].m_rectLeft.center.x, (int) m_foundCargoBays[i].m_rectLeft.center.y,
						(int) area(m_foundCargoBays[i].m_rectLeft), (int) tilt(m_foundCargoBays[i].m_rectLeft),
						(int) m_foundCargoBays[i].m_rectRight.center.x, (int) m_foundCargoBays[i].m_rectRight.center.y,
						(int) area(m_foundCargoBays[i].m_rectRight), (int) tilt(m_foundCargoBays[i].m_rectRight));
			}
		}
		System.out.printf("\n");
	}
	
	public static void sortLargestArea(RotatedRect[] a, int nValid) {
		Arrays.sort(a, 0, nValid, myAreaComparator);
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

	public static double len(Point p1, Point p2) {
		// input points are in no particular order. This function puts left on the left
		// before comparing tilt
		Point pLeft, pRight;
		if (p1.x < p2.x) {
			pLeft = p1;
			pRight = p2;
		} else {
			pLeft = p2;
			pRight = p1;
		}
		double dx = pRight.x - pLeft.x;
		double dy = pRight.y - pLeft.y;
		return Math.sqrt((dx * dx) + (dy * dy));
	}

	public static double slope(Point p1, Point p2) {
		// input points are in no particular order. This function puts left on the left
		// before comparing tilt
		Point pLeft, pRight;
		if (p1.x < p2.x) {
			pLeft = p1;
			pRight = p2;
		} else {
			pLeft = p2;
			pRight = p1;
		}
		double dx = pRight.x - pLeft.x;
		double dy = pRight.y - pLeft.y;
		if (dx == 0) {
			return 0.0;
		}

		double radians = Math.atan(dy / dx); // dy = opposite / dx = adjacent
		return radians * (180 / Math.PI);
	}

	public static boolean areRectsTiltedTowardsEachOther(RotatedRect rotRect1, RotatedRect rotRect2) {
		// input rects are in no particular order. This function puts left on the left
		// before comparing tilt
		double tiltLeft, tiltRight;
		if (rotRect1.center.x < rotRect2.center.x) {
			tiltLeft = tilt(rotRect1);
			tiltRight = tilt(rotRect2);
		} else {
			tiltLeft = tilt(rotRect2);
			tiltRight = tilt(rotRect1);
		}

		// 0.0 degrees is straight up for a rectangle with long side horizontal
		// Typical cargo bay angles will be -75 on the left and +75 on the right
		// Subtract right from left, cargo bay is -150 but mismatched halves is +150
		// Put another way, subtract right from left is negative when tilt toward and
		// positive for tilt away
		double diff = (tiltLeft - tiltRight);
		return (diff < 0.0);
	}

	// tilt angle in degrees, 0.0 is straight up
	public static double tilt(RotatedRect rotRect) {
		double blob_angle_deg = rotRect.angle;
		if (rotRect.size.width < rotRect.size.height) {
			blob_angle_deg = 90 + blob_angle_deg;
		}
		return blob_angle_deg;
	}

	public static boolean areRectsTiltedTowardsEachOther2(RotatedRect rotRect1, RotatedRect rotRect2) {
		// input rects are in no particular order. This function puts left on the left
		// before comparing tilt
		if (rotRect1.center.x < rotRect2.center.x) {
			return (tilt(rotRect1) > tilt(rotRect2));
		}
		return (tilt(rotRect2) > tilt(rotRect1));
	}

	// tilt angle in degrees, 0.0 is straight up
	public static double tilt2(RotatedRect rotRect) {
		Point rect_points[] = new Point[4];
		rotRect.points(rect_points);

		double len1 = len(rect_points[0], rect_points[1]);
		double len2 = len(rect_points[1], rect_points[2]);

		System.out.printf("tilt pts(x,y) = (%d,%d),(%d,%d),(%d,%d),  len1,len2 = %d,%d    ", (int) rect_points[0].x,
				(int) rect_points[0].y, (int) rect_points[1].x, (int) rect_points[1].y, (int) rect_points[2].x,
				(int) rect_points[2].y, (int) len1, (int) len2);

		double myTilt = 0.0;
		if (len1 > len2) {
			myTilt = slope(rect_points[0], rect_points[1]);
			System.out.printf("len1 longer   ");
		} else {
			myTilt = slope(rect_points[0], rect_points[1]);
			System.out.printf("len2 longer   ");
		}
		System.out.printf("tilt = %d\n", (int) myTilt);
		return myTilt;
	}
}
