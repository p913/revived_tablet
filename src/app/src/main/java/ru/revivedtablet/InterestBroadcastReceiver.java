package ru.revivedtablet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import ru.revivedtablet.config.Configuration;

public class InterestBroadcastReceiver extends BroadcastReceiver {
	public InterestBroadcastReceiver() {
	}

	
	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {    

            Intent myStarterIntent = new Intent(context, TabletCanvasActivity.class);
            myStarterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myStarterIntent);

        } else if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			String phoneNumber = null;
			if (bundle != null){
				try{
					StringBuilder sb = new StringBuilder();
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];
					for(int i = 0; i < msgs.length; i++){
						msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
						phoneNumber = msgs[i].getOriginatingAddress();
						sb.append(msgs[i].getMessageBody());
					}
					Configuration.getInstance().notifySmsReceived(phoneNumber, sb.toString());
				} catch(Exception e){
					Log.d("Sms receive error", e.getMessage());
				}
			}

		}
	}
}
