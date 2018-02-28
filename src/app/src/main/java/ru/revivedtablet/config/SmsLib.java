package ru.revivedtablet.config;

import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import ru.revivedtablet.RevivedTabletApp;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SmsLib extends TwoArgFunction {
    private static final Map<String, Date> livedSms =
            new ConcurrentHashMap<String, Date>();

    private static boolean canSendSms;

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        canSendSms = RevivedTabletApp.getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        LuaValue library = tableOf();
        library.set("send", new SmsLib.SendMethod() );
        library.set("receiver", NIL);
        env.set("sms", library );
        return library;
    }

    static class SendMethod extends ThreeArgFunction {
        public LuaValue call(LuaValue phoneNumber, LuaValue message, LuaValue liveInHours) {
            if (phoneNumber.isnil() || message.isnil() || !canSendSms)
                return FALSE;

            //Delete expired sms
            Date now = new Date();
            for (String key: livedSms.keySet())
                if (livedSms.get(key).getTime() < now.getTime())
                    livedSms.remove(key);

            if (!liveInHours.isnil() && liveInHours.toint() > 0) {
                String key = phoneNumber.tojstring() + message.tojstring();
                if (livedSms.containsKey(key))
                    return FALSE;
                else
                    livedSms.put(key, new Date(now.getTime() + 3600 * 1000 * liveInHours.toint()));
            }

            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message.tojstring());
            sms.sendMultipartTextMessage(phoneNumber.tojstring(), null, parts, null, null);
            Log.d("Send SMS to " + phoneNumber.tojstring(), message.tojstring());
            return TRUE;
        }
    }

}
