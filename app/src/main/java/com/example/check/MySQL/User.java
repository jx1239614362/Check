package com.example.check.MySQL;

import java.util.Date;

public class User {
    private String username="";
    private String passward="";
    private String address="";
    private Date date;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassward() {
        return passward;
    }

    public void setPassward(String passward) {
        this.passward = passward;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    public String getUser(){
        return getUsername()+getPassward()+getAddress()+getDate();
    }
}
