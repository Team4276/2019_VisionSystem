/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frc.robot;

import java.util.Arrays;
import java.util.List;


public class JTargetInfo
{
    public Boolean isCargoBayDetected;
    public double visionPixelX;
    public int timeSinceLastCameraFrameMilliseconds;
    public int timeLatencyThisCameraFrameMilliseconds;

    int commaPos;
    String word;
    int num;
    double fnum;
    String s;

    public void JTargetInfo()
    {
        init();
    }

    public void init()
    {
        isCargoBayDetected =false;
        visionPixelX = 0.0;
        timeSinceLastCameraFrameMilliseconds = 0;
        timeLatencyThisCameraFrameMilliseconds = 0;
     
        commaPos = 0;
        word = "";
        num = 0;
        s = "";
        double fnum = 0.0;
    }

    public void initTargetInfoFromText(String str)
    {
        List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
        int idx = 0;
        isCargoBayDetected = (items.get(idx++) == "true");
        visionPixelX = Double.parseDouble(items.get(idx++));
        timeSinceLastCameraFrameMilliseconds = Integer.parseInt(items.get(idx++));
        timeLatencyThisCameraFrameMilliseconds = Integer.parseInt(items.get(idx++));
    }

    public String NumberToText()
    {
        String s = "false,";
        if(isCargoBayDetected) 
        {
            s = "true,";
        }
        s += Double.toString(visionPixelX) + ",";
        s += Integer.toString(timeSinceLastCameraFrameMilliseconds) + ",";
        s += Integer.toString(timeLatencyThisCameraFrameMilliseconds) + ",";
         return s;
    }

    public String displayText()
    {
        String str = "Time Since Last Frame: " + timeSinceLastCameraFrameMilliseconds + "ms.\n";
        str += "Latency This Frame: " + timeSinceLastCameraFrameMilliseconds + "ms.\n";
        if(isCargoBayDetected)
        {
            str += "X Pixel: " + visionPixelX + "\n";
        }    
        else
        {
            str += "No Cargo Bay Detected\n";
        }
         return str;
    }

    public void initFormattedTextFromTargetInfo()
    {
    //targetInfoText = Text;
        // TODO:  Assign values to target info data members by parsing the text string from initFormattedTextFromTargetInfo() 

        // Format text for transmission to the cRio
        initFormattedTextFromTargetInfo();
    }

}
