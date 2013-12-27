/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.nsdchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.nsdchat.NsdHelper;

public class NsdChatActivity extends Activity {

    NsdHelper mNsdHelper;

    private TextView mStatusView;
    private Handler mUpdateHandler;

    public static final String TAG = "NsdChat";

    
    private int QUEUE_CAPACITY = 100;
    private BlockingQueue<NsdServiceInfo> queue = new ArrayBlockingQueue<NsdServiceInfo>(QUEUE_CAPACITY);
    RequestMetaThread request_meta_thread;
    ServerPackageThread serverthread;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatusView = (TextView) findViewById(R.id.status);

        mUpdateHandler = new Handler() {
                @Override
            public void handleMessage(Message msg) {
                String chatLine = msg.getData().getString("msg");
                addChatLine(chatLine);
            }
        };
        
        serverthread = new ServerPackageThread();
        serverthread.run();
        
        mNsdHelper = new NsdHelper(this, queue);      
        request_meta_thread = new RequestMetaThread(queue);
        request_meta_thread.run();
        
        
        
        mNsdHelper.initializeNsd();

    }

    public void clickAdvertise(View v) {
        // Register service
        if(serverthread.getPort() > -1) {
            mNsdHelper.registerService(serverthread.getPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }

    public void clickDiscover(View v) {
        mNsdHelper.discoverServices();
    }

    public void clickConnect(View v) {
    	Log.v("clickConnect", "Do nothing here");
    }
    
    public void clickDisconnect(View w) {
    	try{
	        mNsdHelper.tearDown();
    	}
    	catch(Exception e){
    		Log.d(TAG,e.getStackTrace().toString());
    	}
    }

    public void clickSend(View v) {
    	Log.v("clickSend", "Do nothing here");
    }

    public void addChatLine(String line) {
    	Log.v("addchatLine", "Do nothing here");
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }
    
    @Override
    protected void onDestroy() {
    	Log.v(TAG, "on destroy");
        mNsdHelper.tearDown();
        super.onDestroy();
    }
    
    class PackageHandlerThread implements Runnable {   	
    	private Socket mSocket;
    	public PackageHandlerThread(Socket socket) {
    		this.mSocket = socket;
    	} 	
    	public void run() {
    		try {
    			ProtocolPackage p = ProtocolPackage.receivePackage(mSocket.getInputStream());
    			PackageHandler pHandler = new PackageHandler();
    			pHandler.setSocket(mSocket);   			
    			String external = Environment.getExternalStorageDirectory().getPath();
    			String metaPath = external + "/Pifitest/meta.dat";
    			String transferDirectory = external + "Pifitest/" + p.getSender();
    			String selfName = mNsdHelper.getSelfName();
        		pHandler.setMetaPath(metaPath);
        		pHandler.setTransferDirectory(transferDirectory);
        		pHandler.setSender(selfName);
    			pHandler.handlePackage(p);  			
    		}catch(Exception e) {
    			e.printStackTrace();
    		}   		    			
    	}
    }
    
    protected void DiscoverDevices()
    {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(new Runnable() {
              @Override
              public void run() {
               mNsdHelper.discoverServices();
              }
            }, 0, 10, TimeUnit.SECONDS);
    }
    
    class ServerPackageThread implements Runnable{
        
        private ServerSocket  mSocket;
        
        public int getPort() {
        	if(mSocket == null)
        		return -1;
        	return mSocket.getLocalPort();
        }
        
        public ServerPackageThread() {
        	try {
        		mSocket = new ServerSocket(0);
        	}catch(IOException e) {
        		Log.e(TAG, "Error creating ServerSocket", e);
        		e.printStackTrace();
        	}     	
        }
        
        public void run() {
        	while (!Thread.currentThread().isInterrupted()) {
               try {
				Socket connector = mSocket.accept();	
				Runnable eventHandler = new PackageHandlerThread(connector);
				eventHandler.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
               
        	}
        }
    }
    
    class RequestMetaThread implements Runnable {

        private BlockingQueue<NsdServiceInfo> queue;
        private Socket mSocket;
        
        public RequestMetaThread(BlockingQueue<NsdServiceInfo> queue) {
            this.queue = queue;
        }
        
        private synchronized void setSocket(Socket socket) {
            Log.d(TAG, "setSocket being called.");
            if (socket == null) {
                Log.d(TAG, "Setting a null socket.");
            }
            if (mSocket != null) {
                if (mSocket.isConnected()) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mSocket = socket;
        }
        private Socket getSocket() {
            return mSocket;
        }       

        @Override
        public void run() {
        	while(true) {
        		try {
        			NsdServiceInfo next_device = queue.take();
            		setSocket(new Socket(next_device.getHost(), next_device.getPort()));
            		ProtocolPackage requst_meta_package = new ProtocolPackage(PackageType.SEND_META, 
            				mNsdHelper.getSelfName());
            		requst_meta_package.sendPackage(getSocket().getOutputStream());            		
            		// TODO need to close the socket?
            		mSocket.close();           		
        		}catch(Exception e) {
        			e.printStackTrace();
        		}       		  		
        	}
        }
    }
    
}
