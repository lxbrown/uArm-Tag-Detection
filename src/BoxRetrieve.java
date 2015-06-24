
import april.tag.*;

import java.awt.image.*;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import april.jcam.*;

import april.util.*;  

public class BoxRetrieve {
JFrame jf;
ImageSource is;
TagFamily tf;
TagDetector detector;

public static void main(String args[]) {
    GetOpt opts  = new GetOpt();
    opts.addBoolean('h',"help",false,"See this help screen");
    opts.addString('u',"url","v4l2:///dev/video1","Camera url");
    opts.addString('t',"tagfamily","april.tag.Tag36h11","Tag family");

    if (!opts.parse(args)) {
        System.out.println("option error: " + opts.getReason());
    }

    String url = opts.getString("url");

    String tagfamily = opts.getString("tagfamily");

    if (opts.getBoolean("help") || url.isEmpty()){
    	System.out.println("Usage: BoxRetrieve [cameraurl]");
        opts.doHelp();
        System.exit(1);
    }

    ImageSource is;
	try {
		is = ImageSource.make(url);
	
        TagFamily tf = (TagFamily) ReflectUtil.createObject(tagfamily);

        BoxRetrieve tt = new BoxRetrieve();
        tt.usb_setup(is);
        tt.locate(true, tf, 0);
	} catch (IOException e) {
		e.printStackTrace();
	}

}

public void usb_setup(ImageSource is) {
	this.is = is;
    
    is.start();
}

private BufferedImage get_image(boolean usb) {
	BufferedImage im;
	if(usb) {
	    FrameData frmd = is.getFrame();
	    im = ImageConvert.convertToImage(frmd);
	    
	} else {
		MJPEGParser m = new MJPEGParser("http://localhost:8080/test.mjpg");
		while(!m.ready()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		im = m.getImage();
	}
    return im;
}

public BoxRetrieve() {}

public Vector<int[]> locate(boolean usb, TagFamily tf, int rotPos) {
    detector = new TagDetector(tf);

    //Declare thetaGoal and set it equal to the home position
    double[] thetaGoal = {0, 1.396263402, -2, -Math.PI /2, 0, 0};

    double[] Home = new double[4];

    Home[0] = thetaGoal[0];
    Home[1] = thetaGoal[1] + (Math.PI/2);
    Home[2] = thetaGoal[2];
    Home[3] = thetaGoal[3];

    try{
    	Thread.sleep(1000);
    }
    catch(InterruptedException e){
    }

    boolean tagDetectRun = true;
    int counter = 0;
    Vector<int[]> coordinates = new Vector<int[]>(0);
    System.out.println("Searching");

    while (tagDetectRun && counter < 20) {      
        BufferedImage im = get_image(usb);
        if(im == null) {
        	System.out.println("invalid image");
        	break;
        }

        ArrayList<TagDetection> detections = detector.process(im, new double[] {im.getWidth()/2.0, im.getHeight()/2.0});

        if(detections.size() > 0) {
        	System.out.println("Found " + detections.size() + " boxes");
        }
        else {
        	counter++;
        	System.out.println("Not found");
        }
            
        for (TagDetection d : detections) {
        	// You need to adjust the tag size (measured across the whole tag in meters and the focal length.
        	//4.5 cm from edge to edge >> 0.045 m (big box)
        	//2.8 cm from edge to edge >> 0.028 m (small box)
		    double tagsize_m = 0.045;
		
		    double fx = -597.031353;
		
		    double fy = 595.860429;
		
		    double cx = 326.673;
		
		    double cy = 238.282;
		
		    //Define Robot Parameters.
		    //In meters
			double d1 = 0.09095;
			
			double a2 = 0.1508;
			
			double a3 = 0.14125;
			
			double a4Cam = .080;
			
			double dCamera = 0.080;
			
			double M[][] = CameraUtil.homographyToPose(fx, fy, cx, cy, d.homography);
			
			        M = CameraUtil.scalePose(M, 2.0, tagsize_m);
			
			        //CAMERA
			double thetaCamera = 0;
			
			double cg[][] = new double[4][4];
			
			cg[0][0] = Math.sin(Home[0]);
			cg[0][1] = Math.sin(-thetaCamera - Home[3] - Home[2] + Home[0] -
			Home[1]) / 0.2e1 - Math.sin(thetaCamera + Home[3] + Home[2] + Home[0]
			+ Home[1]) / 0.2e1;
			cg[0][2] = -Math.cos(-thetaCamera - Home[3] - Home[2] + Home[0] -
			Home[1]) / 0.2e1 - Math.cos(thetaCamera + Home[3] + Home[2] + Home[0]
			+ Home[1]) / 0.2e1;
			cg[0][3] = dCamera * Math.sin(-Home[3] - Home[2] + Home[0] -
			Home[1]) / 0.2e1 - dCamera * Math.sin(Home[3] + Home[2] + Home[0]
			+ Home[1]) / 0.2e1 + a4Cam * Math.cos(-Home[3] - Home[2] + Home[0] -
			Home[1]) / 0.2e1 + a4Cam * Math.cos(Home[3] + Home[2] + Home[0] +
			Home[1]) / 0.2e1 + a3 * Math.cos(-Home[2] + Home[0] - Home[1]) /
			0.2e1 + a3 * Math.cos(Home[2] + Home[0] + Home[1]) / 0.2e1 + a2
			* Math.cos(Home[0] - Home[1]) / 0.2e1 + a2 * Math.cos(Home[0] +
			Home[1]) / 0.2e1;
			cg[1][0] = -Math.cos(Home[0]);
			cg[1][1] = Math.cos(thetaCamera + Home[3] + Home[2] + Home[0] +
			Home[1]) / 0.2e1 - Math.cos(-thetaCamera - Home[3] - Home[2] + Home[0]
			- Home[1]) / 0.2e1;
			cg[1][2] = -Math.sin(thetaCamera + Home[3] + Home[2] + Home[0] +
			Home[1]) / 0.2e1 - Math.sin(-thetaCamera - Home[3] - Home[2] + Home[0]
			- Home[1]) / 0.2e1;
			cg[1][3] = dCamera * Math.cos(Home[3] + Home[2] + Home[0] +
			Home[1]) / 0.2e1 - dCamera * Math.cos(-Home[3] - Home[2] +
			Home[0] - Home[1]) / 0.2e1 + a4Cam * Math.sin(Home[3] + Home[2] +
			Home[0] + Home[1]) / 0.2e1 + a4Cam * Math.sin(-Home[3] - Home[2] +
			Home[0] - Home[1]) / 0.2e1 + a3 * Math.sin(Home[2] + Home[0] +
			Home[1]) / 0.2e1 + a3 * Math.sin(-Home[2] + Home[0] - Home[1]) /
			0.2e1 + a2 * Math.sin(Home[0] + Home[1]) / 0.2e1 + a2 * Math.
			sin(Home[0] - Home[1]) / 0.2e1;
			cg[2][0] = 0;
			cg[2][1] = Math.cos(Home[1] + Home[2] + Home[3] + thetaCamera);
			cg[2][2] = -Math.sin(Home[1] + Home[2] + Home[3] + thetaCamera);
			cg[2][3] = Math.cos(Home[1] + Home[2] + Home[3]) * dCamera + a4Cam
			* Math.sin(Home[1] + Home[2] + Home[3]) + a3 * Math.sin(Home[1] +
			Home[2]) + a2 * Math.sin(Home[1]) + d1;
			cg[3][0] = 0;
			cg[3][1] = 0;
			cg[3][2] = 0;
			cg[3][3] = 1;
			
			//Determine the transformation matrix that will project a point in the box frame into the world frame.
			double TrBoxWorld[][] = new double[4][4];
			
			int i;
			int j;
			int k;
			
			//Set to 0
			for(i = 0; i <4; i++){
				for(j=0; j<4; j++){
					TrBoxWorld[i][j] = 0;
				}
			}
			
			//Perform Matrix Multiplication
			for(i=0;i<=3;i++){
				for(j=0;j<=3;j++){
					for(k=0;k<=3;k++){
						TrBoxWorld[i][j] += cg[i][k]*M[k][j];
					}
				}
			}
			
			//Create variables for x, y, and z from TrBoxWorld for use in calculation of servo values
			
			double xBox = TrBoxWorld[0][3]; //- 0.10;
			
			double yBox = TrBoxWorld[1][3] * 1.15;
			
			double zBox = TrBoxWorld[2][3] - 0.01;
			
			boolean reachable;
			
			//System.out.println("xBox: " + xBox);
			//System.out.println("yBox: " + yBox);
			//System.out.println("zBox: " + zBox);
			
			int x, y, z;
			double rotConst, rotOffset, stretchConst;
			double heightOffset, heightConst;
			
			stretchConst = 1100;		
			x = (int) (xBox * stretchConst);
			
			rotConst = -170 - ((210 - x) * (0.166666));
			rotOffset = 0.03074665;
			y = (int) ((yBox - rotOffset) * rotConst);
			y = y + (rotPos * 30);
			
			heightOffset = 0.1526661 - 0.00030359328 * (x - 147);
			heightConst = 950;
			z = (int) ((zBox - heightOffset) * heightConst);
			
			System.out.println("x: " + x);
			System.out.println("y: " + y);
			System.out.println("z: " + z);
			
			if(x >= 0 && x <= 210 &&
				y >= -90 && y <= 90 &&
				z >= -180 && z <= 150) {
				reachable = true;
				System.out.println("Box is reachable");
			}
			else {
				reachable = false;
				System.out.println("Box is not reachable");
			}
			System.out.println();
			
			//If the box is reachable go through picking up box
			if(reachable){
			
				try{
					Thread.sleep(1000);
			
				}catch(InterruptedException e){
				}
				
				int[] boxLocation = new int[3];
				boxLocation[0] = x;
				boxLocation[1] = y;
				boxLocation[2] = z;
				coordinates.add(boxLocation);
				
				if(usb) {
					is.stop();
					is.close();
				}
				
			}//if reachable
			
			tagDetectRun = false;
			
        }//for tag detections d : detections

    }//while tagDetectRun
    if(usb) {
	    is.stop();
	    is.close();
    }
	return coordinates;
}
}
