package org.rankun.onlineteach.android;

import android.app.Application;

/**
 * Created by mindfine on 13-10-5.
 */
public class AndroidApplication extends Application {
    private String SERVER_URL = "http://rankun.tk:8080/OnlineTeach/";

    public String getSERVER_URL() {
        return SERVER_URL;
    }

    public void setSERVER_URL(String SERVER_URL) {
        this.SERVER_URL = SERVER_URL;
    }
}
