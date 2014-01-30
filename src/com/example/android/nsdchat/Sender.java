package com.example.android.nsdchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

class Sender {
	String name;
	Socket s = null;
	PackageHandler packagehandler = null;
	
	public Sender(Socket s, String name) {
		System.out.println("Thread handler built");
		this.name = name;
		this.s = s;
		packagehandler = new PackageHandler(s, name);
	}
	
	public void start() {
		Thread handler = new Thread(new FeedbackReceiver());
		Thread request = new Thread(new SendRequestThread());
		handler.start();
		request.start();
		
	}
	
	// The only Responsible for this thread is to send REQUEST_META package to the other side.
	class SendRequestThread implements Runnable {
		public void run() {
				try {
						OutputStream out = s.getOutputStream();											
						ProtocolPackage requst_meta_package = new ProtocolPackage(PackageType.REQUEST_META, 
        	    				name);
        	    		requst_meta_package.sendPackage(out);
        	    		System.out.println(name + " send package whose type is " + requst_meta_package.getType());
					}catch(SocketException e){
						synchronized(s) {
							if(s.isConnected())
								try {
									s.close();
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
						}			
					}	
					catch(Exception e) {
					e.printStackTrace();
				}
		}
	}
	
	// FeedbackReceiver waits for feedback after sending the initial message. Once a specific feedback is received,
	// it makes certain response. Finally, it will exit after received END_SESSION message.
	class FeedbackReceiver implements Runnable {
		public void run() {
			try {
				InputStream in = s.getInputStream();
				ProtocolPackage p = null;
				while((p = ProtocolPackage.receivePackage(in)) != null) {
					if(p.getType().equals(PackageType.END_SESSION)){
						System.out.println("HandlerServerThread exit");
						throw new SocketException();
					}
					System.out.println(name + " received package whose type is " + p.getType());
					packagehandler.handlePackage(p);							
				}
			}catch(SocketException e){
				synchronized(s) {
					if(s.isConnected())
						try {
							s.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
				}			
			}		
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}