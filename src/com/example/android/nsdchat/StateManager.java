package com.example.android.nsdchat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.net.nsd.NsdServiceInfo;

public class StateManager {
	private HashMap<String, Status> devicelist;	
	private static final StateManager INSTANCE = new StateManager();
	private StateManager() {}
	
	public static StateManager getInstance() {
        return INSTANCE;
    }
	
	// Get a list of all not SynDevices. return null if all synced.
	List<NsdServiceInfo> getNotSyncDevices() {
		List<NsdServiceInfo> list = new ArrayList<NsdServiceInfo>();
		return list;
	}
	
	synchronized void addDevice(String sender, NsdServiceInfo nsdService) {
		
	}
	
	synchronized void finished(String sender) {
		
	}
	
	void changeState(String sender, DeviceSyncState state) {
		
	}
	
	
	
	// get next not sync device. retur null if all synced.
	NsdServiceInfo getNextNotSynDevices() {
		return null;
	}
	
	
	
	class Status {
		NsdServiceInfo nsdservice;
		DeviceSyncState state;
	}
}
