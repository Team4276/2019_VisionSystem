package frc.robot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class JTargetAnnotation {
	
	public Boolean m_showDebugAnnotation = true;
	
	public Boolean m_isCargoBayDetected = false;  
	public double m_visionPixelX = -1;
	RotatedRect m_rectLeft = null;
	RotatedRect m_rectRight = null;
		
	RotatedRect m_tooBigRectangles[] = null;
	RotatedRect m_tooSmallRectangles[] = null;
	RotatedRect m_tooFarRectangles[] = null;
	RotatedRect m_badTiltRectangles[] = null;
	RotatedRect m_cargoBayRectangles[] = null;

	int m_nTooBigRect = 0;
	int m_nTooSmallRect = 0;
	int m_nTooFarRect = 0;
	int m_nBadTiltRect = 0;
	int m_nCargoBayRect = 0;

	Scalar colorRed = new Scalar(0, 0, 255);
	Scalar colorBlue = new Scalar(255, 0, 0);
	Scalar colorYellow = new Scalar(0, 255, 255);
	Scalar colorOrange = new Scalar(0, 192, 192);
	Scalar colorCyan = new Scalar(255, 255, 0);
	Scalar colorWhite = new Scalar(255, 255, 255);
	Scalar colorGreen = new Scalar(0, 255, 0);
	
	Point startPointDebugText1 = new Point((int) 30, 30);
	Point startPointDebugText2 = new Point((int) 30, 60);
	Point startPointDebugText3 = new Point((int) 30, 90);


	JTargetAnnotation() {
		m_isCargoBayDetected = false;  
		m_visionPixelX = -1;
		m_rectLeft = new RotatedRect();
		m_rectRight = new RotatedRect();		
		
		m_tooBigRectangles = new RotatedRect[CargoBayFinder.MAX_VISION_TARGETS];
		m_tooSmallRectangles = new RotatedRect[CargoBayFinder.MAX_VISION_TARGETS];
		m_tooFarRectangles = new RotatedRect[CargoBayFinder.MAX_VISION_TARGETS];
		m_badTiltRectangles = new RotatedRect[CargoBayFinder.MAX_VISION_TARGETS];
		m_cargoBayRectangles = new RotatedRect[CargoBayFinder.MAX_VISION_TARGETS];

		m_nTooBigRect = 0;
		m_nTooSmallRect = 0;
		m_nTooFarRect = 0;
		m_nBadTiltRect = 0;
		m_nCargoBayRect = 0;
	}
	

	public void init() {
		m_isCargoBayDetected = false;  
		m_visionPixelX = -1;
		CargoBayFinder.initRotatedRect(m_rectLeft);
		CargoBayFinder.initRotatedRect(m_rectRight);
	}

	private static void drawRect(Mat img, RotatedRect rotRect, Scalar clrRect, int lineWidth)
	{
		Point rect_points[] = new Point[4];
		rotRect.points(rect_points);
		int j = 0;
		for (j = 0; j < 4; j++) {
			Imgproc.line(img, rect_points[j], rect_points[(j + 1) % 4], clrRect, lineWidth);
		}
	}

	private static void drawRectWithTilt(Mat img, RotatedRect rotRect, Scalar clrRect, Scalar clrText, int lineWidth)
	{
		drawRect(img, rotRect, clrRect, lineWidth);
		double tiltAngle = CargoBayFinder.tilt(rotRect);
		Integer iTemp = new Integer((int)tiltAngle);		
		Imgproc.putText(img, iTemp.toString(), rotRect.center, Core.FONT_HERSHEY_PLAIN, 1.0, clrText);
	}
	
	private static void drawPlus(Mat img, double X, double Y, Scalar clr)
	{
		Point pointLeft = new Point((int) X - 10, (int) Y);
		Point pointRight = new Point((int) X + 10, (int) Y);
		Imgproc.line(img, pointLeft, pointRight, clr, 3);
		Point pointUp = new Point((int) X, (int) Y + 10);
		Point pointDown = new Point((int) X, (int) Y - 10);
		Imgproc.line(img, pointUp, pointDown, clr, 3);
	}
	
	private static void drawMinus(Mat img, double X, double Y, double len, Scalar clr)
	{
		Point pointLeft = new Point((int) X, Y);
		Point pointRight = new Point((int) X + len, (int) Y);
		Imgproc.line(img, pointLeft, pointRight, clr, 3);
	}

	public void drawAnnotation(Mat myMat) {
		int i=0;
		
		if(m_showDebugAnnotation) {
			int totalRect = m_tooBigRectangles.length;
			totalRect += m_tooSmallRectangles.length;
			totalRect += m_tooFarRectangles.length;
			totalRect += m_badTiltRectangles.length;
			totalRect += m_cargoBayRectangles.length;
			Integer iTemp = new Integer((int)totalRect);		
	
			String msg = "[";
			msg += iTemp.toString();
			msg += "]total rectangles, [";
			iTemp = m_tooSmallRectangles.length;
			msg += iTemp.toString();
			msg += "]tooSmall";
			Imgproc.putText(myMat, msg, startPointDebugText1, Core.FONT_HERSHEY_PLAIN, 2.0, colorWhite);
			
			msg = "[";
			iTemp = m_tooBigRectangles.length;
			msg += iTemp.toString();
			msg += "]tooBig, [";
			iTemp = m_tooFarRectangles.length;
			msg += iTemp.toString();
			msg += "]tooFar";
			Imgproc.putText(myMat, msg, startPointDebugText2, Core.FONT_HERSHEY_PLAIN, 2.0, colorWhite);
			
			msg = "[";
			iTemp = m_badTiltRectangles.length;
			msg += iTemp.toString();
			msg += "]badTilt, [";
			iTemp = m_cargoBayRectangles.length;
			msg += iTemp.toString();
			msg += "]partOfValidCargoBay";
			Imgproc.putText(myMat, msg, startPointDebugText3, Core.FONT_HERSHEY_PLAIN, 2.0, colorWhite);

			for(i=0; i<m_nCargoBayRect; i++)
			{
				RotatedRect rect = m_cargoBayRectangles[i];
				drawRect(myMat, rect, colorGreen, 2);									
			}
			
			for(i=0; i<m_nTooBigRect; i++)
			{
				RotatedRect rect = m_tooBigRectangles[i];
				drawRect(myMat, rect, colorBlue, 2);									
			}
			for(i=0; i<m_nTooSmallRect; i++)
			{
				RotatedRect rect = m_tooSmallRectangles[i];
				drawRect(myMat, rect, colorBlue, 2);									
			}
			for(i=0; i<m_nTooFarRect; i++)
			{
				RotatedRect rect = m_tooFarRectangles[i];
				drawRect(myMat, rect, colorBlue, 2);									
			}
			
			for(i=0; i<m_nBadTiltRect; i++)
			{
				RotatedRect rect = m_badTiltRectangles[i];
				drawRect(myMat, rect, colorOrange, 2);									
			}
		}
	
		double X = Main.FRAME_WIDTH - 20;
		double Y = Main.IGNORE_ABOVE_THIS_Y_PIXEL;
		drawMinus(myMat, X, Y, 20, colorGreen);
	
		if(m_isCargoBayDetected) {
			drawRect(myMat, m_rectLeft, colorYellow, 2);
			drawRect(myMat, m_rectRight, colorYellow, 2);
	
			X = m_visionPixelX;
			Y = m_rectLeft.center.y;
			drawPlus(myMat, X, Y, colorYellow);
		} 
		else
		{
			drawRect(myMat, m_rectLeft, colorCyan, 2);
			drawRect(myMat, m_rectRight, colorCyan, 2);			
		}
	}
}
