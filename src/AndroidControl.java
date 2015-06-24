import java.io.PrintWriter;
import java.io.StringWriter;

import org.jeromq.ZMQ;


public class AndroidControl {
ZMQ.Context context;
ZMQ.Socket socket;

int str, rot, hei;
byte suctionCup;

public static volatile boolean exitFlag;

//Empty constructor
public AndroidControl() {}

//Constructor that starts the thread
public AndroidControl(int _str, int _rot, int _hei, byte _suctionCup) {
	System.out.println("Setting up server");
	context = ZMQ.context(1);
	socket = context.socket(ZMQ.REP);
	str = _str;
	rot = _rot;
	hei = _hei;
	suctionCup = _suctionCup;
	exitFlag = false;
	
	//Use a thread so the computer can also control the uArm
	//and look for boxes
	new RunThread().start();
}

//Call the full constructor
public void begin(int _str, int _rot, int _hei, byte _suctionCup) {
	new AndroidControl(_str, _rot, _hei, _suctionCup);
}

//If the button is unclicked, set the flag to true
//to exit the thread
public void exit() {
	exitFlag = true;
}

class RunThread extends Thread {
	public void run() {
		
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
		        String confirm = null;
		        
		        //If the button is unclicked exit the thread
		        if(exitFlag) { break; }
		        
		        System.out.println("Message Received: " + r);
		        
		        //Move the direction associated with message
		        if(r.equals("l")) {
			        confirm = "Moving left";
			        
			        rot -= 10;
		        }
		        else if(r.equals("r")) {
			        confirm = "Moving right";
			        
			        rot += 10;
		        }
		        else if(r.equals("f")) {
			        confirm = "Moving forward";
			        
			        str += 30;
		        }
		        else if(r.equals("b")) {
			        confirm = "Moving back";
			        
			        str -= 30;
		        }
		        else if(r.equals("u")) {
			        confirm = "Moving up";
			        
			        hei += 30;
		        }
		        else if(r.equals("d")) {
			        confirm = "Moving down";
			        
			        hei -= 30;
		        }
		        else if(r.equals("Connected")) {
			        confirm = "Use directional arrows to control uArm";
		        }
		        else {
		        	confirm = "Invalid response";
		        }
		        
		        socket.send(confirm.getBytes(), 0);
				      
		        //Send the new position to the arm and update the sliders
			    uArm_Client.sendPos(str, hei, rot, 0, suctionCup);
				uArm_Client.slider2d2.setValueXY(rot, str);
				uArm_Client.slider1.setValue(hei);
		        //Thread.sleep(1000); // Do some 'work'
		    }
		} catch(Exception e) {
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    System.out.println(sw.toString());
		}
		
		System.out.println("Thread has been ended");
		socket.close();
		context.term();
	}
}
}
