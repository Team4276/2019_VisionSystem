package frc.robot;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraCalibration {
	private Mat cameraMatrix = null;
	private Mat distCoeffs = null;

	public void loadFromYamlFile(String path) {
//		
//		 camera_matrix: !!opencv-matrix
//		
//		   rows: 3
//		   cols: 3
//		   dt: d
//		   data: [ 2.9482783765726424e+02, 0., 3.1480862269626300e+02, 0.,
//		       2.9482783765726424e+02, 2.3886484081310755e+02, 0., 0., 1. ]
//		distortion_coefficients: !!opencv-matrix
//		   rows: 5
//		   cols: 1
//		   dt: d
//		   data: [ -2.7415242407561496e-01, 6.0732740115875483e-02, 0., 0.,
//		       -5.5934428233374665e-03 ]
		double[] dMatrix = {
				2.9482783765726424e+02, 0., 3.1480862269626300e+02, 0.,
			       2.9482783765726424e+02, 2.3886484081310755e+02, 0., 0., 1.
		};
		cameraMatrix = new Mat( 3, 3, CvType.CV_64FC1 );
		int row = 0, col = 0;
		cameraMatrix.put(row ,col, dMatrix);
		
		double dDist[] = {
				-2.7415242407561496e-01, 6.0732740115875483e-02, 0., 0.,
			       -5.5934428233374665e-03
		};
		distCoeffs = new Mat( 1, 5, CvType.CV_64FC1 );
		distCoeffs.put(row ,col, dDist);
	}
	
	public Mat getCameraMatrix() {
		if(cameraMatrix == null) {
			System.out.println(" cameraMatrix is null");
		}
		return cameraMatrix;
	}
	
	public Mat getDistCoeffs() {
		if(distCoeffs == null) {
			System.out.println(" distCoeffs is null");
		}
		return distCoeffs;
	}
}
