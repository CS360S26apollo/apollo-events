package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;

import java.text.NumberFormat;

/**
 * US-25: Purchase Success screen.
 * Shown after tokens are successfully credited to the student's account.
 */
public class PurchaseSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_success);

        int    tokens     = getIntent().getIntExtra("tokens", 0);
        long   newBalance = getIntent().getLongExtra("newBalance", 0);
        String label      = getIntent().getStringExtra("label");

        NumberFormat nf = NumberFormat.getNumberInstance();

        setText(R.id.tvPurchasedTokens, nf.format(tokens));
        setText(R.id.tvNewBalance,      nf.format(newBalance));
        setText(R.id.tvPackageLabel,    label != null ? label + " Package" : "Tokens");

        Button btnHome = findViewById(R.id.btnGoHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        Button btnBrowse = findViewById(R.id.btnBrowseTutors);
        if (btnBrowse != null) {
            btnBrowse.setOnClickListener(v -> {
                startActivity(new Intent(this, BrowseTutorsActivity.class));
                finish();
            });
        }
    }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }
}