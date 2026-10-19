package com.iyaadam.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.Locale;

public class CartActivity extends AppCompatActivity
implements CartAdapter.OnCartChangeListener {

private RecyclerView cartRecycler;
private CartAdapter adapter;
private TextView totalPriceTV;
private TextView emptyMsg;
private Button checkoutBtn;
private ImageView backBtn;

private final NumberFormat nairaFormat =
        NumberFormat.getNumberInstance(Locale.US);

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cart);

    backBtn = findViewById(R.id.back_btn);
    cartRecycler = findViewById(R.id.cart_recycler);
    totalPriceTV = findViewById(R.id.total_price);
    emptyMsg = findViewById(R.id.empty_msg);
    checkoutBtn = findViewById(R.id.checkout_btn);

    backBtn.setOnClickListener(v -> finish());

    cartRecycler.setLayoutManager(
            new LinearLayoutManager(this)
    );

    adapter = new CartAdapter(
            this,
            CartManager.getInstance().getCartItems(),
            this
    );

    cartRecycler.setAdapter(adapter);

    checkoutBtn.setOnClickListener(v -> {
        if (CartManager.getInstance().getItemCount() <= 0) {
            updateUI();
            return;
        }

        Intent intent = new Intent(
                CartActivity.this,
                CheckoutActivity.class
        );

        startActivity(intent);
    });

    updateUI();
}

@Override
protected void onResume() {
    super.onResume();

    if (adapter != null) {
        adapter.notifyDataSetChanged();
        updateUI();
    }
}

@Override
public void onCartChanged() {
    runOnUiThread(() -> {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUI();
    });
}

private void updateUI() {
    int itemCount =
            CartManager.getInstance().getItemCount();

    int totalPrice =
            CartManager.getInstance().getTotalPrice();

    totalPriceTV.setText(
            "₦" + nairaFormat.format(totalPrice)
    );

    boolean isEmpty = itemCount <= 0;

    emptyMsg.setVisibility(
            isEmpty ? View.VISIBLE : View.GONE
    );

    cartRecycler.setVisibility(
            isEmpty ? View.GONE : View.VISIBLE
    );

    checkoutBtn.setEnabled(!isEmpty);
    checkoutBtn.setAlpha(isEmpty ? 0.5f : 1.0f);
}

}
