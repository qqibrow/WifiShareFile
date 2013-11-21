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

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChatConnection {

    private Handler mUpdateHandler;
    private ChatServer mChatServer;
    private ChatClient mChatClient;

    private static final String TAG = "ChatConnection";

    private Socket mSocket;
    private int mPort = -1;

    public ChatConnection(Handler handler) {
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
    }

    public void tearDown() {
        mChatServer.tearDown();
        mChatClient.tearDown();
    }

    public void connectToServer(InetAddress address, int port) {
        mChatClient = new ChatClient(address, port);
    }

    public void sendMessage(String msg) {
        if (mChatClient != null) {
            mChatClient.sendFile(msg);
        }
    }
    
    public int getLocalPort() {
        return mPort;
    }
    
    public void setLocalPort(int port) {
        mPort = port;
    }
    

    public synchronized void updateMessages(String msg, boolean local) {
        Log.e(TAG, "Updating message: " + msg);

        if (local) {
            msg = "me: " + msg;
        } else {
            msg = "them: " + msg;
        }

        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);

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
                    // TODO(alexlucas): Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    private Socket getSocket() {
        return mSocket;
    }

    private class ChatServer {
        ServerSocket mServerSocket = null;
        Thread mThread = null;

        public ChatServer(Handler handler) {
            mThread = new Thread(new ServerThread());
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

        class ServerThread implements Runnable {

            @Override
            public void run() {

                try {
                    // Since discovery will happen via Nsd, we don't need to care which port is
                    // used.  Just grab an available one  and advertise it via Nsd.
                    mServerSocket = new ServerSocket(0);
                    setLocalPort(mServerSocket.getLocalPort());
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        setSocket(mServerSocket.accept());
                        Log.d(TAG, "Connected.");
                        if (mChatClient == null) {
                            int port = mSocket.getPort();
                            InetAddress address = mSocket.getInetAddress();
                            connectToServer(address, port);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            }
        }
    }

    private class ChatClient {

        private InetAddress mAddress;
        private int PORT;

        private final String CLIENT_TAG = "ChatClient";
        private final String DIRECTORY = "DATA";
        private Thread mSendThread;
        private Thread mRecThread;

        public ChatClient(InetAddress address, int port) {

            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        class SendingThread implements Runnable {

            BlockingQueue<String> mMessageQueue;
            private int QUEUE_CAPACITY = 10;

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    if (getSocket() == null) {
                        setSocket(new Socket(mAddress, PORT));
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");

                    } else {
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                    }

                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                } catch (IOException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                }

                while (true) {
                    try {
                        String msg = mMessageQueue.take();
                        sendMessage(msg);
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }

        class ReceivingThread implements Runnable {

            @Override
            public void run() {

               // BufferedReader input;
                try {
//                    input = new BufferedReader(new InputStreamReader(
//                            mSocket.getInputStream()));
//	                    
//                    while (!Thread.currentThread().isInterrupted()) {
//
//                        String messageStr = null;
//                        messageStr = input.readLine();
//                        if (messageStr != null) {
//                            Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);
//                            updateMessages(messageStr, false);
//                        } else {
//                            Log.d(CLIENT_TAG, "The nulls! The nulls!");
//                            break;
//                        }
//                    }
//                    input.close();
                	
                	DataInputStream socket_is = new DataInputStream(mSocket.getInputStream());
                	String file_name = socket_is.readUTF();
                	long file_length = socket_is.readLong();
                	
                	//BufferedInputStream socket_buffer_is = new BufferedInputStream(mSocket.getInputStream());
                	File new_file = new File(Environment.getExternalStorageDirectory(), '/' +DIRECTORY + '/'  + file_name);                	
                	FileOutputStream file_os = new FileOutputStream(new_file);
                	DataOutputStream file_data_os = new DataOutputStream(file_os);
                	long bytesReaded = 0;              	
                	byte[] bytes = new byte[8 * 1024];
                	while(bytesReaded < file_length) {
                		int readed = socket_is.read(bytes);
                		bytesReaded += readed;
                		file_data_os.write(bytes, 0, readed);
                	}
                	file_data_os.close(); 
                	Log.v(TAG, "file received finished");
                } catch (IOException e) {
                    Log.e(CLIENT_TAG, "Server loop error: ", e);
                }
            }
        }

        public void tearDown() {
            try {
                getSocket().close();
            } catch (IOException ioe) {
                Log.e(CLIENT_TAG, "Error when closing server socket.");
            }
        }

        public void sendMessage(String msg) {
            try {
                Socket socket = getSocket();
                if (socket == null) {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                } else if (socket.getOutputStream() == null) {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }

                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(getSocket().getOutputStream())), true);
                out.println(msg);
                out.flush();
                updateMessages(msg, true);
            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Unknown Host", e);
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "I/O Exception", e);
            } catch (Exception e) {
                Log.d(CLIENT_TAG, "Error3", e);
            }
            Log.d(CLIENT_TAG, "Client sent message: " + msg);
        }
        
        public void sendFile(String file_name) {
        	try {
        		Socket socket = getSocket();
        		if(socket == null) {
        			Log.d(CLIENT_TAG, "Socket is null, wtf?");
        		}else if(socket.getOutputStream() == null) {
        			Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
        		}
        		
        		// send file.
        		File file = new File(Environment.getExternalStorageDirectory(),
        				'/' + DIRECTORY + '/' + file_name);
        		
        		OutputStream os = socket.getOutputStream();
        		DataOutputStream socket_data_os = new DataOutputStream(os);  
        
        		byte[] buffer = new byte[8 * 1024];
        		socket_data_os.writeUTF(file.getName());
        	
        		socket_data_os.flush();
        		
        		long file_length = (long)file.length();
        		socket_data_os.writeLong(file_length);
        		socket_data_os.flush();
        		
        		FileInputStream fis = new FileInputStream(file.toString());   
        		DataInputStream bis = new DataInputStream(fis);    
        		
        		int readed = 0;
        		while(readed != -1) {
        			readed = bis.read(buffer);
        			socket_data_os.write(buffer, 0, readed);
        			socket_data_os.flush();
        		}
        		Log.v(TAG, "file send finished");
        		bis.close();
        		
        	}catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Unknown Host", e);
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "I/O Exception", e);
            } catch (Exception e) {
                Log.d(CLIENT_TAG, "Error3", e);
            }
            Log.d(CLIENT_TAG, "Client sent message: " + file_name); 
        }
    }
}
