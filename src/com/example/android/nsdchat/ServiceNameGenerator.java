package com.example.android.nsdchat;

import java.util.UUID;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class ServiceNameGenerator {
	
	private String service_name;
	
	public ServiceNameGenerator(String service_name) {
		this.service_name = service_name;
	}
	public String getString() {
			WifiManager wifiMan = (WifiManager) this.getSystemService(
	                Context.WIFI_SERVICE);
	WifiInfo wifiInf = wifiMan.getConnectionInfo();
	String macAddr = wifiInf.getMacAddress();
		return macAddr.toString() + service_name;
	}
	public boolean matchPattern(String name) {
		return name.matches(service_name + '$');
	}
}
