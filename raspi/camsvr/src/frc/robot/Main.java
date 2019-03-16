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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Main {

	public static JVideoFrameQueue myFrameQueue_FREE = null;
	public static JVideoFrameQueue myFrameQueue_WAIT_FOR_BLOB_DETECT = null;
	public static JVideoFrameQueue myFrameQueue_WAIT_FOR_TEXT_CLIENT = null;
	public static JVideoFrameQueue myFrameQueue_WAIT_FOR_BROWSER_CLIENT = null;

	public static TestMonitor m_testMonitor = null;

	private static Boolean useSingleJpegInseadOfCamera = false;
	static final int FRAME_WIDTH = 640;
	static final int FRAME_HEIGHT = 480;
	static final int FRAME_CENTER_PIXEL_X = FRAME_WIDTH / 2;
	static final int IGNORE_ABOVE_THIS_Y_PIXEL = (int)(0.15*FRAME_HEIGHT);

	private static final int MAX_FRAMES = 32;

	public static boolean isShuttingDown = false;
	private static Thread m_gripThread = null;
	private static Thread m_textThread = null;
	private static Thread m_streamThread = null;
	private static long timeLastFrame = 0;

	private static int nSequence = 0;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.exit(0);
			}
		});

		// Loads our OpenCV library. This MUST be included
		System.out.println("java.libary.path = "
				+ System.getProperty("java.library.path"));
		System.loadLibrary("opencv");

		m_testMonitor = new TestMonitor();
		
		myFrameQueue_FREE = new JVideoFrameQueue(JFrameQueueType.FREE);
		myFrameQueue_WAIT_FOR_BLOB_DETECT = new JVideoFrameQueue(
				JFrameQueueType.WAIT_FOR_BLOB_DETECT);
		myFrameQueue_WAIT_FOR_TEXT_CLIENT = new JVideoFrameQueue(
				JFrameQueueType.WAIT_FOR_TEXT_CLIENT);
		myFrameQueue_WAIT_FOR_BROWSER_CLIENT = new JVideoFrameQueue(
				JFrameQueueType.WAIT_FOR_BROWSER_CLIENT);

		for (int i = 0; i < MAX_FRAMES; i++) {
			myFrameQueue_FREE.addTail(new JVideoFrame());
		}

		m_gripThread = new Thread(new QGripThreadRunnable());
		m_gripThread.start();

		m_textThread = new Thread(new QTextThreadRunnable());
		m_textThread.start();

		m_streamThread = new Thread(new QStreamThreadRunnable());
		m_streamThread.start();
		
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                try {
                	isShuttingDown = true;
                	
                	m_streamThread.interrupt();
                	m_textThread.interrupt();
                	m_gripThread.interrupt();
                 	
                	m_streamThread.join();
                	m_textThread.join();
                	m_gripThread.join();
                }
                catch (InterruptedException e) {
                }
            }
        });



		// Connect NetworkTables, and get access to the publishing table
		NetworkTable.setClientMode();
		// Set your team number here
		NetworkTable.setTeam(4276);

		// RoboRIO required for testing with the following line uncommented
		// NetworkTable.initialize();

		// This stores our reference to our mjpeg server for streaming the input
		// image
		MjpegServer inputStream = new MjpegServer("MJPEG Server",
				JTargetInfo.streamSourcePortOnRaspberryPi);

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
			camera.setFPS(30);
			camera.setExposureManual(25);

			// This creates a CvSink for us to use. This grabs images from our
			// selected
			// camera,
			// and will allow us to use those images in opencv
			imageSink = new CvSink("CV Image Grabber");
			imageSink.setSource(camera);
		}

		// Infinitely process image
		int type = CvType.CV_8UC3;
		Mat inputImage = new Mat(FRAME_HEIGHT, FRAME_WIDTH, type);
		while (!isShuttingDown) {
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

			JVideoFrame frm = myFrameQueue_FREE.removeHead();
			if (frm == null) {
				// 'No free frames'
				// Try to steal an old packet from one of the active queues
				if (myFrameQueue_WAIT_FOR_BLOB_DETECT.size() > 1) {
					frm = myFrameQueue_WAIT_FOR_BLOB_DETECT.removeHead();
					if (frm != null) {
						myFrameQueue_WAIT_FOR_BLOB_DETECT.m_droppedFrames++;
					} else {
						if (myFrameQueue_WAIT_FOR_TEXT_CLIENT.size() > 1) {
							frm = myFrameQueue_WAIT_FOR_TEXT_CLIENT
									.removeHead();
							if (frm != null) {
								myFrameQueue_WAIT_FOR_TEXT_CLIENT.m_droppedFrames++;
							} else {
								if (myFrameQueue_WAIT_FOR_BROWSER_CLIENT.size() > 1) {
									frm = myFrameQueue_WAIT_FOR_BROWSER_CLIENT
											.removeHead();
									if (frm != null) {
										myFrameQueue_WAIT_FOR_BROWSER_CLIENT.m_droppedFrames++;
									}
								}
							}
						}
					}
				}
			}
			if (frm == null) {
				System.out.printf("* RESOURCE LEAK -- no packets avalable\n");
				continue;
			}

			frm.m_targetInfo.init();
			frm.m_targetInfo.nSequence = Main.nSequence++;

			if (0 == (frm.m_targetInfo.nSequence % 50)) {
				m_testMonitor.displayQueueLengths();
			}

			frm.m_frame = inputImage;
			if(timeLastFrame != 0)
			{
				frm.m_targetInfo.timeSinceLastCameraFrameMilliseconds = (System.nanoTime() - timeLastFrame)/1000000;
			}
			timeLastFrame = System.nanoTime();
			myFrameQueue_WAIT_FOR_BLOB_DETECT.addTail(frm);
		}
	}

	/*
	 * private static HttpCamera setHttpCamera(String cameraName, MjpegServer
	 * server) { // Start by grabbing the camera from NetworkTables NetworkTable
	 * publishingTable = NetworkTable.getTable("CameraPublisher"); // Wait for robot
	 * to connect. Allow this to be attempted indefinitely while (true) { try { if
	 * (publishingTable.getSubTables().size() > 0) { break; } Thread.sleep(500); }
	 * catch (Exception e) { // TODO Auto-generated catch block e.printStackTrace();
	 * } }
	 * 
	 * HttpCamera camera = null; if (!publishingTable.containsSubTable(cameraName))
	 * { return null; } ITable cameraTable =
	 * publishingTable.getSubTable(cameraName); String[] urls =
	 * cameraTable.getStringArray("streams", null); if (urls == null) { return null;
	 * } ArrayList<String> fixedUrls = new ArrayList<String>(); for (String url :
	 * urls) { if (url.startsWith("mjpg")) { fixedUrls.add(url.split(":", 2)[1]); }
	 * } camera = new HttpCamera("CoprocessorCamera", fixedUrls.toArray(new
	 * String[0])); server.setSource(camera); return camera; }
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