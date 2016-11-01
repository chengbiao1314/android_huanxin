package com.cb.hxim_library.domain;

/**
 * Created by Ricky on 2016/11/1.
 */
public class HXUser {
    private String userId;//环信ID
    private String password;//环信ID对应的环信服务器密码
    private String nickname;//昵称

    private String targetUserId;//聊天对象的id

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
}
