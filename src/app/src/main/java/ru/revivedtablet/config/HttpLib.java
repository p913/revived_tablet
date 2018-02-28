package ru.revivedtablet.config;


import android.util.Log;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpLib extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("get", new HttpGetMethod() );
        library.set("post", new HttpPostMethod() );
        env.set("http", library );
        return library;
    }

    static class HttpGetMethod extends OneArgFunction {
        public LuaValue call(LuaValue url) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection)(new URL(url.tojstring())).openConnection();
                Log.d("Http GET", "Response code: " + urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    try {
                        String content = br.readLine();
                        Log.d("Http GET", "Content: " + content);
                        return LuaValue.valueOf(content);
                    } finally {
                        br.close();
                    }
                }
            } catch (IOException e) {
                Log.e("Http GET error", e.getMessage());
            }
            return NIL;
        }
    }

    static class HttpPostMethod extends OneArgFunction {
        public LuaValue call(LuaValue url) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection)(new URL(url.tojstring())).openConnection();
                urlConnection.setRequestMethod("POST");
                Log.d("Http POST", "Response code: " + urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    try {
                        String content = br.readLine();
                        Log.d("Http POST", "Response content: " + content);
                        return LuaValue.valueOf(content);
                    } finally {
                        br.close();
                    }
                }
            } catch (IOException e) {
                Log.e("Http POST error", e.getMessage());
            }
            return NIL;
        }
    }

}
