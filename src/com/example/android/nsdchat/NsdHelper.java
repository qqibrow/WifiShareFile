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

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
public class NsdHelper {
    Context mContext;
    HashMap<String, NsdServiceInfo> name2NsdInfo = new HashMap<String, NsdServiceInfo>();
    
    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public String mServiceType;
    public String mServiceName;
    private String selfName;
    private BlockingQueue<NsdServiceInfo> queue = null;
    
    
    public NsdHelper(Context context, BlockingQueue<NsdServiceInfo> queue) {
        mContext = context;
        
        this.queue = queue;
        mServiceType = "PIFI_WIFI";
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        
    	WifiManager wifiMan = (WifiManager) mContext.getSystemService(
                Context.WIFI_SERVICE);
    	WifiInfo wifiInf = wifiMan.getConnectionInfo();
    	selfName = wifiInf.getMacAddress();
        mServiceName = selfName + mServiceType;
    }   
    
    public String getSelfName() {
    	return selfName;
    }
    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);

    }
    
    private String getDeviceNameFromService(NsdServiceInfo service) {
    	String mydata = service.getServiceName();
    	Pattern pattern = Pattern.compile("^(.*?)" + mServiceType);
    	Matcher matcher = pattern.matcher(mydata);
    	if (matcher.find())
    	{
    	    return matcher.group(1);
    	}
    	return null;
    }
    
    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
            	Log.d(TAG, "Service discovery success\n" + service);
            	String full_service_name = service.getServiceName();
            	if(full_service_name == mServiceName) {
            		Log.v(TAG, "not add myself in the list.");
            		return;
            	}
            	String device_name = getDeviceNameFromService(service);
            	if(device_name == null) {
            		Log.v(TAG, "device name cannot be resolved.");
            		return;
            	}            	
            	if(!name2NsdInfo.containsKey(device_name) || full_service_name.matches("\\(\\d\\)$")) {
            		Log.d(TAG, "Service name need to be added or updated.");
            		name2NsdInfo.put(device_name, service);            		
            		// TODO the resolved service will get updated in the map.
            		mNsdManager.resolveService(service, mResolveListener);
            	}else
            		Log.d(TAG, "Service doesn't need to be updated.");
               
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                String device_name = getDeviceNameFromService(service);
                name2NsdInfo.remove(device_name);
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);        
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
            	String device_name = getDeviceNameFromService(serviceInfo);
                Log.e(TAG, "Resolve Succeeded" +device_name + ".\n"  + serviceInfo);
                name2NsdInfo.put(device_name, serviceInfo);
                Log.d(TAG, "send package of request meta data.");
                try {
                	queue.put(serviceInfo);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }               
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
                Log.v(TAG, "service registered with name" + mServiceName);
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }
            
        };
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        
    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }
    
    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }
    
    public void tearDown() {
    	if(mRegistrationListener != null)
    		mNsdManager.unregisterService(mRegistrationListener);
    }
}
