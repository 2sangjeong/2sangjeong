package com.lge.asr.extractor.vo;

import java.util.Date;

public class UserDataVo {
	private Date gmTime = null; // gmt
	private String contactData = null; // user contacts data
	private String additionalData = null;   // user additional data
	private String userId  = null;
	private String password = null;
	private String userName = null;
	private String email = null;
	private String date = null;
	private String region = null;
	private String command = null;
	private String ip = null;
	
	public Date getGmTime() {
		return gmTime;
	}
	public void setGmTime(Date time) {
		this.gmTime = time;
	}
	public String getContactData() {
		return contactData;
	}
	public void setContactData(String contactData) {
		this.contactData = contactData;
	}
	public String getAdditionalData() {
		return additionalData;
	}
	public void setAdditionalData(String additionalData) {
		this.additionalData = additionalData;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	@Override
	public String toString() {
		return "UserDataVo [gmTime=" + gmTime + ", contactData=" + contactData + ", additionalData=" + additionalData
				+ ", userId=" + userId + ", password=" + password + ", userName=" + userName + ", email=" + email
				+ ", date=" + date + ", region=" + region + ", command=" + command + ", ip=" + ip + "]";
	}
	
	

	
}
