package com.varuncjain.behancemuzei;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PreferenceHelper {

    public static final int CONNECTION_WIFI = 0;
    public static final int CONNECTION_ALL = 1;

    public static final int MIN_FREQ_MILLIS = 3 * 60 * 60 * 1000;
    private static final int DEFAULT_FREQ_MILLIS = 24 * 60 * 60 * 1000;

    public static final int USERS_POPULAR_OFF = 0;
    public static final int USERS_POPULAR_ON = 1;

    public static int getConfigConnection(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt("config_connection", CONNECTION_WIFI);
    }

    public static void setConfigConnection(Context context, int connection) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putInt("config_connection", connection).apply();
    }

    public static void setConfigFreq(Context context, int durationMillis) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putInt("config_freq", durationMillis).apply();
    }

    public static int getConfigFreq(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt("config_freq", DEFAULT_FREQ_MILLIS);
    }

    public static void limitConfigFreq(Context context) {
        int configFreq = getConfigFreq(context);
        if(configFreq < MIN_FREQ_MILLIS) {
            setConfigFreq(context, MIN_FREQ_MILLIS);
        }
    }

    public static int getConfigPopular(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt("config_popular", USERS_POPULAR_ON);
    }

    public static void setConfigPopular(Context context, int popular) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putInt("config_popular", popular).apply();
    }

    public static List<String> userNamesFromPref(Context context) {
        ArrayList<String> projects = new ArrayList<String>();
        SharedPreferences preferences = getPreferences(context);
        String prefProjects = preferences.getString("names", "[\"gahfe\", \"selinozgur\"]");
        if (!TextUtils.isEmpty(prefProjects)) {
            try {
                JSONArray jsonArray = new JSONArray(prefProjects);
                for(int index = 0 ; index < jsonArray.length() ; index++) {
                    projects.add(jsonArray.getString(index));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return projects;
    }

    public static void userNamesToPref(Context context, List<String> projects) {
        SharedPreferences preferences = getPreferences(context);
        JSONArray jsonArray = new JSONArray(projects);
        preferences.edit().putString("names", jsonArray.toString()).apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("user", Context.MODE_PRIVATE);
    }
}