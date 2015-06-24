import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

import javax.imageio.*;

public class MJPEGParser {
	private BufferedImage bimage = null;
	private boolean ready;
	String mJpegUrl;
  
	int w;
	int h;
	boolean connected;

	DataInputStream dis;  
	/*  
public static void main(String[] args) {
	CopyOfMJPEGParser m = new CopyOfMJPEGParser();
	//8080 or 8090
	m.getImage("http://localhost:8080/test.mjpg");
}*/
  
public MJPEGParser(String _mJpegUrl) {
	
	mJpegUrl = _mJpegUrl;
	
	w = 640;
	h = 480;
	ready = false;
	connected = connect();
	
	new RunThread().start();
}

class RunThread extends Thread {
	public void run() {
		if(!connected) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//return;
			//return bimage;
		}
		
		int i = 0;
		int found = 0;
		System.out.println("Reading .mjpg stream");
		
		while (found == 0) {
		    readLine(3, dis); // discard the first 3 lines
		    found = readJPG();
		    readLine(2, dis); // discard the last two lines
		    i++;
		    if(i > 700) {
		    	System.out.println("Too Many Calls");
		    	break;
		    }
		}
		ready = true;
	}
	//return bimage;
}

public BufferedImage getImage() {
	return bimage;
}

public boolean ready() {
	return ready;
}

private boolean connect() {
	try {
    	URL url = new URL(mJpegUrl);
        
    	BufferedInputStream bin = new BufferedInputStream(url.openStream());
    	dis = new DataInputStream(bin);
    
      	} catch (IOException e) {
      		try {
      			System.out.println("Failed to connect");
				Thread.sleep(500);
			} catch (InterruptedException e1) {}
      		return false;
      	}   
	return true;
  }

private int readJPG() { // read the embedded jpeg image
	try {
		bimage = ImageIO.read(dis);
		//File file = new File("image.jpg");
		if(bimage != null) {
			//ImageIO.write(bimage, "jpg", file);
		    System.out.println("Image file written successfully");
		    return 1;
		}
	      
    } catch (NullPointerException e) {
    	System.out.println("NULL");
    	return -1;
    } catch (IOException e) {
    	System.out.println("Display Failed");
    	return -1;
	} catch (IllegalArgumentException e) {
		System.out.println("Data Stream Error");
		return -1;
	}
	
	return 0;
}	
  
private void readLine(int n, DataInputStream dis) { // used to strip out the header lines
	for (int i = 0; i < n; i++) {
      readLine(dis);
    }
  }
  
String lineEnd = "\n"; // assumes that the end of the line is marked with this
byte[] lineEndBytes = lineEnd.getBytes();
byte[] byteBuf = new byte[lineEndBytes.length];
boolean end = false;
String t = new String(byteBuf);
    
private void readLine(DataInputStream dis) {
    //System.out.println("readLine");
	try {
		end = false;

		while (!end) {
			dis.read(byteBuf, 0, lineEndBytes.length);
		    t = new String(byteBuf);
		    // System.out.print(t); //uncomment if you want to see what the lines actually look like
		    if (t.equals(lineEnd)) end = true;
	    }
    } catch (Exception e) {
      e.printStackTrace();
      }
}
}
