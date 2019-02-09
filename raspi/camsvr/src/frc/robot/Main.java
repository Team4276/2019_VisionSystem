package frc.robot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Main {

	private static Boolean useSingleJpegInseadOfCamera = false;
	private static final int FRAME_WIDTH = 640;
	private static final int FRAME_HEIGHT = 480;

	private static CameraCalibration cam1Calibration = new CameraCalibration();

	private final static Object imgLock = new Object();
	private static GripPipeline myGripPipeline = null;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		// Loads our OpenCV library. This MUST be included
		System.out.println("java.libary.path = "
				+ System.getProperty("java.library.path"));
		System.loadLibrary("opencv");

		myGripPipeline = new GripPipeline();

		// Connect NetworkTables, and get access to the publishing table
		NetworkTable.setClientMode();
		// Set your team number here
		NetworkTable.setTeam(4276);

		// RoboRIO required for testing with the following line uncommented
		// NetworkTable.initialize();

		// This is the network port you want to stream the raw received image to
		// By rules, this has to be between 1180 and 1190, so 1185 is a good
		// choice
		int streamPort = 1185;

		// This stores our reference to our mjpeg server for streaming the input
		// image
		MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

		// Selecting a Camera
		// Uncomment one of the 2 following camera options
		// The top one receives a stream from another device, and performs
		// operations
		// based on that
		// On windows, this one must be used since USB is not supported
		// The bottom one opens a USB camera, and performs operations on that,
		// along
		// with streaming
		// the input image so other devices can see it.

		// HTTP Camera
		/*
		 * // This is our camera name from the robot. this can be set in your
		 * robot code with the following command //
		 * CameraServer.getInstance().startAutomaticCapture
		 * ("YourCameraNameHere"); // "USB Camera 0" is the default if no string
		 * is specified String cameraName = "USB Camera 0"; HttpCamera camera =
		 * setHttpCamera(cameraName, inputStream); // It is possible for the
		 * camera to be null. If it is, that means no camera could // be found
		 * using NetworkTables to connect to. Create an HttpCamera by giving a
		 * specified stream // Note if this happens, no restream will be created
		 * if (camera == null) { camera = new HttpCamera("CoprocessorCamera",
		 * "YourURLHere"); inputStream.setSource(camera); }
		 */

		/***********************************************/

		UsbCamera camera;
		CvSink imageSink = null;
		if (!useSingleJpegInseadOfCamera) {
			camera = setUsbCamera(0, inputStream);

			// USB Camera
			// This gets the image from a USB camera
			// Usually this will be on device 0, but there are other overloads
			// that can be used
			// Set the resolution for our camera, since this is over USB
			camera.setResolution(640, 480);

			// This creates a CvSink for us to use. This grabs images from our
			// selected
			// camera,
			// and will allow us to use those images in opencv
			imageSink = new CvSink("CV Image Grabber");
			imageSink.setSource(camera);
		}

		// This creates a CvSource to use. This will take in a Mat image that
		// has had
		// OpenCV operations
		// operations
		CvSource imageSource = new CvSource("CV Image Source",
				VideoMode.PixelFormat.kMJPEG, FRAME_WIDTH, FRAME_HEIGHT, 30);
		MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
		cvStream.setSource(imageSource);

		// All Mats and Lists should be stored outside the loop to avoid
		// allocations
		// as they are expensive to create
		int rows = FRAME_HEIGHT;
		int cols = FRAME_WIDTH;
		int type = CvType.CV_8UC3;

		double[] dMatrix = {
				2.9482783765726424e+02, 0., 3.1480862269626300e+02, 0.,
			       2.9482783765726424e+02, 2.3886484081310755e+02, 0., 0., 1.
		};
		Mat cameraMatrix = new Mat( 3, 3, CvType.CV_64FC1 );
		int row = 0, col = 0;
		cameraMatrix.put(row ,col, dMatrix);
		
		double dDist[] = {
				-2.7415242407561496e-01, 6.0732740115875483e-02, 0., 0.,
			       -5.5934428233374665e-03
		};
		Mat distCoeffs = new Mat( 1, 5, CvType.CV_64FC1 );
		distCoeffs.put(row ,col, dDist);

		// Infinitely process image
		int iCount = 0;
		while (true) {
			Mat inputImage = new Mat(rows, cols, type);
			Mat undistortedImage = new Mat();
			if (useSingleJpegInseadOfCamera) {
				inputImage = Imgcodecs
						.imread("/home/pi/cam170_rocket_00deg_06ft_edit.JPG");
				// inputImage = Imgcodecs.imwrite("/home/pi/t.JPG", inputImage);
			} else {
				// Grab a frame. If it has a frame time of 0, there was an
				// error.
				// Just skip and continue
				long frameTime = imageSink.grabFrame(inputImage);
				if (frameTime == 0) {
					continue;
				}
			}

			Imgproc.undistort(inputImage, undistortedImage, cameraMatrix, distCoeffs);

			myGripPipeline.process(inputImage);
			ArrayList<MatOfPoint> contours = myGripPipeline
					.findContoursOutput();
			if (contours.isEmpty()) {
				imageSource.putFrame(inputImage);
			} else {
				Rect rectLargest = new Rect();
				double largestArea = 0.0;
				int idxLargestContour = 0;
				for (int i = 0; i < contours.size(); i++) {
					Rect rectContour = Imgproc.boundingRect(contours.get(i));
					double area = rectContour.width * rectContour.height;
					if (largestArea < area) {
						largestArea = area;
						rectLargest = rectContour;
						idxLargestContour = i;
					}
				}
				double centerX = rectLargest.x + (rectLargest.width / 2);
				double centerY = rectLargest.y + (rectLargest.height / 2);
				float radius = Math.min(rectLargest.width, rectLargest.height);
				Scalar colorBlue = new Scalar(0, 0, 255);
				Scalar colorRed = new Scalar(255, 0, 0);
				synchronized (imgLock) {
					Point pt1 = new Point(centerX - radius, centerY);
					Point pt2 = new Point(centerX + radius, centerY);
					Imgproc.line(inputImage, pt1, pt2, colorBlue);
					Point pt3 = new Point(centerX, centerY - radius);
					Point pt4 = new Point(centerX, centerY + radius);
					Imgproc.line(inputImage, pt3, pt4, colorBlue);
					Imgproc.drawContours(inputImage, contours,
							idxLargestContour, colorRed);
				}
				//imageSource.putFrame(inputImage);
				imageSource.putFrame(undistortedImage);
				iCount++;
			}
		}
	}

	/*
	 * private static HttpCamera setHttpCamera(String cameraName, MjpegServer
	 * server) { // Start by grabbing the camera from NetworkTables NetworkTable
	 * publishingTable = NetworkTable.getTable("CameraPublisher"); // Wait for
	 * robot to connect. Allow this to be attempted indefinitely while (true) {
	 * try { if (publishingTable.getSubTables().size() > 0) { break; }
	 * Thread.sleep(500); } catch (Exception e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } }
	 * 
	 * HttpCamera camera = null; if
	 * (!publishingTable.containsSubTable(cameraName)) { return null; } ITable
	 * cameraTable = publishingTable.getSubTable(cameraName); String[] urls =
	 * cameraTable.getStringArray("streams", null); if (urls == null) { return
	 * null; } ArrayList<String> fixedUrls = new ArrayList<String>(); for
	 * (String url : urls) { if (url.startsWith("mjpg")) {
	 * fixedUrls.add(url.split(":", 2)[1]); } } camera = new
	 * HttpCamera("CoprocessorCamera", fixedUrls.toArray(new String[0]));
	 * server.setSource(camera); return camera; }
	 */

	private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
		// This gets the image from a USB camera
		// Usually this will be on device 0, but there are other overloads
		// that can be used
		UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
		server.setSource(camera);
		return camera;
	}
}