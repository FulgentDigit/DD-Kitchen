package com.iyaadam.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private static final String BANK_NAME = "OPay";
    private static final String ACCOUNT_NAME = "Adeniyi Ismail S.";
    private static final String ACCOUNT_NUMBER = "9078917130";
    private static final String WHATSAPP_NUMBER = "2349078917130";

    private EditText nameInput;
    private EditText phoneInput;
    private EditText addressInput;

    private TextView totalPriceTV;
    private TextView bankNameTV;
    private TextView accountNameTV;
    private TextView accountNumberTV;
    private TextView paymentNoteTV;

    private Button payBtn;
    private ImageView backBtn;
    private ImageView whatsappFloat;
    private ProgressBar progressBar;

    private boolean openingWhatsApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        backBtn = findViewById(R.id.back_btn);

        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        addressInput = findViewById(R.id.address_input);

        totalPriceTV = findViewById(R.id.total_price);
        bankNameTV = findViewById(R.id.bank_name);
        accountNameTV = findViewById(R.id.account_name);
        accountNumberTV = findViewById(R.id.account_number);
        paymentNoteTV = findViewById(R.id.payment_note);

        payBtn = findViewById(R.id.pay_btn);
        whatsappFloat = findViewById(R.id.whatsapp_float);
        progressBar = findViewById(R.id.progress_bar);

        displayPaymentDetails();
        updateTotal();

        progressBar.setVisibility(View.GONE);

        backBtn.setOnClickListener(v -> finish());

        findViewById(R.id.copy_account_btn)
                .setOnClickListener(v -> copyAccountNumber());

        whatsappFloat.setOnClickListener(v -> openWhatsAppChat());

        payBtn.setOnClickListener(v -> submitOrder());
    }

    private void displayPaymentDetails() {
        bankNameTV.setText(BANK_NAME);
        accountNameTV.setText(ACCOUNT_NAME);
        accountNumberTV.setText(ACCOUNT_NUMBER);

        paymentNoteTV.setText(
                "Transfer the exact order amount to the account above. " +
                "Tap SUBMIT ORDER to open WhatsApp with your order details. " +
                "Review the message and tap Send. Payment must be confirmed."
        );
    }

    private void updateTotal() {
        int total = CartManager.getInstance().getTotalPrice();

        totalPriceTV.setText(
                "₦" + String.format(Locale.US, "%,d", total)
        );
    }

    private void copyAccountNumber() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(
                    "OPay Account Number",
                    ACCOUNT_NUMBER
            );

            clipboard.setPrimaryClip(clip);

            Toast.makeText(
                    this,
                    "Account number copied",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openWhatsAppChat() {
        openWhatsAppWithMessage(
                "Hello DD Kitchen, I need assistance with my food order."
        );
    }

    private void openWhatsAppWithMessage(String message) {
        try {
            String encodedMessage = URLEncoder.encode(
                    message,
                    StandardCharsets.UTF_8.name()
            );

            String url = "https://wa.me/"
                    + WHATSAPP_NUMBER
                    + "?text="
                    + encodedMessage;

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Unable to open WhatsApp. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void submitOrder() {
        if (openingWhatsApp) {
            return;
        }

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Enter your name");
            nameInput.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Enter your phone number");
            phoneInput.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            addressInput.setError("Enter delivery address");
            addressInput.requestFocus();
            return;
        }

        List<CartManager.CartItem> cartItems =
                CartManager.getInstance().getCartItems();

        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(
                    this,
                    "Your cart is empty",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Copy the current cart so the message uses a consistent snapshot.
        List<CartManager.CartItem> orderItems =
                new ArrayList<>(cartItems);

        int orderTotal = CartManager.getInstance().getTotalPrice();

        String message = buildOrderMessage(
                name,
                phone,
                address,
                orderItems,
                orderTotal
        );

        openingWhatsApp = true;
        payBtn.setEnabled(false);
        payBtn.setText("OPENING WHATSAPP...");
        progressBar.setVisibility(View.GONE);

        try {
            String encodedMessage = URLEncoder.encode(
                    message,
                    StandardCharsets.UTF_8.name()
            );

            Uri uri = Uri.parse(
                    "https://wa.me/" + WHATSAPP_NUMBER
                            + "?text=" + encodedMessage
            );

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);

            Toast.makeText(
                    this,
                    "Review your order in WhatsApp and tap Send.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            openingWhatsApp = false;
            payBtn.setEnabled(true);
            payBtn.setText("SUBMIT ORDER");

            Toast.makeText(
                    this,
                    "Could not open WhatsApp. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String buildOrderMessage(
            String customerName,
            String customerPhone,
            String deliveryAddress,
            List<CartManager.CartItem> items,
            int total
    ) {
        StringBuilder message = new StringBuilder();

        message.append("HELLO DD KITCHEN!\n");
        message.append("I would like to place a food order.\n\n");

        message.append("CUSTOMER DETAILS\n");
        message.append("Name: ").append(customerName).append("\n");
        message.append("Phone: ").append(customerPhone).append("\n");
        message.append("Delivery Address: ")
                .append(deliveryAddress)
                .append("\n\n");

        message.append("ORDER DETAILS\n");

        for (CartManager.CartItem cartItem : items) {
            Dish dish = cartItem.dish;

            if (dish == null) {
                continue;
            }

            int quantity = cartItem.quantity;
            int itemTotal = dish.price * quantity;

            message.append(dish.name)
                    .append(" x ")
                    .append(quantity)
                    .append(" = ₦")
                    .append(String.format(Locale.US, "%,d", itemTotal))
                    .append("\n");
        }

        message.append("\nTOTAL: ₦")
                .append(String.format(Locale.US, "%,d", total))
                .append("\n\n");

        message.append("BANK TRANSFER DETAILS\n");
        message.append("Bank: ").append(BANK_NAME).append("\n");
        message.append("Account Name: ").append(ACCOUNT_NAME).append("\n");
        message.append("Account Number: ").append(ACCOUNT_NUMBER).append("\n\n");

        message.append(
                "Please confirm my order and payment after checking the transfer."
        );

        return message.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();

        openingWhatsApp = false;

        if (payBtn != null) {
            payBtn.setEnabled(true);
            payBtn.setText("SUBMIT ORDER");
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        updateTotal();
    }
}
