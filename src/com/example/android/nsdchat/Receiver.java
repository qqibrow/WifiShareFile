package com.example.android.nsdchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class Receiver {
	private static final int WAIT_TIME_TO_DIE = 300;
	Socket s;
	String name;
	PackageHandler packagehandler = null;
	public Receiver(Socket s, String name) {
		this.s = s;
		this.name = name;
		packagehandler = new PackageHandler(s, name);
	}
	
	public void start() {
		new Thread(new ReceiverThread()).start();
	}
	
	
	class ReceiverThread implements Runnable {
		public void run() {
			try {
				InputStream in = s.getInputStream();
				ProtocolPackage p = null;
				while((p = ProtocolPackage.receivePackage(in)) != null) {
					// When receiver received the end_of_session package, it will send end_of_session back to make sure 
					// socket will be closed on the other side and then closed socket after certain time.
					if(p.getType().equals(PackageType.END_SESSION)){
						ProtocolPackage endsession = new ProtocolPackage(PackageType.END_SESSION, 
		    					name);
		    			endsession.sendPackage(s.getOutputStream());
			    		System.out.println(name + " send package whose type is " + endsession.getType());
						Thread.sleep(WAIT_TIME_TO_DIE);
						System.out.println("Receiver " + name + " exit.");
						throw new SocketException();
					}
					packagehandler.handlePackage(p);
				}
			}catch(SocketException e){
				if(s.isConnected())
					try {
						s.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			}		
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
