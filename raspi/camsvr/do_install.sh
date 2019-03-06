#!/bin/bash
cd /home/pi/2019_VisionSystem/raspi
rm -rf release
mkdir release
cd release 
cp -r /home/pi/2019_VisionSystem/raspi/camsvr /home/pi/2019_VisionSystem/raspi/release
dos2unix /home/pi/2019_VisionSystem/raspi/release/camsvr/*.sh
cd /home/pi/2019_VisionSystem/raspi/release/camsvr
ant clean
ant build

