package com.example.peertutoring.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.example.peertutoring.ui.BuyTokensActivity;

public class LowBalanceDialog {

    public static void show(Context context, int balance, int required) {
        new AlertDialog.Builder(context)
                .setTitle("Insufficient Tokens")
                .setMessage("You need at least " + required + " tokens to book this session, "
                        + "but you only have " + balance + " tokens.\n\n"
                        + "Would you like to buy more tokens?")
                .setPositiveButton("Buy Tokens", (dialog, which) -> {
                    Intent intent = new Intent(context, BuyTokensActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}