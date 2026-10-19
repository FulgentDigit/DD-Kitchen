package com.iyaadam.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutActivity extends AppCompatActivity {

private static final String API_URL =
        "https://iyaadam.uhd.com.ng/api/create_order.php";

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

private boolean submittingOrder = false;

private final ExecutorService executor =
        Executors.newSingleThreadExecutor();

private final Handler mainHandler =
        new Handler(Looper.getMainLooper());

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
            "After submitting your order, WhatsApp will open so you can " +
            "send your order details and payment confirmation. " +
            "Your transfer must still be confirmed."
    );
}

private void updateTotal() {
    int total = CartManager.getInstance().getTotalPrice();
    totalPriceTV.setText("₦" + String.format("%,d", total));
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
    String message =
            "Hello DD Kitchen, I need assistance with my food order.";

    openWhatsAppWithMessage(message);
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
    if (submittingOrder) {
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

    final String customerName = name;
    final String customerPhone = phone;
    final String deliveryAddress = address;
    final int orderTotal =
            CartManager.getInstance().getTotalPrice();

    final List<CartManager.CartItem> orderItems =
            new ArrayList<>(cartItems);

    submittingOrder = true;
    payBtn.setEnabled(false);
    payBtn.setText("SUBMITTING ORDER...");
    progressBar.setVisibility(View.VISIBLE);

    executor.execute(() -> {
        HttpURLConnection connection = null;

        try {
            JSONObject order = new JSONObject();

            order.put("customer_name", customerName);
            order.put("customer_phone", customerPhone);
            order.put("delivery_address", deliveryAddress);
            order.put(
                    "notes",
                    "Payment method: OPay bank transfer"
            );

            JSONArray items = new JSONArray();

            for (CartManager.CartItem cartItem : orderItems) {
                Dish dish = cartItem.dish;

                JSONObject item = new JSONObject();
                item.put("dish_id", dish.id);
                item.put("dish_name", dish.name);
                item.put("unit_price", dish.price);
                item.put("quantity", cartItem.quantity);

                items.put(item);
            }

            order.put("items", items);

            URL url = new URL(API_URL);

            connection = (HttpURLConnection)
                    url.openConnection();

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

            try (OutputStream output =
                         connection.getOutputStream()) {
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

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         inputStream,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject result =
                    new JSONObject(response.toString());

            boolean success =
                    result.optBoolean("success", false);

            String message = result.optString(
                    "message",
                    "Unable to submit order."
            );

            String orderNumber = result.optString(
                    "order_number",
                    ""
            );

            if (success) {
                String whatsappMessage =
                        buildOrderMessage(
                                orderNumber,
                                customerName,
                                customerPhone,
                                deliveryAddress,
                                orderItems,
                                orderTotal
                        );

                mainHandler.post(() -> {
                    submittingOrder = false;
                    progressBar.setVisibility(View.GONE);

                    payBtn.setEnabled(false);
                    payBtn.setText("ORDER SUBMITTED ✓");

                    CartManager.getInstance().clearCart();

                    Toast.makeText(
                            CheckoutActivity.this,
                            "Order submitted successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    openWhatsAppWithMessage(whatsappMessage);

                    finish();
                });

            } else {
                mainHandler.post(() -> {
                    submittingOrder = false;
                    progressBar.setVisibility(View.GONE);

                    payBtn.setEnabled(true);
                    payBtn.setText("SUBMIT ORDER");

                    Toast.makeText(
                            CheckoutActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        } catch (Exception e) {
            mainHandler.post(() -> {
                submittingOrder = false;
                progressBar.setVisibility(View.GONE);

                payBtn.setEnabled(true);
                payBtn.setText("SUBMIT ORDER");

                Toast.makeText(
                        CheckoutActivity.this,
                        "Unable to submit order. Please check your internet and try again.",
                        Toast.LENGTH_LONG
                ).show();
            });

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    });
}

private String buildOrderMessage(
        String orderNumber,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        List<CartManager.CartItem> items,
        int total
) {
    StringBuilder message = new StringBuilder();

    message.append("Hello DD Kitchen!\n");
    message.append("I have submitted a food order.\n\n");

    if (!orderNumber.isEmpty()) {
        message.append("Order Number: ")
                .append(orderNumber)
                .append("\n");
    }

    message.append("Customer: ")
            .append(customerName)
            .append("\n");

    message.append("Phone: ")
            .append(customerPhone)
            .append("\n");

    message.append("Delivery Address: ")
            .append(deliveryAddress)
            .append("\n\n");

    message.append("ORDER DETAILS\n");

    for (CartManager.CartItem cartItem : items) {
        Dish dish = cartItem.dish;

        message.append(dish.name)
                .append(" x ")
                .append(cartItem.quantity)
                .append(" = ₦")
                .append(dish.price * cartItem.quantity)
                .append("\n");
    }

    message.append("\nTOTAL: ₦")
            .append(String.format("%,d", total))
            .append("\n\n");

    message.append("BANK TRANSFER DETAILS\n");
    message.append("Bank: ")
            .append(BANK_NAME)
            .append("\n");

    message.append("Account Name: ")
            .append(ACCOUNT_NAME)
            .append("\n");

    message.append("Account Number: ")
            .append(ACCOUNT_NUMBER)
            .append("\n\n");

    message.append(
            "I will provide my transfer confirmation. " +
            "Please confirm receipt of payment and my order."
    );

    return message.toString();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
}

}
