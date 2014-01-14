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
    
    
    MetaRequester mMetaRequester = null;
    PackageServer mPackageServer = null;
	
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
        
        mNsdHelper = new NsdHelper(this, queue);
        mNsdHelper.initializeNsd();
        
        mPackageServer = new PackageServer();
        mMetaRequester = new MetaRequester(queue);  
    }

    public void clickAdvertise(View v) {
        // Register service
        if(mPackageServer.getPort() > -1) {
            mNsdHelper.registerService(mPackageServer.getPort());
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
        mPackageServer.tearDown();
        super.onDestroy();
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
    
    private class PackageServer {
    	ServerSocket mServerSocket = null;
    	Thread mThread = null;
    	
    	public PackageServer() {
    		mThread = new Thread(new ServerPackageThread());
    		mThread.start();
    	}
    	
    	public void tearDown() {
            mThread.interrupt();
            try {
                mServerSocket.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }
    	
    	public int getPort() {
        	if(mServerSocket == null)
        		return -1;
        	return mServerSocket.getLocalPort();
        }
    	
    	class ServerPackageThread implements Runnable{
            public void run() {
            	try {
            		mServerSocket = new ServerSocket(0);
            	while (!Thread.currentThread().isInterrupted()) {                  
                	Log.d(TAG, "ServerSocket Created, awaiting connection");
    				Socket connector = mServerSocket.accept();	
    				Log.d(TAG, "received request.");
    				PackageHandler handler = new PackageHandler(connector, mNsdHelper.getSelfName());
    				handler.handle();
            		}
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            }
        }
    }
    
    
    private class MetaRequester {
    	private BlockingQueue<NsdServiceInfo> queue = null;
    	private Socket mSocket = null;
    	private Thread mThread = null;
    	
    	public MetaRequester(BlockingQueue<NsdServiceInfo> q) {
    		queue = q;
    		mThread = new Thread(new RequestMetaThread());
    		mThread.start();
    	}
    	
    	public void tearDown() {
            mThread.interrupt();
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
        
        class RequestMetaThread implements Runnable {
            @Override
            public void run() {
            	while(true) {
            		try {           			
            			NsdServiceInfo next_device = queue.take();
            			Log.d(TAG, "send request to device " + next_device.getServiceName());
                		setSocket(new Socket(next_device.getHost(), next_device.getPort()));
                		ProtocolPackage requst_meta_package = new ProtocolPackage(PackageType.REQUEST_META, 
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
}
