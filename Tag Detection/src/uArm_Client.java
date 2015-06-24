import processing.core.*; 
import processing.event.*; 
import g4p_controls.*; 
import processing.video.*; 

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.util.Vector;

import org.jeromq.ZMQ;

import april.jcam.ImageSource;
import april.tag.TagFamily;
import april.util.ReflectUtil;

public class uArm_Client extends PApplet {

private static final long serialVersionUID = 1L;
boolean CAMERA_EN  = false;
boolean FFCAMERA_EN = false;
boolean UPDATE_EN = false;
byte suctionCup = 0x02;
String OS_NAME;
float wheelFactor;
ImageSource is = null;
TagFamily tf = (TagFamily) ReflectUtil.createObject("april.tag.Tag36h11");

public void setup() {
	OS_NAME = System.getProperty("os.name");
	if(OS_NAME.startsWith("Windows")){
		wheelFactor = 5.0f;
	}
	else {
		wheelFactor = 1.0f;
	}
	size(995, 450, JAVA2D);
 
	createGUI();
	customGUI();
	try {
		scanPort();
	} catch (Exception e) {
	e.printStackTrace();
	}
}

public void draw() {
	background(220);
	if(CAMERA_EN || FFCAMERA_EN) {
		readCamera();
    }
	if(UPDATE_EN) {
	    sendPos(slider2d2.getValueYI(), slider1.getValueI(), slider2d2.getValueXI(), knob1.getValueI(), suctionCup);
	    label4.setText(slider2d2.getValueXS());
	    label4.setTextBold();
	    label5.setText(slider2d2.getValueYS());
	    label5.setTextBold();
	    label7.setText(knob1.getValueS());
	    label7.setTextBold();
	    UPDATE_EN = false;
	}
}

public void setUIPos(int _handPosX, int _handPosY, int _handPosZ, int _handRot) {
	knob1.setValue(_handRot);
	slider1.setValue(_handPosY);
	slider2d2.setValueX(_handPosX);
	slider2d2.setValueY(_handPosZ);
}

public void customGUI() {
	slider2d2.setEasing(8.0f);
}

// Camera
Capture video;

//Start the camera feed within a thread so user can still do things
public void initCamera() {
	new startCam().start();
}

boolean ready = false;
class startCam extends Thread {
	public void run() {
		String[] cameras = Capture.list();
		video = new Capture(uArm_Client.this, 640, 480, cameras[0]);
		video.start();
		ready = true;
	}
}

//Open a web browser with the feed
public void initFFCamera() {/*
	if(Desktop.isDesktopSupported()) {
		try {
			Desktop.getDesktop().browse(new URI("http://localhost:8080/test.mjpg"));
		}
		catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}*/
}

public void exitCamera() {
	noTint(); // Disable tint
	video.stop();
}

public void readCamera() {
	if(CAMERA_EN && ready) {
		int xCoord = 87;
		int yCoord = 52;
		tint(255, 200);  // Display at half opacity
		image(video, xCoord, yCoord, 516, 386);
		noFill();
		stroke(0, 255, 0);
		strokeWeight(1);
	}
}

public void readFFCamera() {
	if(FFCAMERA_EN) {
	    /*
		int xCoord = 87;
		int yCoord = 52;
		tint(255, 200);  // Display at half opacity
		
		image(video, xCoord, yCoord, 516, 386);
		noFill();
		stroke(0, 255, 0);
		strokeWeight(1);*/
	}
}

//If no box was found in an area, rotate 30 degrees and keep looking
public Vector<int[]> scanForBoxes() {
	int i = -1;
	boolean up = false;
	Vector<int[]> boxCoordinates = new Vector<int[]>(0);
	
	while(boxCoordinates.isEmpty()) {
		System.out.println("Box not found. Scanning area");
		
		sendPos(0, 0, i*30, 0, suctionCup);
		
		//Detect box and retrieve coordinates
		BoxRetrieve tt = new BoxRetrieve();
		if(CAMERA_EN) {
			try {is = ImageSource.make("v4l2:///dev/video1");} 
			catch (IOException e) {e.printStackTrace();}
			tt.usb_setup(is);
		}
		boxCoordinates = tt.locate(CAMERA_EN, tf, i);
		
		if(up) i++;
		else i--;
		if(i <= -3) up = true;
		if(i >=  3) up = false;
	}
	
	return boxCoordinates;
}

//Set up a ZMQ server so an android or other device can respond
//and tell the uArm to either retrieve or not retrieve the box
public boolean sendZMQ() {
	System.out.println("Setting up server");
	ZMQ.Context context = ZMQ.context(1);
	// Socket to talk to clients
	ZMQ.Socket socket = context.socket(ZMQ.REP);
	
	//local
	//socket.bind ("tcp://*:5555");
	
	//network
	socket.bind ("tcp://192.168.1.195:9000");
	
	byte[] reply;
	String r;
	
	System.out.println("Waiting for message...");
	try {
	    while (!Thread.currentThread().isInterrupted()) {
	        reply = socket.recv(0);
	        r = new String(reply);
	        
	        System.out.println("Message Received: " + r);
	        
	        //if client responds with y or n exit the function
	        if(r.equals("y")) {
		        String request = "Retrieving box";
		        socket.send(request.getBytes(), 0);
	        	socket.close();
	        	context.term();
	        	return true;
	        }
	        else if(r.equals("n")) {
		        String request = "Not retrieving box";
		        socket.send(request.getBytes(), 0);
	        	socket.close();
	        	context.term();
	        	return false;
	        }	        
	        
	        //prompt the client for whether they want to retrieve
	        //the box(es)
	        int i = boxCoordinates.size();
	        String request;
	        if(i > 1) {
	        	request = i + " Boxes available. Retrieve? (y/n)" ;
	        }
	        else {
	        	request = i + " Box available. Retrieve? (y/n)" ;
	        }
	        socket.send(request.getBytes(), 0);
	        Thread.sleep(1000); // Do some 'work'
	    }
	} catch(Exception e) {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    System.out.println(sw.toString());
	}
	System.out.println("Thread has been interrupted");
	socket.close();
	context.term();
	
	return false;
}

//Get the box and bring it back to the user
public void getBox(Vector<int[]> coordinates) {
	if(suctionCup == 0x01){
		button1.setText("Release");
	    button1.setTextBold();
	    suctionCup = 0x02;
	}
	//If there is more than one box
	for(int[] c : coordinates) {
		//Move to just above the box
		sendPos(0, c[2] + 75, 0, 0, suctionCup);
		try {Thread.sleep(100);}
		catch (InterruptedException e) {e.printStackTrace();}
		sendPos(c[0], c[2] + 65, c[1], 0, suctionCup);
		try {Thread.sleep(1000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
		//Lower to the box
		button1.setText("Catch");
	    button1.setTextBold();
	    suctionCup = 0x01;
		sendPos(c[0], c[2], c[1], 0, suctionCup);
		try {Thread.sleep(1000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
		//Lift the box up
		sendPos(c[0], c[2] + 75, c[1], 0, suctionCup);
		try {Thread.sleep(1000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
		//Drop the box off
		sendPos(30, 50, 90, 0, suctionCup);
		try {Thread.sleep(1000);}
		catch (InterruptedException e) {e.printStackTrace();}	
		button1.setText("Release");
	    button1.setTextBold();
	    suctionCup = 0x02;
		sendPos(30, 50, 90, 0, suctionCup);
		try {Thread.sleep(1000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
	    //Return to the home position
		sendPos(30, 50, 0, 0, suctionCup);
		try {Thread.sleep(200);}
		catch (InterruptedException e) {e.printStackTrace();}
		sendPos(0, 0, 0, 0, suctionCup);
		try {Thread.sleep(2000);}
		catch (InterruptedException e) {e.printStackTrace();}
	}
}

public void captureEvent(Capture c) {
	c.read();
}

public void panel4_Click1(GPanel source, GEvent event) {} //_CODE_:panel4:963415:

//MJPEG feed option
public void checkbox1_clicked1(GCheckbox source, GEvent event) {
	if(checkbox1.isSelected()) {
		initFFCamera();
		FFCAMERA_EN = true;
		button3.setEnabled(true);
		button3.setAlpha(255);
	}
	else {
		FFCAMERA_EN = false;
		//exitCamera();
		button3.setEnabled(false);
		button3.setAlpha(100);
	}
	if(checkbox2.isSelected()) {
		CAMERA_EN = false;
		exitCamera();
		checkbox2.setSelected(false);
	}
}

//Choose which tag family you want to use
public void dropList1_click1(GDropList source, GEvent event) {
	System.out.println(dropList1.getSelectedText());
	tf = (TagFamily) ReflectUtil.createObject("april.tag." + dropList1.getSelectedText());
}

//USB feed option
public void checkbox2_clicked1(GCheckbox source, GEvent event) {
	if(checkbox2.isSelected()) {
		initCamera();
		CAMERA_EN = true;
		button3.setEnabled(true);
		button3.setAlpha(255);
	}
	else {
		CAMERA_EN = false;
		exitCamera();
		button3.setEnabled(false);
		button3.setAlpha(100);
	}
	if(checkbox1.isSelected()) {
		FFCAMERA_EN	= false;
		checkbox1.setSelected(false);
	}
}

public void slider1_change1(GSlider source, GEvent event) {
	UPDATE_EN = true;
}

public void knob1_turn1(GKnob source, GEvent event) {
	UPDATE_EN = true;
}

public void button1_click1(GButton source, GEvent event) {
	UPDATE_EN = true;
	if(suctionCup == 0x01) {
		button1.setText("Release");
		button1.setTextBold();
		suctionCup = 0x02;
	}
	else {
		button1.setText("Catch");
		button1.setTextBold();
		suctionCup = 0x01;
	}
}

public void slider2d2_change1(GSlider2D source, GEvent event) {
	UPDATE_EN = true;
}

Vector<int[]> boxCoordinates = new Vector<int[]>(0);

//Locate all boxes in view
public void button3_click1(GButton source, GEvent event) {
	if(CAMERA_EN || FFCAMERA_EN) {
		//Move the arm to the home position for the camera to look
		sendPos(0, 0, 0, 0, suctionCup);
		
		//Switch the video stream to search for boxes
		try {Thread.sleep(500);}
		catch (InterruptedException e1) {e1.printStackTrace();}
		
		
		//Detect box and retrieve coordinates
		BoxRetrieve tt = new BoxRetrieve();
		if(CAMERA_EN) {
			video.stop();
			try {is = ImageSource.make("v4l2:///dev/video1");} 
			catch (IOException e) {e.printStackTrace();}
			tt.usb_setup(is);
		}
		boxCoordinates = tt.locate(CAMERA_EN, tf, 0);
		
		
		//If no boxes were found, continue looking
		if(boxCoordinates.isEmpty()) {
			boxCoordinates = scanForBoxes();
		}
		
		//Return the video stream to the display
		if(CAMERA_EN) {
			video.start();
		}
		
		//Enable the retrieval of the boxes
		button4.setEnabled(true);
		button4.setAlpha(255);
		button5.setEnabled(true);
		button5.setAlpha(255);
	}
}

//Get the box
public void button4_click1(GButton source, GEvent event) {
	//Retrieve the box
	getBox(boxCoordinates);
	
	System.out.println("---------------------------");

	//Disable the button until new boxes are located
	button4.setEnabled(false);
	button4.setAlpha(100);
	button5.setEnabled(false);
	button5.setAlpha(100);
}

//Send a ZMQ message that the box is ready for retrieval
public void button5_click1(GButton source, GEvent event) {
	boolean get = sendZMQ();
	
	if(get) {
		System.out.println("User selected to retrieve box");
		
		//Retrieve the box
		getBox(boxCoordinates);
		
		System.out.println("---------------------------");

		//Disable the button until new boxes are located
		button4.setEnabled(false);
		button4.setAlpha(100);
		button5.setEnabled(false);
		button5.setAlpha(100);
	}
	else {
		System.out.println("User selected not to retrieve box");
	}
}

//Run the class AndroidControl which lets the device move the arm
boolean clicked = false;
AndroidControl a = new AndroidControl();

public void button6_click1(GButton source, GEvent event) {
	//Gets the current location for all of the servos
	int s = slider2d2.getValueYI();
	int r = slider2d2.getValueXI();
	int h = slider1.getValueI();
	//turn off all of the other buttons
	if(!clicked) {
		clicked = true;
		button6.setTextBold();
		button3.setEnabled(false);
		button3.setAlpha(100);
		button4.setEnabled(false);
		button4.setAlpha(100);
		button5.setEnabled(false);
		button5.setAlpha(100);
		a.begin(s, r, h, suctionCup);
	}
	//Takes away control from the device
	else {
		a.exit();
		button6_unclick();
	}
}

//make the box appear unclicked
public void button6_unclick() {
	clicked = false;
	button6.setTextPlain();
	button3.setEnabled(true);
	button3.setAlpha(255);
}

public void imgButton2_click1(GImageButton source, GEvent event) {}

// Create all the GUI controls. 
// autogenerated do not edit
public void createGUI() {
	G4P.messagesEnabled(false);
	G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
	G4P.setCursor(ARROW);
	if(frame != null)
		frame.setTitle("uArm Control Panel");
	panel4 = new GPanel(this, 665, 260, 320, 180, "Setting");
	panel4.setText("Setting");
	panel4.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	panel4.setOpaque(true);
	panel4.addEventHandler(this, "panel4_Click1");
	checkbox1 = new GCheckbox(this, 11, 125, 300, 25);
	checkbox1.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
	checkbox1.setText("Camera - ffstream");
	checkbox1.setTextBold();
	checkbox1.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	checkbox1.setOpaque(false);
	checkbox1.addEventHandler(this, "checkbox1_clicked1");
	dropList1 = new GDropList(this, 10, 35, 300, 210, 10);
	String items[] = new String[6];			
		items[0] = "Tag16h5";				
		items[1] = "Tag25h7";				
		items[2] = "Tag25h9";				
		items[3] = "Tag36h9";				
		items[4] = "Tag36h10";				
		items[5] = "Tag36h11";
	dropList1.setItems(items, 5);
	dropList1.addEventHandler(this, "dropList1_click1");
	checkbox2 = new GCheckbox(this, 11, 150, 300, 25);
	checkbox2.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
	checkbox2.setText("Camera - USB");
	checkbox2.setTextBold();
	checkbox2.setOpaque(false);
	checkbox2.addEventHandler(this, "checkbox2_clicked1");
	panel4.addControl(dropList1);
	panel4.addControl(checkbox1);
	panel4.addControl(checkbox2);
	slider1 = new GSlider(this, 85, 90, 350, 80, 20.0f);
	slider1.setShowValue(true);
	slider1.setShowLimits(true);
	slider1.setTextOrientation(G4P.ORIENT_LEFT);
	slider1.setRotation(PI/2, GControlMode.CORNER);
	slider1.setLimits(0, 150, -180);
	slider1.setEasing(8.0f);
	slider1.setNumberFormat(G4P.INTEGER, 0);
	slider1.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	slider1.setOpaque(false);
	slider1.addEventHandler(this, "slider1_change1");
	knob1 = new GKnob(this, 725, 52, 200, 200, 0.8f);
	knob1.setTurnRange(180, 0);
	knob1.setTurnMode(GKnob.CTRL_ANGULAR);
	knob1.setShowArcOnly(true);
	knob1.setOverArcOnly(true);
	knob1.setIncludeOverBezel(false);
	knob1.setShowTrack(false);
	knob1.setLimits(0.0f, -90.0f, 90.0f);
	knob1.setShowTicks(true);
	knob1.setEasing(10.0f);
	knob1.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	knob1.setOpaque(false);
	knob1.addEventHandler(this, "knob1_turn1");
	button1 = new GButton(this, 735, 190, 180, 50);
	button1.setText("Release");
	button1.setTextBold();
	button1.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	button1.addEventHandler(this, "button1_click1");
	label1 = new GLabel(this, 509, 32, 164, 22);
	label1.setTextAlign(GAlign.LEFT, GAlign.TOP);
	label1.setText("Notice: Polar coordinates");
	label1.setTextBold();
	label1.setOpaque(false);
	slider2d2 = new GSlider2D(this, 85, 50, 520, 390);
	slider2d2.setLimitsX(0.0f, -90.0f, 90.0f);
	slider2d2.setLimitsY(210.0f, 210.0f, 0.0f);
	slider2d2.setNumberFormat(G4P.INTEGER, 0);
	slider2d2.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	slider2d2.setOpaque(true);
	slider2d2.addEventHandler(this, "slider2d2_change1");
	imgButton1 = new GImageButton(this, 85, 20, 160, 18, new String[] { "logo.png", "logo.png", "logo.png" } );
	button3 = new GButton(this, 930, 20, 60, 40);
	button3.setText("Locate Boxes");
	button3.setEnabled(false);
	button3.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	button3.setAlpha(100);
	button3.addEventHandler(this, "button3_click1");
	button4 = new GButton(this, 930, 80, 60, 40);
	button4.setText("Retrieve Boxes");
	button4.setEnabled(false);
	button4.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	button4.setAlpha(100);
	button4.addEventHandler(this, "button4_click1");
	button5 = new GButton(this, 930, 140, 60, 40);
	button5.setText("Send ZMQ");
	button5.setEnabled(false);
	button5.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	button5.setAlpha(100);
	button5.addEventHandler(this, "button5_click1");
	button6 = new GButton(this, 630, 20, 80, 60);
	button6.setText("Android control");
	button6.setEnabled(true);
	button6.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	button6.setAlpha(255);
	button6.addEventHandler(this, "button6_click1");
	imgButton2 = new GImageButton(this, 25, 12, 40, 40, new String[] { "LOGO-50.png", "LOGO-50.png", "LOGO-50.png" } );
	imgButton2.addEventHandler(this, "imgButton2_click1");
	label2 = new GLabel(this, 275, 32, 68, 20);
	label2.setText("Rotation:");
	label2.setTextBold();
	label2.setOpaque(false);
	label3 = new GLabel(this, 385, 32, 65, 20);
	label3.setText("Stretch:");
	label3.setTextBold();
	label3.setOpaque(false);
	label4 = new GLabel(this, 335, 32, 40, 20);
	label4.setText("0");
	label4.setTextBold();
	label4.setOpaque(false);
	label5 = new GLabel(this, 440, 32, 40, 20);
	label5.setText("0");
	label5.setTextBold();
	label5.setOpaque(false);
	label6 = new GLabel(this, 765, 32, 80, 20);
	label6.setText("Hand Angle: ");
	label6.setTextBold();
	label6.setOpaque(false);
	label7 = new GLabel(this, 845, 32, 50, 20);
	label7.setText("0");
	label7.setTextBold();
	label7.setOpaque(false);
	label8 = new GLabel(this, 785, 172, 80, 20);
	label8.setText("Grab");
	label8.setTextBold();
	label8.setOpaque(false);
	label9 = new GLabel(this, 12, 65, 65, 20);
	label9.setText("Height:");
	label9.setTextBold();
	label9.setOpaque(false);
	label10 = new GLabel(this, 928, 120, 65, 20);
	label10.setText("or");
	//label10.setTextBold();
	label10.setOpaque(false);
}

public void mouseWheel(MouseEvent event) {
	float e = event.getCount();
	  
	float v = slider1.getValueF();
	  
	slider1.setValue(v - e*wheelFactor);
}

boolean hasCLICKED;

public void mouseClicked() {
	if(mouseX >=85 && mouseX <=600 && mouseY >=50 && mouseY <= 430){ 
		hasCLICKED = !hasCLICKED;
	}
}

public void mouseMoved() {
	if(hasCLICKED){
		float valueX = map(mouseX,85,600,-90,90);
		float valueY = map(mouseY,50,430,210,0);
		slider2d2.setValueX(valueX);
		slider2d2.setValueY(valueY);
	}
}

// Variable declarations 
// autogenerated do not edit
GPanel panel4;
GCheckbox checkbox1;  
GDropList dropList1; 
GCheckbox checkbox2; 
static GSlider slider1; 
GKnob knob1; 
GButton button1; 
GLabel label1; 
static GSlider2D slider2d2; 
GImageButton imgButton1; 
GButton button3;
GButton button4;
GButton button5;
GButton button6;
GImageButton imgButton2; 
GLabel label2; 
GLabel label3; 
GLabel label4; 
GLabel label5; 
GLabel label6; 
GLabel label7; 
GLabel label8; 
GLabel label9; 
GLabel label10;

// serial port
SerialPort serialPort;
private static OutputStream   serialOut;

public void scanPort()
throws Exception{
	CommPortIdentifier port = CommPortIdentifier.getPortIdentifier("/dev/ttyUSB0"); 
    CommPort commPort = port.open(this.getClass().getName(),2000);
    serialPort = (SerialPort) commPort;
    serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
	serialOut=serialPort.getOutputStream();
    serialPort.notifyOnDataAvailable(true);
}

public static void sendPos(int _posZ, int _posY, int _posX, int _posH, byte ctlData) {
	//println("Rotation: " + _posX + "  Stretch: " + _posY + "  Height: " + _posZ + "  HandRot: " + _posH + "  Grab: " + ctlData);
	byte[] send = {
		PApplet.parseByte(0xFF),
		PApplet.parseByte(0xAA),
		PApplet.parseByte((_posX >> 8) & 0xFF),
		PApplet.parseByte( _posX & 0xFF),
		PApplet.parseByte((_posZ >> 8) & 0xFF),
		PApplet.parseByte( _posZ & 0xFF),
		PApplet.parseByte((_posY >> 8) & 0xFF),
		PApplet.parseByte( _posY & 0xFF),
		PApplet.parseByte((_posH >> 8) & 0xFF),
		PApplet.parseByte( _posH & 0xFF),
		PApplet.parseByte(ctlData)
	};
	try {
		serialOut.write(send);
	} catch (IOException e) {
		e.printStackTrace();
	}
}

static public void main(String[] passedArgs) {
	String[] appletArgs = new String[] { "uArm_Client" };
	if (passedArgs != null) {
		PApplet.main(concat(appletArgs, passedArgs));
	} else {
		PApplet.main(appletArgs);
	}
}
}
