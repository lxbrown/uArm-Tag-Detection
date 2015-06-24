package com.camera.simplemjpeg;
import android.os.Handler;

import org.jeromq.ZMQ;
 
public class ZeroMQServer implements Runnable {
    private final Handler uiThreadHandler;
 
    public ZeroMQServer(Handler uiThreadHandler) {
        this.uiThreadHandler = uiThreadHandler;
    }
    
    
 
    @Override
    public void run() {
    	
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        uiThreadHandler.sendMessage(
                Util.bundledMessage(uiThreadHandler, "HIlI"));
        //use for android to android
        //socket.bind("tcp://127.0.0.1:5556");
        
        //use for linux client to android server
        socket.bind("tcp://192.168.1.196:9000");
        uiThreadHandler.sendMessage(
                Util.bundledMessage(uiThreadHandler, "HI2I"));
       Thread.currentThread().setPriority(Thread.MAX_PRIORITY); 
        try{
	        while(!Thread.currentThread().isInterrupted()) {
	        	uiThreadHandler.sendMessage(
	                    Util.bundledMessage(uiThreadHandler, "HI3I"));
	            byte[] msg = socket.recv(0);
	            uiThreadHandler.sendMessage(
	                    Util.bundledMessage(uiThreadHandler, new String(msg)));
	            socket.send(new String((msg)), 0);
	        }
        }
        catch(Exception e) {
        	
        }
        
        
        socket.close();
        context.term();
    }
}
