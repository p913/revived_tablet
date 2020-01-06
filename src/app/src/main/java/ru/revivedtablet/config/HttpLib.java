package ru.revivedtablet.config;


import android.util.Log;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpLib extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("get", new HttpGetMethod() );
        library.set("post", new HttpPostMethod() );
        library.set("request", new HttpRequestMethod() );
        library.set("encodeurl", new EncodeUrlMethod() );
        env.set("http", library );
        return library;
    }

    static class HttpGetMethod extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue url) {
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection)(new URL(url.tojstring())).openConnection();
                urlConnection.setUseCaches(false);
                //Log.d("Http GET", "Response code: " + urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    try {
                        String content;
                        StringBuilder sb = new StringBuilder();
                        for (int c; (c = br.read()) >= 0;)
                            sb.append((char)c);
                        content = sb.toString();
                        //Log.d("Http GET", "Content: " + content);
                        return LuaValue.valueOf(content);
                    } finally {
                        br.close();
                    }
                }
            } catch (IOException e) {
                Log.e("Http GET error", url.toString(),  e);
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return NIL;
        }
    }

    static class HttpPostMethod extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue url, LuaValue data) {
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection)(new URL(url.tojstring())).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("charset", "utf-8");
                urlConnection.setUseCaches(false);

                if (data != null && !data.isnil()) {
                    byte[] postData = data.tojstring().getBytes("UTF-8");
                    int postDataLength = postData.length;

                    urlConnection.setRequestProperty("Content-Length", Integer.toString( postDataLength ));

                    urlConnection.getOutputStream().write(postData);
                }
                else
                    urlConnection.setRequestProperty("Content-Length", "0");

                //Log.d("Http POST", "Response code: " + urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    try {
                        String content;
                        StringBuilder sb = new StringBuilder();
                        for (int c; (c = br.read()) >= 0;)
                            sb.append((char)c);
                        content = sb.toString();
                        //Log.d("Http POST", "Content: " + content);
                        return LuaValue.valueOf(content);
                    } finally {
                        br.close();
                    }
                }
            } catch (IOException e) {
                Log.e("Http POST error", url.toString(), e);
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return NIL;
        }
    }

    static class HttpRequestMethod extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            String method = args.isnil(1) ? null : args.tojstring(1);
            String url = args.isnil(2) ? null : args.tojstring(2);
            String data = args.isnil(3) ? null : args.tojstring(3);
            LuaTable headers = args.istable(4) ? args.checktable(4) : null;
            if (method != null && url != null) {
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection)(new URL(url)).openConnection();
                    urlConnection.setRequestMethod(method);
                    urlConnection.setRequestProperty("charset", "utf-8");
                    urlConnection.setUseCaches(false);

                    if (headers != null) {
                        for (LuaValue key: headers.keys())
                            urlConnection.setRequestProperty(key.tojstring(), headers.get(key).tojstring());
                    }

                    if (data != null) {
                        urlConnection.setDoOutput(true);

                        byte[] postData = data.getBytes("UTF-8");
                        int postDataLength = postData.length;

                        urlConnection.setRequestProperty("Content-Length", Integer.toString( postDataLength ));

                        urlConnection.getOutputStream().write(postData);
                    }

                    //Log.d("Http request " + method, "Response code: " + urlConnection.getResponseCode());
                    BufferedReader br;
                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
                        br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    else
                        br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), "UTF-8"));
                    try {
                        String content;
                        StringBuilder sb = new StringBuilder();
                        for (int c; (c = br.read()) >= 0;)
                            sb.append((char)c);
                        content = sb.toString();
                        //Log.d("Http request " + method, "Content: " + content);

                        LuaTable respHeaders = LuaTable.tableOf();
                        for (String key: urlConnection.getHeaderFields().keySet()) {
                            List<String> values = urlConnection.getHeaderFields().get(key);
                            if (key == null) {
                                for (String v : values)
                                    respHeaders.set(v, LuaValue.NIL);
                            } else {
                                if (values.size() == 1)
                                    respHeaders.set(key, values.get(0));
                                else if (values.size() == 0)
                                    respHeaders.set(key, LuaValue.NIL);
                                else {
                                    LuaTable respHeadersForKey = LuaTable.tableOf();
                                    int i = 1;
                                    for (String v : values)
                                        respHeadersForKey.set(i++, v);
                                    respHeaders.set(key, respHeadersForKey);
                                }
                            }
                        }

                        return LuaValue.varargsOf(new LuaValue[] {
                                LuaValue.valueOf(urlConnection.getResponseCode()),
                                LuaValue.valueOf(content),
                                respHeaders });
                    } finally {
                        br.close();
                    }
                } catch (IOException e) {
                    Log.e("Http request error", method + " " + url, e);
                    return LuaValue.varargsOf(new LuaValue[] {
                            LuaValue.valueOf(0),
                            LuaValue.valueOf(e.getMessage()),
                            LuaValue.NIL
                    });

                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
            return LuaValue.varargsOf(new LuaValue[] {
                    LuaValue.valueOf(0),
                    LuaValue.valueOf("Bad request parameters"),
                    LuaValue.NIL
            });
        }
    }

    private class EncodeUrlMethod extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            try {
                return LuaValue.valueOf(URLEncoder.encode(arg.tojstring(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
