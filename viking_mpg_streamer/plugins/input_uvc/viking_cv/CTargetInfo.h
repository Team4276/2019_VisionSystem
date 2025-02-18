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


#ifndef CTARGETINFO_H
#define	CTARGETINFO_H

class CTargetInfo
{
public:
    CTargetInfo();
    CTargetInfo(const CTargetInfo& orig);
    virtual ~CTargetInfo();

    void init();

    std::string displayText() const
    {
        return m_targetInfoText;
    }
    void updateTargetInfo(
            bool isClosestObjectFound,
            double distanceToClosestObjectInches,
            int xPixelCenterOfClosestObject);
    
   void updateLatency(
           int timeLatencyThisCameraFrameMilliseconds);
 
    void initTargetInfoFromText(const std::string& targetInfoText);

    /* Not absolute time -- use for relative time between samples */
    int getTimeSinceLastCameraFrameMilliseconds(int idx)
    {
        return m_timeSinceLastCameraFrameMilliseconds[idx];
    }
    
    void setTimeSinceLastCameraFrameMilliseconds(int idx, int iMillisecs)
    {
        m_timeSinceLastCameraFrameMilliseconds[idx] = iMillisecs;
    }

    int getTimeLatencyThisCameraFrameMilliseconds()
    {
        return m_timeLatencyThisCameraFrameMilliseconds;
    }

    /* 0 == not found */
    int isClosestObjectFound() const
    {
        return m_isClosestObjectFound;
    }

    /* degrees, + == to the right */
    float xPixelCenterOfClosestObject() const
    {
        return m_xPixelCenterOfClosestObject;
    }

    /* feet from back of robot, should always be positive */
    float distanceToClosestObjectInches() const
    {
        return m_distanceToClosestObjectInches;
    }

    std::string initFormattedTextFromTargetInfo();

private:


    std::string m_targetInfoText;
    int m_timeSinceLastCameraFrameMilliseconds[2];
    int m_timeLatencyThisCameraFrameMilliseconds;
    bool m_isClosestObjectFound;
    double m_distanceToClosestObjectInches;
    int m_xPixelCenterOfClosestObject;
    int m_xPixelAvoidClosestObjectRightPath;
    int m_xPixelAvoidClosestObjectLeftPath;
 };

#endif	/* CTARGETINFO_H */

