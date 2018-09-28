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


#include <string>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include "CSetting.h"
#include "CSettingList.h"

#define BASE_DIR "/home/pi/"

CSettingList g_settings;

CSettingList::CSettingList()
{
}

CSettingList::~CSettingList()
{
}

void CSettingList::init()
{
    char buf[128] = {0};
    const CSetting::INIT_SETTINGS initSettings[] = {
        {CSetting::SETTING_ENABLE_DYNAMIC_SETTINGS,    "enableDynamicSettings.txt",         0},
        {CSetting::SETTING_EXPOSURE,                   "exposureZeroTo2047.txt",          260},
        {CSetting::SETTING_ENABLE_STREAM_FILTER_IMAGE, "enableStreamFilterImage.txt",       0},
        {CSetting::SETTING_FILTER_HUE_LOWER_BOUND,     "filterHueLowerBoundZeroTo255.txt", 79},
        {CSetting::SETTING_FILTER_HUE_UPPER_BOUND,     "filterHueUpperBoundZeroTo255.txt", 96},
        {CSetting::SETTING_TYPE_UNKNOWN, "", 0},
    };
    const CSetting::INIT_SETTINGS* pInit = initSettings;
    int i = 0;
    while (pInit->settingType != CSetting::SETTING_TYPE_UNKNOWN)
    {
        m_settings[i++].init(*pInit);
        pInit++;
    }
    m_settings[i++].init(*pInit);
    
    i = 0;
    while(m_settings[i].settingType() != CSetting::SETTING_TYPE_UNKNOWN)
    {
        int iTemp = getValueFromFile(m_settings[i].settingType());
        if (iTemp != -1)
        {
            m_settings[i].setValue(iTemp);
        }
        if (!isSettingFileExist(m_settings[i].settingType()))
        {
            std::string sPath = BASE_DIR;
            sPath += m_settings[i].name();
            FILE* fp = fopen(sPath.c_str(), "w");
            if (fp == NULL)
            {
                printf("?? Can't create file");
            }
            else
            {
                sprintf(buf, "%d", m_settings[i].value());
                fwrite(buf, 1, strlen(buf), fp);
                fclose(fp);
                std::string sCmd = "chmod 777 ";
                sCmd += sPath;
                int rv = system(sCmd.c_str());
            }
        }
        i++;
    }
}
    
int CSettingList::value(CSetting::SETTING_TYPE typ) const
{
    return m_settings[typ].value();
}

bool CSettingList::isDynamicSettingsEnabled() const
{
    static int iCount = 0;
    if (m_settings[CSetting::SETTING_ENABLE_DYNAMIC_SETTINGS].value() != 0)
    {
        iCount++;
        if (iCount % 20)
        {
            return true;
        }
    }
    return false;
}
    
bool CSettingList::isValueChanged(CSetting::SETTING_TYPE typ)
{
    return m_settings[typ].isValueChanged();
}

std::string CSettingList::getSettingText(CSetting::SETTING_TYPE typ)
{
    char buf[128] = {0};
    sprintf(buf, "%d", getSetting(typ));
    return buf;
}

int CSettingList::getSetting(CSetting::SETTING_TYPE typ)
{
    if (typ == CSetting::SETTING_ENABLE_DYNAMIC_SETTINGS)
    {
        printf("?? SETTING_ENABLE_DYNAMIC_SETTINGS cannot be set dynamically");
    }
    else
    {
        m_settings[typ].setValue(getValueFromFile(typ));
    }
    return m_settings[typ].value();
}

bool CSettingList::isSettingFileExist(CSetting::SETTING_TYPE typ) const
{
    std::string sPath = BASE_DIR;
    sPath += m_settings[typ].name();
    return (access(sPath.c_str(), F_OK) != -1);
}

int CSettingList::getValueFromFile(CSetting::SETTING_TYPE typ)
{
    int iRet = -1;
    if (isSettingFileExist(typ))
    {
        std::string sPath = BASE_DIR;
        sPath += m_settings[typ].name();
        FILE* fp = fopen(sPath.c_str(), "r");
        if (fp == NULL)
        {
            printf("?? File exists but cannot open C(%d)", errno);
            fflush(NULL);
        }
        else
        {
            char buf[128] = {0};
            int nRead = fread(buf, 1, 128, fp);
            if (nRead > 0)
            {
                m_settings[typ].setValue(atoi(buf));
            }
            fclose(fp);
        }
    }
    return m_settings[typ].value();
}
