package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * US-24: Withdrawal success confirmation screen.
 * Displays transaction ID, method, processing time, and date.
 */
public class WithdrawSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_success);

        int    amount         = getIntent().getIntExtra("amount", 0);
        String method         = getIntent().getStringExtra("method");
        String processingTime = getIntent().getStringExtra("processingTime");

        // Generate transaction ID
        String txId = "WD-" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date()) + "-" + amount;

        String dateTime = new SimpleDateFormat("MMMM d, yyyy, h:mm a", Locale.getDefault())
                .format(new Date());

        setText(R.id.tvWithdrawAmount, NumberFormat.getNumberInstance().format(amount));
        setText(R.id.tvTransactionId,  txId);
        setText(R.id.tvMethodName,     method != null ? method : "Bank Transfer");
        setText(R.id.tvProcessingTime, processingTime != null ? processingTime : "2-3 business days");
        setText(R.id.tvDateTime,       dateTime);

        Button btnBack = findViewById(R.id.btnBackToDashboard);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorEarningsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }
}