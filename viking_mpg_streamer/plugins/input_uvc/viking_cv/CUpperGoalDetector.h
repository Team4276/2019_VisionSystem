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

#ifndef CUPPERGOALDETECTOR_H
#define	CUPPERGOALDETECTOR_H

//#define DISPLAY_CALIBRATION_INFO
#define DETECT_LARGEST_BLOB_NO_FILTER_BASED_ON_SIZE

#define VIEW_PIXEL_Y_WIDTH 240
#define VIEW_PIXEL_X_HEIGHT 424

class CUpperGoalDetector
{
public:

    typedef struct
    {
        float pixelRadius;
        float m_distanceToUpperGoalFeet;
        float angleToUpperGoalDegrees;
    } RADIUS_TABLE_ITEM;

    CUpperGoalDetector();
    CUpperGoalDetector(const CUpperGoalDetector& orig);
    virtual ~CUpperGoalDetector();

    void init();
    void detectBlobs(CVideoFrame* pFrame, CFrameGrinder* pFrameGrinder);
    double estimateDistanceInches(const CUpperGoalRectangle& goalRect);
    double estimateAzimuthAngle(double distance, double pixelX);

private:

    float m_tolerancePercentForRadius;
    //CGpioLed m_gpioLed;

private:
    bool filterContours(
            const std::vector<std::vector<cv::Point> >& listContours,
            int originalMatHeight, int originalMatWidth,
            CUpperGoalRectangle& upperGoalRectangle,
            float& upperGoalAzimuthDegrees,
            float& distanceToUpperGoalInches);
    
    double normalize360(double angle);
};

#endif	/* CUPPERGOALDETECTOR_H */

