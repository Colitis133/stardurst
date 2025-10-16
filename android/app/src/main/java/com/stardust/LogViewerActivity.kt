package com.stardust;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LogViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        TextView logTextView = findViewById(R.id.logTextView);
        logTextView.setText(getLogs());
    }

    private String getLogs() {
        File logFile = new File(getFilesDir(), "runtime/logs/stdout.log");
        if (!logFile.exists()) {
            return "No logs found.";
        }
        try {
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            return text.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading logs.";
        }
    }
}
