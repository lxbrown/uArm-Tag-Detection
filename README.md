# uArm-Tag-Detection
Utilizing the april library created at the University of Michigan and the open source uArm Client created by the makers of the uArm, control the uArm from a linux computer and have it automatically detect and retrieve tagged boxes. It can also send and recieve commands from an Android device. The Android device can control the uArm or tell the uArm to pick up a box when one is detected.

TagDetection is the java program to be run on a linux machine and AndroidArmGUI is the Android application.

This program uses open source content which can be downloaded at the following links from their original creators:

april library - http://april.eecs.umich.edu/
(detection of the the tagged boxes)

uArm Client - https://github.com/UFactory-Team/uArm_Client
(used for original applet layout and control of the arm using sliders)

MJPEGParser - https://github.com/vanevery/Processing-Read-Axis-Camera-MJPEG/blob/master/MJPEGParser.pde
(reading an image in from a MJPEG stream)

Arduino - https://atulmaharaj.wordpress.com/2013/05/03/arduino-serial-communication-using-java-and-rxtx/
(serial port communication between the applet and the Arduino)

jeromq - https://github.com/zeromq/jeromq
(allowed communication between the applet and an Android device)
