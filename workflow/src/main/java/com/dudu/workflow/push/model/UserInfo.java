package com.dudu.workflow.push.model;

/**
 * Created by Administrator on 2016/2/16.
 */
public class UserInfo {

    private String userName;
    private String passWord;
    private String pushId;
    private String platform;

    public UserInfo(String userName, String passWord, String pushId,String platform) {
        this.userName = userName;
        this.passWord = passWord;
        this.pushId = pushId;
        this.platform=platform;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
