package frc.robot;

import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

public class CargoBayFinder {
	int MAX_CARGO_BAY = 12;                        // Up to 3 cargo side bays, 2 front bays, and a rocket bay behind
	int MAX_VISION_TARGETS = 2 * MAX_CARGO_BAY;    // 2 each 
	RotatedRect  m_largestRectangles[] = new RotatedRect[MAX_VISION_TARGETS];  
	RotatedRect  m_leftToRightRectangles[] = new RotatedRect[MAX_VISION_TARGETS];  
	CargoBay m_foundCargoBays[] = new CargoBay[MAX_CARGO_BAY];
	int m_nValidRect = 0;
	int m_nValidCargoBay = 0;
	
	public void initFromContours(ArrayList<MatOfPoint> contours)
	{
		int i, j, k;
		for(i=0; i<contours.size(); i++)
		{
			Rect rectContour = Imgproc.boundingRect(contours.get(i));
			double area = rectContour.width * rectContour.height; 
			MatOfPoint2f points = new MatOfPoint2f(contours.get(i));
			RotatedRect rotRect = Imgproc.minAreaRect(points);
			if(m_nValidRect < MAX_VISION_TARGETS)
			{
				m_largestRectangles[m_nValidRect] = rotRect;
				m_nValidRect++;
			}
			else
			{
				for(j=0; j<m_nValidRect; j++)
				{
					if(m_largestRectangles[j].boundingRect().area() < area)
					{
						k = m_nValidRect-1;
						while(k > j)
						{
							m_largestRectangles[k] = m_largestRectangles[k-1];
						}
						m_largestRectangles[j] = rotRect;
					}
				}				
			}
		}
		
		// m_largestRctangles now contains the largest rectangles in order of size
		// Sort the list into horizontal order left to right  
		int nValid = 0;	
		for(i=0; i<m_nValidRect; i++)
		{
			Boolean isFound = false;
			for(j=0; i<nValid; j++)
			{
				if(m_largestRectangles[i].boundingRect().x < m_leftToRightRectangles[j].boundingRect().x)
				{
					k = nValid-1;
					while(k > j)
					{
						m_leftToRightRectangles[k] = m_leftToRightRectangles[k-1];
					}
					m_leftToRightRectangles[j] = m_largestRectangles[i];
					nValid++;
					isFound = true;
					break;
				}
			}
			if(!isFound)
			{
				m_leftToRightRectangles[nValid] = m_largestRectangles[i];
				nValid++;
			}
		}
		
		// Now look for horizontally adjacent vision targets that ALSO tilt toward each other
		for(i=0; i<m_nValidRect; i++)
		{
			int idxNext = i+1;
			if(m_leftToRightRectangles[i].angle > 0.0)
			{
				if(m_leftToRightRectangles[idxNext].angle < 0.0)
				{
					m_foundCargoBays[m_nValidCargoBay] = new CargoBay(m_leftToRightRectangles[i], m_leftToRightRectangles[idxNext]);
					m_nValidCargoBay++;
				}				
			}
		}

		System.out.println("m_nValidRect = " + m_nValidRect + "   m_nValidCargoBay = " + m_nValidCargoBay); 

		

	}

}
