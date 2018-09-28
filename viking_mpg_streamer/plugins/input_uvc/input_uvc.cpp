/*******************************************************************************
# Linux-UVC streaming input-plugin for MJPG-streamer                           #
#                                                                              #
# This package work with the Logitech UVC based webcams with the mjpeg feature #
#                                                                              #
# Copyright (C) 2005 2006 Laurent Pinchart &&  Michel Xhaard                   #
#                    2007 Lucas van Staden                                     #
#                    2007 Tom Stöveken                                         #
#                                                                              #
# This program is free software; you can redistribute it and/or modify         #
# it under the terms of the GNU General Public License as published by         #
# the Free Software Foundation; either version 2 of the License, or            #
# (at your option) any later version.                                          #
#                                                                              #
# This program is distributed in the hope that it will be useful,              #
# but WITHOUT ANY WARRANTY; without even the implied warranty of               #
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                #
# GNU General Public License for more details.                                 #
#                                                                              #
# You should have received a copy of the GNU General Public License            #
# along with this program; if not, write to the Free Software                  #
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA    #
#                                                                              #
 *******************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <linux/videodev2.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <signal.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <getopt.h>
#include <pthread.h>
#include <syslog.h>

#include "../../utils.h"
//#include "../../mjpg_streamer.h"
#include "v4l2uvc.h" // this header will includes the ../../mjpg_streamer.h
#include "huffman.h"
#include "jpeg_utils.h"
#include "dynctrl.h"
//#include "uvcvideo.h"

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include "viking_cv/CUpperGoalRectangle.h"
#include "viking_cv/CTargetInfo.h"
#include "viking_cv/CVideoFrame.h"
#include "viking_cv/CVideoFrameQueue.h"
#include "viking_cv/CConnectionServer.h"
#include "viking_cv/CGpioLed.h"
#include "viking_cv/CUpperGoalDetector.h"
#include "viking_cv/CTestMonitor.h"
#include "viking_cv/CFrameGrinder.h"
#include "viking_cv/CSetting.h"
#include "viking_cv/CSettingList.h"
#include "viking_cv/dbgMsg.h"

CFrameGrinder frameGrinder;

// Global shutdown flag is set when user typed Ctrl-C
extern bool g_isShutdown;

extern CSettingList g_settings;
extern context cams[MAX_INPUT_PLUGINS];

#define INPUT_PLUGIN_NAME "UVC webcam grabber"
#define FILE_NAME_EXPOSURE "/home/pi/exposure3To2047.txt"

/*
 * UVC resolutions mentioned at: (at least for some webcams)
 * http://www.quickcamteam.net/hcl/frame-format-matrix/
 */
static const struct {
    const char *string;
    const int width, height;
} resolutions[] = {
    { "QSIF", 160, 120},
    { "QCIF", 176, 144},
    { "CGA", 320, 200},
    { "QVGA", 320, 240},
    { "CIF", 352, 288},
    { "VGA", 640, 480},
    { "SVGA", 800, 600},
    { "XGA", 1024, 768},
    { "SXGA", 1280, 1024},
    { "HD720", 1280, 720}
};

/* private functions and variables to this plugin */
globals *pglobal;
static int gquality = 80;
static unsigned int minimum_size = 0;
static int dynctrls = 1;

/*** private functions for this plugin below ***/

/******************************************************************************
Description.: print a help message to stderr
Input Value.: -
Return Value: -
 ******************************************************************************/
void help(void) {
    int i;

    fprintf(stderr, " ---------------------------------------------------------------\n" \
    " Help for input plugin..: "INPUT_PLUGIN_NAME"\n" \
    " ---------------------------------------------------------------\n" \
    " The following parameters can be passed to this plugin:\n\n" \
    " [-d | --device ].......: video device to open (your camera)\n" \
    " [-r | --resolution ]...: the resolution of the video device,\n" \
    "                          can be one of the following strings:\n" \
    "                          ");

    for (i = 0; i < LENGTH_OF(resolutions); i++) {
        fprintf(stderr, "%s ", resolutions[i].string);
        if ((i + 1) % 6 == 0)
            fprintf(stderr, "\n                          ");
    }
    fprintf(stderr, "\n                          or a custom value like the following" \
    "\n                          example: 640x480\n");

    fprintf(stderr, " [-f | --fps ]..........: frames per second\n" \
    " [-y | --yuv ]..........: enable YUYV format and disable MJPEG mode\n" \
    " [-q | --quality ]......: JPEG compression quality in percent \n" \
    "                          (activates YUYV format, disables MJPEG)\n" \
    " [-m | --minimum_size ].: drop frames smaller then this limit, useful\n" \
    "                          if the webcam produces small-sized garbage frames\n" \
    "                          may happen under low light conditions\n" \
    " [-n | --no_dynctrl ]...: do not initalize dynctrls of Linux-UVC driver\n" \
    " [-l | --led ]..........: switch the LED \"on\", \"off\", let it \"blink\" or leave\n" \
    "                          it up to the driver using the value \"auto\"\n" \
    " ---------------------------------------------------------------\n\n");
}

/*** plugin interface functions ***/
/******************************************************************************
Description.: This function ializes the plugin. It parses the commandline-
              parameter and stores the default and parsed values in the
              appropriate variables.
Input Value.: param contains among others the command-line string
Return Value: 0 if everything is fine
              1 if "--help" was triggered, in this case the calling programm
              should stop running and leave.
 ******************************************************************************/
extern "C" {

    int input_init(input_parameter *param, int id) {
        char *dev = (char*) "/dev/video0";
        char *s = NULL;
        int width = 640, height = 480, fps = 5, format = V4L2_PIX_FMT_MJPEG, i;
        /* initialize the mutes variable */
        if (pthread_mutex_init(&cams[id].controls_mutex, NULL) != 0) {
            IPRINT("could not initialize mutex variable\n");
            exit(EXIT_FAILURE);
        }

        param->argv[0] = (char*) INPUT_PLUGIN_NAME;

        /* show all parameters for DBG purposes */
        for (i = 0; i < param->argc; i++) {
            DBG("argv[%d]=%s\n", i, param->argv[i]);
        }

        /* parse the parameters */
        reset_getopt();
        while (1) {
            int option_index = 0, c = 0;
            static struct option long_options[] = {
                {"h", no_argument, 0, 0},
                {"help", no_argument, 0, 0},
                {"d", required_argument, 0, 0},
                {"device", required_argument, 0, 0},
                {"r", required_argument, 0, 0},
                {"resolution", required_argument, 0, 0},
                {"f", required_argument, 0, 0},
                {"fps", required_argument, 0, 0},
                {"y", no_argument, 0, 0},
                {"yuv", no_argument, 0, 0},
                {"q", required_argument, 0, 0},
                {"quality", required_argument, 0, 0},
                {"m", required_argument, 0, 0},
                {"minimum_size", required_argument, 0, 0},
                {"n", no_argument, 0, 0},
                {"no_dynctrl", no_argument, 0, 0},
                {"l", required_argument, 0, 0},
                {"led", required_argument, 0, 0},
                {0, 0, 0, 0}
            };

            /* parsing all parameters according to the list above is sufficent */
            c = getopt_long_only(param->argc, param->argv, "", long_options, &option_index);

            /* no more options to parse */
            if (c == -1) break;

            /* unrecognized option */
            if (c == '?') {
                help();
                return 1;
            }

            /* dispatch the given options */
            switch (option_index) {
                    /* h, help */
                case 0:
                case 1:
                    DBG("case 0,1\n");
                    help();
                    return 1;
                    break;

                    /* d, device */
                case 2:
                case 3:
                    DBG("case 2,3\n");
                    dev = strdup(optarg);
                    break;

                    /* r, resolution */
                case 4:
                case 5:
                    DBG("case 4,5\n");
                    width = -1;
                    height = -1;

                    /* try to find the resolution in lookup table "resolutions" */
                    for (i = 0; i < LENGTH_OF(resolutions); i++) {
                        if (strcmp(resolutions[i].string, optarg) == 0) {
                            width = resolutions[i].width;
                            height = resolutions[i].height;
                        }
                    }
                    /* done if width and height were set */
                    if (width != -1 && height != -1)
                        break;
                    /* parse value as decimal value */
                    width = strtol(optarg, &s, 10);
                    height = strtol(s + 1, NULL, 10);
                    break;

                    /* f, fps */
                case 6:
                case 7:
                    DBG("case 6,7\n");
                    fps = atoi(optarg);
                    break;

                    /* y, yuv */
                case 8:
                case 9:
                    DBG("case 8,9\n");
                    format = V4L2_PIX_FMT_YUYV;
                    break;

                    /* q, quality */
                case 10:
                case 11:
                    DBG("case 10,11\n");
                    format = V4L2_PIX_FMT_YUYV;
                    gquality = MIN(MAX(atoi(optarg), 0), 100);
                    break;

                    /* m, minimum_size */
                case 12:
                case 13:
                    DBG("case 12,13\n");
                    minimum_size = MAX(atoi(optarg), 0);
                    break;

                    /* n, no_dynctrl */
                case 14:
                case 15:
                    DBG("case 14,15\n");
                    dynctrls = 0;
                    break;

                    /* l, led */
                case 16:
                case 17:/*
        DBG("case 16,17\n");
        if ( strcmp("on", optarg) == 0 ) {
          led = IN_CMD_LED_ON;
        } else if ( strcmp("off", optarg) == 0 ) {
          led = IN_CMD_LED_OFF;
        } else if ( strcmp("auto", optarg) == 0 ) {
          led = IN_CMD_LED_AUTO;
        } else if ( strcmp("blink", optarg) == 0 ) {
          led = IN_CMD_LED_BLINK;
        }*/
                    break;

                default:
                    DBG("default case\n");
                    help();
                    return 1;
            }
        }
        DBG("input id: %d\n", id);
        cams[id].id = id;
        cams[id].pglobal = param->global;

        /* allocate webcam datastructure */
        cams[id].videoIn = (vdIn*) malloc(sizeof (struct vdIn));
        if (cams[id].videoIn == NULL) {
            IPRINT("not enough memory for videoIn\n");
            exit(EXIT_FAILURE);
        }
        memset(cams[id].videoIn, 0, sizeof (struct vdIn));

        /* display the parsed values */
        IPRINT("Using V4L2 device.: %s\n", dev);
        IPRINT("Desired Resolution: %i x %i\n", width, height);
        IPRINT("Frames Per Second.: %i\n", fps);
        IPRINT("Format............: %s\n", (format == V4L2_PIX_FMT_YUYV) ? "YUV" : "MJPEG");
        if (format == V4L2_PIX_FMT_YUYV)
            IPRINT("JPEG Quality......: %d\n", gquality);

#ifndef TEST_USE_JPEGS_NOT_CAMERA


        DBG("vdIn pn: %d\n", id);
        /* open video device and prepare data structure */
        if (init_videoIn(cams[id].videoIn, dev, width, height, fps, format, 1, cams[id].pglobal, id) < 0) {
            IPRINT("init_VideoIn failed\n");
            closelog();
            exit(EXIT_FAILURE);
        }
        /*
         * recent linux-uvc driver (revision > ~#125) requires to use dynctrls
         * for pan/tilt/focus/...
         * dynctrls must get initialized
         */
        if (dynctrls)
            initDynCtrls(cams[id].videoIn->fd);

        enumerateControls(cams[id].videoIn, cams[id].pglobal, id); // enumerate V4L2 controls after UVC extended mapping

#endif   // TEST_USE_JPEGS_NOT_CAMERA
        return 0;
    }

    /******************************************************************************
    Description.: Stops the execution of worker thread
    Input Value.: -
    Return Value: always 0
     ******************************************************************************/
    int input_stop(int id) {
        DBG("will cancel camera thread #%02d\n", id);
        pthread_cancel(cams[id].threadID);
        return 0;
    }

    /******************************************************************************
    Description.:
    Input Value.:
    Return Value:
     ******************************************************************************/
    void cam_cleanup(void *arg) {
        static unsigned char first_run = 1;
        context *pcontext = (context*) arg;
        pglobal = pcontext->pglobal;
        if (!first_run) {
            DBG("already cleaned up ressources\n");
            return;
        }

        first_run = 0;
        IPRINT("cleaning up ressources allocated by input thread\n");

        close_v4l2(pcontext->videoIn);
        if (pcontext->videoIn->tmpbuffer != NULL) free(pcontext->videoIn->tmpbuffer);
        if (pcontext->videoIn != NULL) free(pcontext->videoIn);
        if (pglobal->in[pcontext->id].buf != NULL)
            free(pglobal->in[pcontext->id].buf);
    }
    
    void setCameraExposure()
    {
        int rv = system("v4l2-ctl -d /dev/video0 --set-ctrl exposure_auto=1");
        usleep(1000000);
                
        char buf2[128] = {0};
        strcpy(buf2, "v4l2-ctl -d /dev/video0 --set-ctrl exposure_absolute=");
        strcat(buf2, g_settings.getSettingText(CSetting::SETTING_EXPOSURE).c_str());
        int rv2 = system(buf2);
        usleep(1000000);
        if (rv2 != 0) 
        {
            printf("rv2 = %d,  %s\n", rv2, buf2);
        }
    }

    /******************************************************************************
    Description.: this thread worker grabs a frame and copies it to the global buffer
    Input Value.: unused
    Return Value: unused, always NULL
     ******************************************************************************/
    void *cam_thread(void *arg) {
        
        g_settings.init();
        setCameraExposure();
        
        CVideoFrame* pFrame = NULL;

#ifndef TEST_USE_JPEGS_NOT_CAMERA 
        int width = VIEW_PIXEL_X_WIDTH;
        int height = VIEW_PIXEL_Y_HEIGHT;
        IplImage * img = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, 3); // obraz OpenCV
#endif

        frameGrinder.init();

#ifdef TEST_USE_JPEGS_NOT_CAMERA 
        std::string sBasePath = "/home/";
        sBasePath += HOME_NAME;
        std::string sPath = sBasePath;
        sPath += "/0243-20150125-22-21-46.jpg";
        //sPath += "/0007-20150125-22-36-25.jpg";  
        cv::Mat frame1 = cv::imread(sPath.c_str(), CV_LOAD_IMAGE_COLOR);
        if (frame1.empty()) {
            dbgMsg_s("Failed to read image data from a file1\n");
        }

        sPath = sBasePath;
        sPath += "/0243-20150125-22-21-46.jpg";
        //sPath += "/0007-20150125-22-36-25.jpg";  
        cv::Mat frame2 = cv::imread(sPath.c_str(), CV_LOAD_IMAGE_COLOR);
        if (frame2.empty()) {
            dbgMsg_s("Failed to read image data from a file2\n");
        }
        bool toggle = false;
#endif

        context *pcontext = (context*) arg;
        pglobal = pcontext->pglobal;

        /* set cleanup handler to cleanup allocated ressources */
        pthread_cleanup_push(cam_cleanup, pcontext);

        while (!pglobal->stop) {
            while (pcontext->videoIn->streamingState == STREAMING_PAUSED) {
                usleep(1); // maybe not the best way so FIXME
            }

#ifdef TEST_USE_JPEGS_NOT_CAMERA 
            if (frameGrinder.safeGetFreeFrame(&pFrame)) {
                if (toggle) {
                    pFrame->m_frame = frame1;
                } else {
                    pFrame->m_frame = frame2;
                }
                toggle = (!toggle);
                if (!pFrame->m_frame.empty()) {
                    frameGrinder.safeAddTail(pFrame, CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT);
                } else {
                    dbgMsg_s("Frame is empty\n");
                    frameGrinder.safeAddTail(pFrame, CVideoFrame::FRAME_QUEUE_FREE);
                }
                frameGrinder.m_testMonitor.m_nTasksDone[CTestMonitor::TASK_DONE_CAMERA]++;
            }

#else
            /* grab a frame */
            if (uvcGrab(pcontext->videoIn) < 0) {
                IPRINT("Error grabbing frames\n");
                exit(EXIT_FAILURE);
            }

            DBG("received frame of size: %d from plugin: %d\n", pcontext->videoIn->buf.bytesused, pcontext->id);

            /*
             * Workaround for broken, corrupted frames:
             * Under low light conditions corrupted frames may get captured.
             * The good thing is such frames are quite small compared to the regular pictures.
             * For example a VGA (640x480) webcam picture is normally >= 8kByte large,
             * corrupted frames are smaller.
             */
            if (pcontext->videoIn->buf.bytesused < minimum_size) {
                DBG("dropping too small frame, assuming it as broken\n");
                continue;
            }

            if (g_settings.isDynamicSettingsEnabled())
            {
                g_settings.getValueFromFile(CSetting::SETTING_EXPOSURE);
            }
            if(g_settings.isValueChanged(CSetting::SETTING_EXPOSURE))
            {
                setCameraExposure();
            }

#ifdef NO_CV_JUST_STREAM_THE_CAMERA

            /* copy JPG picture to global buffer */
            pthread_mutex_lock(&pglobal->in[pcontext->id].db);

            /*
             * If capturing in YUV mode convert to JPEG now.
             * This compression requires many CPU cycles, so try to avoid YUV format.
             * Getting JPEGs straight from the webcam, is one of the major advantages of
             * Linux-UVC compatible devices.
             */
            if (pcontext->videoIn->formatIn == V4L2_PIX_FMT_YUYV) {
                DBG("compressing frame from input: %d\n", (int) pcontext->id);
                pglobal->in[pcontext->id].size = compress_yuyv_to_jpeg(pcontext->videoIn, pglobal->in[pcontext->id].buf, pcontext->videoIn->framesizeIn, gquality);
            } else {
                DBG("copying frame from input: %d\n", (int) pcontext->id);
                pglobal->in[pcontext->id].size = memcpy_picture(pglobal->in[pcontext->id].buf, pcontext->videoIn->tmpbuffer, pcontext->videoIn->buf.bytesused);
            }

            /* copy this frame's timestamp to user space */
            pglobal->in[pcontext->id].timestamp = pcontext->videoIn->buf.timestamp;

            /* signal fresh_frame */
            pthread_cond_broadcast(&pglobal->in[pcontext->id].db_update);
            pthread_mutex_unlock(&pglobal->in[pcontext->id].db);


#else // #ifndef NO_CV_JUST_STREAM_THE_CAMERA

            if (frameGrinder.safeGetFreeFrame(&pFrame)) {
                std::vector<uchar> vectordata(pcontext->videoIn->tmpbuffer, pcontext->videoIn->tmpbuffer + (height * width));
                cv::Mat data_mat(vectordata, false);
                cv::Mat image(cv::imdecode(data_mat, 1)); //put 0 if you want greyscale
                pFrame->m_frame = image;
                if (!pFrame->m_frame.empty()) {
                    frameGrinder.safeAddTail(pFrame, CVideoFrame::FRAME_QUEUE_WAIT_FOR_BLOB_DETECT);
                } else {
                    dbgMsg_s("Frame is empty\n");
                    frameGrinder.safeAddTail(pFrame, CVideoFrame::FRAME_QUEUE_FREE);
                }
                frameGrinder.m_testMonitor.m_nTasksDone[CTestMonitor::TASK_DONE_CAMERA]++;
            }

#endif  // #ifndef NO_CV_JUST_STREAM_THE_CAMERA

#endif   // TEST_USE_JPEGS_NOT_CAMERA
        }

        DBG("leaving input thread, calling cleanup function now\n");
        pthread_cleanup_pop(1);

        return NULL;
    }

    /******************************************************************************
    Description.: spins of a worker thread
    Input Value.: -
    Return Value: always 0
     ******************************************************************************/
    int input_run(int id) {
        cams[id].pglobal->in[id].buf = (unsigned char*) malloc(cams[id].videoIn->framesizeIn);
        if (cams[id].pglobal->in[id].buf == NULL) {
            fprintf(stderr, "could not allocate memory\n");
            exit(EXIT_FAILURE);
        }

        DBG("launching camera thread #%02d\n", id);
        /* create thread and pass context to thread function */
        pthread_create(&(cams[id].threadID), NULL, cam_thread, &(cams[id]));
        pthread_detach(cams[id].threadID);
        return 0;
    }

    void RGB2IplImage(IplImage *img,
            const unsigned char* rgbArray,
            unsigned int width,
            unsigned int height) {
        int i, j;
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                ((uchar *) (img->imageData + i * img->widthStep))[j * img->nChannels + 0] =
                        *(rgbArray + i * 176 * 3 + j * 3 + 0);
                ((uchar *) (img->imageData + i * img->widthStep))[j * img->nChannels + 1] =
                        *(rgbArray + i * 176 * 3 + j * 3 + 1);
                ((uchar *) (img->imageData + i * img->widthStep))[j * img->nChannels + 2] =
                        *(rgbArray + i * 176 * 3 + j * 3 + 2);
            }
        }
    }
    
};

