
package frc.robot;


/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot  {
	
	public static int nSequenceVisionSystem;
	public static JTargetInfo visionTargetInfo;
	
	private static Thread visionThread;


    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public static void robotInit() {

     	
        nSequenceVisionSystem = 0;
        visionTargetInfo = new JTargetInfo();
        visionThread = new Thread(new JVisionSystemReceiverRunnable());
        visionThread.start();
        
        while(true) {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
    	
  
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
    	
    	
         
    }
    
    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
    
    }
    
}
