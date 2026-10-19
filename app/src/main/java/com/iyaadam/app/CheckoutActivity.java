package com.iyaadam.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutActivity extends AppCompatActivity {

private static final String API_URL =
        "https://iyaadam.uhd.com.ng/api/create_order.php";

private static final String ACCOUNT_NUMBER = "9078917130";
private static final String ACCOUNT_NAME = "Adeniyi Ismail S.";
private static final String BANK_NAME = "OPay";
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
private Button copyAccountBtn;
private ImageView backBtn;
private ProgressBar progressBar;

private final ExecutorService executor =
        Executors.newSingleThreadExecutor();

private final Handler mainHandler =
        new Handler(Looper.getMainLooper());

private boolean submittingOrder = false;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_checkout);

    bindViews();
    displayPaymentDetails();
    displayOrderTotal();

    backBtn.setOnClickListener(v -> finish());

    copyAccountBtn.setOnClickListener(
            v -> copyAccountNumber()
    );

    payBtn.setOnClickListener(
            v -> submitOrder()
    );

    findViewById(R.id.whatsapp_float).setOnClickListener(
            v -> openWhatsApp(
                    "Hello DD Kitchen, I need assistance with my order."
            )
    );
}

private void bindViews() {
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
    copyAccountBtn = findViewById(R.id.copy_account_btn);
    progressBar = findViewById(R.id.progress_bar);
}

private void displayPaymentDetails() {
    bankNameTV.setText(BANK_NAME);
    accountNameTV.setText(ACCOUNT_NAME);
    accountNumberTV.setText(ACCOUNT_NUMBER);

    paymentNoteTV.setText(
            "Transfer the exact order total to this account. " +
            "After transferring, tap the confirmation button below " +
            "to submit your order and send your order details through WhatsApp."
    );
}

private void displayOrderTotal() {
    int total = CartManager.getInstance().getTotalPrice();

    totalPriceTV.setText(
            "₦" + String.format("%,d", total)
    );
}

private void copyAccountNumber() {
    ClipboardManager clipboard =
            (ClipboardManager) getSystemService(
                    Context.CLIPBOARD_SERVICE
            );

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

private void submitOrder() {

    if (submittingOrder) {
        return;
    }

    String name = nameInput.getText().toString().trim();
    String phone = phoneInput.getText().toString().trim();
    String address = addressInput.getText().toString().trim();

    if (name.isEmpty()) {
        nameInput.setError("Enter your full name");
        nameInput.requestFocus();
        return;
    }

    if (phone.isEmpty()) {
        phoneInput.setError("Enter your phone number");
        phoneInput.requestFocus();
        return;
    }

    if (address.isEmpty()) {
        addressInput.setError("Enter your delivery address");
        addressInput.requestFocus();
        return;
    }

    List<CartManager.CartItem> cartItems =
            CartManager.getInstance().getCartItems();

    if (cartItems == null || cartItems.isEmpty()) {
        Toast.makeText(
                this,
                "Your cart is empty",
                Toast.LENGTH_LONG
        ).show();
        return;
    }

    submittingOrder = true;

    payBtn.setEnabled(false);
    payBtn.setText("SUBMITTING ORDER...");
    progressBar.setVisibility(View.VISIBLE);

    final String customerName = name;
    final String customerPhone = phone;
    final String deliveryAddress = address;

    executor.execute(() -> {
        try {
            JSONObject order = new JSONObject();

            order.put("customer_name", customerName);
            order.put("customer_phone", customerPhone);
            order.put("delivery_address", deliveryAddress);
            order.put("notes", "Payment method: OPay bank transfer");

            JSONArray items = new JSONArray();

            StringBuilder itemSummary = new StringBuilder();

            for (CartManager.CartItem cartItem : cartItems) {
                Dish dish = cartItem.dish;

                JSONObject item = new JSONObject();

                item.put("dish_id", dish.id);
                item.put("dish_name", dish.name);
                item.put("unit_price", dish.price);
                item.put("quantity", cartItem.quantity);

                items.put(item);

                itemSummary.append("• ")
                        .append(dish.name)
                        .append(" x ")
                        .append(cartItem.quantity)
                        .append(" — ₦")
                        .append(String.format(
                                "%,d",
                                dish.price * cartItem.quantity
                        ))
                        .append("\n");
            }

            order.put("items", items);

            final int total =
                    CartManager.getInstance().getTotalPrice();

            String response = sendOrder(order);

            JSONObject result = new JSONObject(response);

            boolean success =
                    result.optBoolean("success", false);

            String message = result.optString(
                    "message",
                    "Unable to submit order."
            );

            if (success) {
                String orderNumber = result.optString(
                        "order_number",
                        "Not provided"
                );

                String whatsappMessage =
                        buildWhatsAppOrderMessage(
                                customerName,
                                customerPhone,
                                deliveryAddress,
                                orderNumber,
                                itemSummary.toString(),
                                total
                        );

                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);

                    payBtn.setEnabled(true);
                    payBtn.setText("ORDER SUBMITTED ✓");
                    submittingOrder = false;

                    Toast.makeText(
                            CheckoutActivity.this,
                            "Order submitted. Continue to WhatsApp to send your confirmation.",
                            Toast.LENGTH_LONG
                    ).show();

                    openWhatsApp(whatsappMessage);
                });

            } else {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    payBtn.setEnabled(true);
                    payBtn.setText("I'VE MADE THE TRANSFER");
                    submittingOrder = false;

                    Toast.makeText(
                            CheckoutActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        } catch (Exception e) {
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                payBtn.setEnabled(true);
                payBtn.setText("I'VE MADE THE TRANSFER");
                submittingOrder = false;

                Toast.makeText(
                        CheckoutActivity.this,
                        "Could not submit order. Check your internet and try again.",
                        Toast.LENGTH_LONG
                ).show();
            });
        }
    });
}

private String buildWhatsAppOrderMessage(
        String name,
        String phone,
        String address,
        String orderNumber,
        String items,
        int total
) {
    return "HELLO DD KITCHEN!\n\n"
            + "PAYMENT / ORDER CONFIRMATION\n"
            + "------------------------------\n"
            + "Order Number: " + orderNumber + "\n"
            + "Customer: " + name + "\n"
            + "Phone: " + phone + "\n"
            + "Delivery Address: " + address + "\n\n"
            + "ORDER ITEMS\n"
            + items + "\n"
            + "TOTAL: ₦" + String.format("%,d", total) + "\n\n"
            + "TRANSFER DETAILS\n"
            + "Bank: " + BANK_NAME + "\n"
            + "Account Name: " + ACCOUNT_NAME + "\n"
            + "Account Number: " + ACCOUNT_NUMBER + "\n\n"
            + "I have made the transfer. Please confirm receipt "
            + "and process my order.\n\n"
            + "I understand that payment must be verified before "
            + "the order is treated as paid.";
}

private void openWhatsApp(String message) {
    try {
        String encodedMessage = URLEncoder.encode(
                message,
                "UTF-8"
        );

        Uri uri = Uri.parse(
                "https://wa.me/" + WHATSAPP_NUMBER
                        + "?text=" + encodedMessage
        );

        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                uri
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

private String sendOrder(JSONObject order) throws Exception {
    HttpURLConnection connection = null;

    try {
        URL url = new URL(API_URL);

        connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        connection.setDoOutput(true);

        byte[] data = order.toString().getBytes(
                StandardCharsets.UTF_8
        );

        try (OutputStream output = connection.getOutputStream()) {
            output.write(data);
            output.flush();
        }

        int responseCode = connection.getResponseCode();

        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 400) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        if (inputStream == null) {
            throw new Exception("No response from server");
        }

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                ))) {

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();

    } finally {
        if (connection != null) {
            connection.disconnect();
        }
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
}

}
