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


#ifndef CSETTING_H
#define	CSETTING_H

class CSetting
{
public:
    typedef enum
    {
        SETTING_TYPE_UNKNOWN = -1,
        SETTING_ENABLE_DYNAMIC_SETTINGS = 0, // ==0 init at startup only
        SETTING_EXPOSURE,
        SETTING_ENABLE_STREAM_FILTER_IMAGE, // != 0 show filtered blobs instead of annotated original
        SETTING_FILTER_HUE_LOWER_BOUND,
        SETTING_FILTER_HUE_UPPER_BOUND,
        NUMBER_OF_SETTINGS,
    } SETTING_TYPE;

    typedef struct
    {
        SETTING_TYPE settingType;
        const char* name;
        int defaultValue;
    } INIT_SETTINGS;

    void init(INIT_SETTINGS ini)
    {
        m_settingType = ini.settingType;
        m_defaultValue = ini.defaultValue;
        m_value = m_defaultValue;
        m_isValueChanged = true;  // Will get set at least once, even if default value
        if( (ini.name != NULL) && (*ini.name != 0) )
        {
            m_name = (char*) ini.name;
        }
    }
    
    SETTING_TYPE settingType() const {return m_settingType;}
    int value() const {return m_value;}
    std::string name() const {return m_name;}
    
    void setValue(int val)
    {
        if(val != m_value)
        {
            m_value = val;
            m_isValueChanged = true;
        }
    }
    
    bool isValueChanged() 
    {
        if(m_isValueChanged)
        {
            m_isValueChanged = false;
            return true;
        }
        return false;
    }
    
private:
    SETTING_TYPE m_settingType;
    std::string m_name;
    int m_defaultValue;
    int m_value;
    bool m_isValueChanged;
};

#endif	/* CSETTING_H */

