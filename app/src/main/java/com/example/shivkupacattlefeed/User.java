package com.example.shivkupacattlefeed;

public class User {
    public String name;
    public String role;
    public String mobile;
    public String password;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String name, String role, String mobile, String password) {
        this.name = name;
        this.role = role;
        this.mobile = mobile;
        this.password = password;
    }
}
