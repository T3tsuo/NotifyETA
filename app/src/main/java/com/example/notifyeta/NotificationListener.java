package com.example.notifyeta;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


public class NotificationListener extends NotificationListenerService {


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();
        if (pack.equals("com.google.android.apps.maps")) {
            Bundle extras = sbn.getNotification().extras;
            CharSequence text = extras.getCharSequence("android.subText");


            Intent intent = new Intent("com.example.notifyeta");
            intent.putExtra("NotificationCode", 1);
            intent.putExtra("text", text);

            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();

        if (pack.equals("com.google.android.apps.maps")) {
            Intent intent = new Intent("com.example.notifyeta");
            intent.putExtra("text", "finished");
            sendBroadcast(intent);
        }
    }
}
