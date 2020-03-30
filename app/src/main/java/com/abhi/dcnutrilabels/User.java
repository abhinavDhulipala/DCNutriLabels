package com.abhi.dcnutrilabels;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    private String username, email;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {}

    public User(String username, String email) {

        this.username = username;
        this.email = email;
    }


}
