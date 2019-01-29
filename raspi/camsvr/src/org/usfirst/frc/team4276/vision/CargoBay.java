package org.usfirst.frc.team4276.vision;

import org.opencv.core.RotatedRect;

public class CargoBay {
	RotatedRect m_rectLeft;
	RotatedRect m_rectRight;
	
	CargoBay(RotatedRect lft, RotatedRect rt) {
		m_rectLeft = lft;
		m_rectRight = rt;
	}
	
}
