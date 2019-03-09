#!/bin/bash
cd /home/pi/2019_VisionSystem/raspi
rm -rf release
mkdir release
cd release 
cp /home/pi/2019_VisionSystem/raspi/camsvr/*.* /home/pi/2019_VisionSystem/raspi/release
