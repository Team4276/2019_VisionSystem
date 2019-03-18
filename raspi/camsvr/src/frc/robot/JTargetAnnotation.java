package frc.robot;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class JTargetAnnotation {
	public Boolean m_isCargoBayDetected = false;  
	public double m_visionPixelX = -1;
	RotatedRect m_rectLeft = null;
	RotatedRect m_rectRight = null;
	
	JTargetAnnotation() {
		m_isCargoBayDetected = false;  
		m_visionPixelX = -1;
		m_rectLeft = new RotatedRect();
		m_rectRight = new RotatedRect();		
	}
	

	public void init() {
		m_isCargoBayDetected = false;  
		m_visionPixelX = -1;
		CargoBayFinder.initRotatedRect(m_rectLeft);
		CargoBayFinder.initRotatedRect(m_rectRight);
	}

	private static void drawRect(Mat img, RotatedRect rotRect, Scalar clrRect, Scalar clrText)
	{
		Point rect_points[] = new Point[4];
		rotRect.points(rect_points);
		int j = 0;
		for (j = 0; j < 4; j++) {
			Imgproc.line(img, rect_points[j], rect_points[(j + 1) % 4], clrRect, 2);
		}
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

		Scalar colorRed = new Scalar(0, 0, 255);
		Scalar colorBlue = new Scalar(255, 0, 0);
		Scalar colorYellow = new Scalar(0, 255, 255);
		Scalar colorOrange = new Scalar(0, 192, 192);
		Scalar colorCyan = new Scalar(255, 255, 0);
		Scalar colorWhite = new Scalar(255, 255, 255);
		Scalar colorGreen = new Scalar(0, 255, 0);

		double X = Main.FRAME_WIDTH - 20;
		double Y = Main.IGNORE_ABOVE_THIS_Y_PIXEL;
		drawMinus(myMat, X, Y, 20, colorGreen);

		if(m_isCargoBayDetected) {
			drawRect(myMat, m_rectLeft, colorCyan, colorWhite);
			drawRect(myMat, m_rectRight, colorCyan, colorWhite);

			X = m_visionPixelX;
			Y = m_rectLeft.center.y;
			drawPlus(myMat, X, Y, colorYellow);
		} 
		else
		{
			drawRect(myMat, m_rectLeft, colorCyan, colorWhite);
			drawRect(myMat, m_rectRight, colorCyan, colorWhite);			
		}
	}
}
