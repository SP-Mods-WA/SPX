package com.spmods.spx.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.spmods.spx.AudioService;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        Intent svcIntent = new Intent(context, AudioService.class);
        svcIntent.setAction(action);
        context.startService(svcIntent);
    }
}
