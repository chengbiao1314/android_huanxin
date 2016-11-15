package com.cb.hxim_library.domain;

import java.io.Serializable;

/**
 * Created by Ricky on 2016/11/1.
 */
public class HXUser implements Serializable{
    private String userId;//环信ID
    private String password;//环信ID对应的环信服务器密码
    private String name;//名字

    private String targetUserId;//聊天对象的id
    private PageEnum targetType;//聊天对象的类型

    //如果是客服
    private String CSGroupID;//技能组
    private String CSAgentID;//具体客服

    private String nickname;//昵称
    private String qq;//QQ
    private String phone;//手机
    private String companyName;//公司名称
    private String description;//用户描述
    private String email;//邮箱

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public PageEnum getTargetType() {
        return targetType;
    }

    public void setTargetType(PageEnum targetType) {
        this.targetType = targetType;
    }

    public String getCSGroupID() {
        return CSGroupID;
    }

    public void setCSGroupID(String CSGroupID) {
        this.CSGroupID = CSGroupID;
    }

    public String getCSAgentID() {
        return CSAgentID;
    }

    public void setCSAgentID(String CSAgentID) {
        this.CSAgentID = CSAgentID;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
