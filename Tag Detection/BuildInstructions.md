The following instructions are designed to help build and use the TagDetection program:

1. Follow the instructions here http://rxtx.qbang.org/wiki/index.php/Main_Page to install the RXTX library used in the serial port communication. You should just need to copy RXTXcomm.jar into /$JAVA_HOME/jre/lib/ext/ and librxtxSerial.so into /$JAVA_HOME/jre/lib/[machine type] (Java versions are located in /usr/lib/jvm/).

2. Load the program into Eclipse or a similar IDE and make sure all of the libraries in the lib folder are included in the build path. *Note that the april library is only compatible with linux machines.

3. Plug in the uArm and video camera to the computer via USB. Then, if you are not an administator on the computer you will need to add the following line of code to a terminal to gain access to the uArm: (excluding quotations)
"sudo chmod 666 /dev/ttyUSB0"
If the port is in a different location than /dev/ttyUSB0 use that instead.

4. If you want to use the ffserver first download ffmpeg: 
https://www.ffmpeg.org/download.html
Then run the bash script "startff.sh" in the ffserver folder. If the webcam is in a different location than /dev/video1 edit the bash script to use the new location.

5. Run uArm_Client.java.

6. Select the Tag Family being used in the drop-box located in the middle-right area of the screen.

7. To activate the camera click the checkbox in the lower-right corner associated with the feed you are using.

8. Once it is up and running you should be able to click the "Locate Boxes" button which will look for boxes in the area. If it can't find any it will scan the area by rotating the arm.

9. Once a box is found the "Retrieve Boxes" and "Send ZMQ" buttons will become available. "Retrieve Boxes" will pick up the boxes automatically and "Send ZMQ" will send a message to the Android tablet and let the tablet decide if it will retrieve the box. 

10. "Android Control" gives the tablet control of the device and can be removed by clicking the button again.

11. The sliders on the main display can also be used to control the arm from the computer.
