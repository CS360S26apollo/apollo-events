package com.example.peertutoring.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.example.peertutoring.ui.BuyTokensActivity;

/**
 * Shows a dialog when a student doesn't have enough tokens to book a session.
 * Offers to navigate to the BuyTokensActivity to top up.
 */
public class LowBalanceDialog {

    /**
     * Shows the low balance dialog.
     *
     * @param context    The calling Activity context.
     * @param balance    Student's current token balance.
     * @param required   Minimum tokens needed for the session.
     */
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