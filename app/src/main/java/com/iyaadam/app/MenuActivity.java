package com.iyaadam.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuActivity extends AppCompatActivity {

    private static final String MENU_API =
            "https://iyaadam.uhd.com.ng/api/menu.php";

    private static final String WHATSAPP_NUMBER =
            "2349078917130";

    private static final String RESTAURANT_NAME =
            "DD KITCHEN";

    private static final String RESTAURANT_ADDRESS =
            "Off Olisa Road, Ijebu Ode, Ogun State, Nigeria";

    private RecyclerView menuRecycler;
    private TextView restaurantName;
    private TextView restaurantAddress;
    private TextView menuStatus;
    private TextView whatsappButton;

    private final List<Dish> allDishes = new ArrayList<>();
    private final List<Dish> displayedDishes = new ArrayList<>();

    private DishAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        initializeViews();
        setupRecyclerView();
        setupButtons();
        updateBranding();

        loadLocalMenu();
        loadMenuFromServer();
    }

    private void initializeViews() {
        menuRecycler = findViewById(R.id.menu_recycler);
        restaurantName = findViewById(R.id.restaurant_name);
        restaurantAddress = findViewById(R.id.restaurant_address);
        menuStatus = findViewById(R.id.menu_status);
        whatsappButton = findViewById(R.id.whatsapp_btn);
    }

    private void updateBranding() {
        restaurantName.setText(RESTAURANT_NAME);
        restaurantAddress.setText(RESTAURANT_ADDRESS);
        menuStatus.setText("Freshly prepared. Made with love.");
    }

    private void setupRecyclerView() {
        menuRecycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new DishAdapter(
                this,
                displayedDishes,
                new DishAdapter.OnDishClickListener() {
                    @Override
                    public void onDishClick(Dish dish) {
                        Toast.makeText(
                                MenuActivity.this,
                                dish.name,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onAddToCart(Dish dish) {
                        CartManager.getInstance().addToCart(dish);

                        Toast.makeText(
                                MenuActivity.this,
                                dish.name + " added to basket",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        menuRecycler.setAdapter(adapter);
    }

    private void setupButtons() {
        View backBtn = findViewById(R.id.back_btn);

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        setCategoryClick(R.id.category_all, "All");
        setCategoryClick(R.id.category_african, "African");
        setCategoryClick(R.id.category_continental, "Continental");
        setCategoryClick(R.id.category_grills, "Grills");
        setCategoryClick(R.id.category_rice, "Rice");
        setCategoryClick(R.id.category_soups, "Soups");

        View customOrder = findViewById(R.id.custom_order_btn);

        if (customOrder != null) {
            customOrder.setOnClickListener(v ->
                    startActivity(
                            new Intent(
                                    MenuActivity.this,
                                    CustomOrderActivity.class
                            )
                    )
            );
        }

        if (whatsappButton != null) {
            whatsappButton.setOnClickListener(v ->
                    openWhatsApp(
                            "Hello DD Kitchen! I would like to make an order."
                    )
            );
        }
    }

    private void setCategoryClick(int viewId, String category) {
        Button button = findViewById(viewId);

        if (button != null) {
            button.setOnClickListener(v -> showCategory(category));
        }
    }

    private void loadLocalMenu() {
        allDishes.clear();

        allDishes.add(new Dish(
                "fufu_ewedu",
                "Fufu & Ewedu",
                2500,
                "Traditional fufu served with delicious ewedu soup.",
                "images", 4.8, 125, 25, true
        ));

        allDishes.add(new Dish(
                "pounded_yam",
                "Pounded Yam",
                2500,
                "Smooth, freshly prepared pounded yam with Nigerian soup.",
                "images2", 4.7, 98, 25, true
        ));

        allDishes.add(new Dish(
                "jollof_chicken",
                "Jollof Rice & Chicken",
                3000,
                "Flavourful Nigerian jollof rice served with chicken.",
                "images3", 4.9, 210, 30, true
        ));

        allDishes.add(new Dish(
                "fried_rice",
                "Fried Rice & Chicken",
                3000,
                "Special fried rice paired with tasty chicken.",
                "images4", 4.7, 145, 30, true
        ));

        allDishes.add(new Dish(
                "white_rice",
                "White Rice & Stew",
                2500,
                "Steamed white rice served with rich Nigerian stew.",
                "images5", 4.6, 87, 25, true
        ));

        allDishes.add(new Dish(
                "pepper_soup",
                "Pepper Soup",
                2500,
                "A warming bowl of spicy Nigerian pepper soup.",
                "images6", 4.8, 76, 20, true
        ));

        allDishes.add(new Dish(
                "grilled_chicken",
                "Grilled Chicken",
                3500,
                "Well-seasoned chicken, grilled to delicious perfection.",
                "images7", 4.9, 112, 35, true
        ));

        allDishes.add(new Dish(
                "beef_suya",
                "Beef Suya",
                2500,
                "Spicy Nigerian-style grilled beef suya.",
                "images8", 4.8, 134, 20, true
        ));

        displayedDishes.clear();
        displayedDishes.addAll(allDishes);
        adapter.notifyDataSetChanged();
    }

    private void loadMenuFromServer() {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(MENU_API);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return;
                }

                StringBuilder response = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                )) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject root = new JSONObject(response.toString());

                if (!root.optBoolean("success", false)) {
                    return;
                }

                JSONArray items = root.optJSONArray("items");

                if (items == null || items.length() == 0) {
                    return;
                }

                List<Dish> serverDishes = new ArrayList<>();

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    String dishId = item.optString(
                            "dish_id",
                            item.optString("id", String.valueOf(i))
                    );

                    String name = item.optString("name", "Dish");
                    String description = item.optString("description", "");
                    int price = (int) item.optDouble("price", 0);
                    String imageUrl = item.optString("image_url", "");
                    double rating = item.optDouble("rating", 4.8);
                    int reviewCount = item.optInt("review_count", 0);
                    int prepTime = item.optInt("prep_time", 30);

                    boolean available = item.optInt("available", 1) == 1;

                    if (!available) {
                        continue;
                    }

                    serverDishes.add(new Dish(
                            dishId,
                            name,
                            price,
                            description,
                            imageUrl,
                            rating,
                            reviewCount,
                            prepTime,
                            true
                    ));
                }

                runOnUiThread(() -> {
                    if (!serverDishes.isEmpty()) {
                        allDishes.clear();
                        allDishes.addAll(serverDishes);

                        displayedDishes.clear();
                        displayedDishes.addAll(serverDishes);

                        adapter.notifyDataSetChanged();

                        if (menuStatus != null) {
                            menuStatus.setText(
                                    "Freshly prepared. Made with love."
                            );
                        }
                    }
                });

            } catch (Exception ignored) {
                // Keep the local menu available if the server is unreachable.
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void showCategory(String category) {
        displayedDishes.clear();

        if (category == null || category.equalsIgnoreCase("All")) {
            displayedDishes.addAll(allDishes);
        } else {
            for (Dish dish : allDishes) {
                if (matchesCategory(dish, category)) {
                    displayedDishes.add(dish);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (displayedDishes.isEmpty()) {
            Toast.makeText(
                    this,
                    "No dishes found in this category",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private boolean matchesCategory(Dish dish, String category) {
        String name = dish.name == null
                ? ""
                : dish.name.toLowerCase(Locale.ROOT);

        String search = category.toLowerCase(Locale.ROOT);

        switch (search) {
            case "african":
                return name.contains("fufu")
                        || name.contains("ewedu")
                        || name.contains("pounded")
                        || name.contains("jollof")
                        || name.contains("suya")
                        || name.contains("pepper soup");

            case "rice":
                return name.contains("rice");

            case "soups":
                return name.contains("soup")
                        || name.contains("ewedu");

            case "grills":
                return name.contains("grill")
                        || name.contains("suya")
                        || name.contains("chicken");

            case "continental":
                return !name.contains("fufu")
                        && !name.contains("ewedu")
                        && !name.contains("pounded")
                        && !name.contains("suya")
                        && !name.contains("jollof")
                        && !name.contains("pepper soup");

            default:
                return true;
        }
    }

    private void openWhatsApp(String message) {
        try {
            String url = "https://wa.me/"
                    + WHATSAPP_NUMBER
                    + "?text="
                    + Uri.encode(message);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
