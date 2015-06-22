# uArm-Tag-Detection
Utilizing the april library created at the University of Michigan and the open source uArm Client created by the makers , control the uArm and have it automatically send and recieve commands from an Android device, have the Android device control the uArm, and detect and retrieve boxes with april tags.

This program uses open source content which can be downloaded at the following links from their original creators:

uArm Client - https://github.com/UFactory-Team/uArm_Client
(used for original applet layout and control of the arm using sliders)

MJPEGParser - https://github.com/vanevery/Processing-Read-Axis-Camera-MJPEG/blob/master/MJPEGParser.pde
(reading an image in from a MJPEG stream)

Arduino - https://atulmaharaj.wordpress.com/2013/05/03/arduino-serial-communication-using-java-and-rxtx/
(serial port communication between the applet and the Arduino)

jeromq - https://github.com/zeromq/jeromq
(allowed communication between the applet and an Android device)
