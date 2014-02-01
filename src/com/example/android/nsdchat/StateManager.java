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
		devicelist.put(sender, new Status(DeviceSyncState.ADDED, nsdService));
	}
	
	synchronized void finished(String sender) {
		Status status = devicelist.get(sender);
		if(status != null)
			status.state = DeviceSyncState.FINISHED;
	}
	
	void changeState(String sender, DeviceSyncState state) {
		
	}	
	
	// get next not sync device. retur null if all synced.
	NsdServiceInfo getNextNotSynDevices() {
		for(Status status : devicelist.values()) {
			if(status.state == DeviceSyncState.ADDED)
				return status.nsdservice;
		}
		return null;
	}	
	
	class Status {
		NsdServiceInfo nsdservice;
		DeviceSyncState state;
		public Status(DeviceSyncState s, NsdServiceInfo n) {
			this.state = s;
			this.nsdservice = n;
		}
	}
}
