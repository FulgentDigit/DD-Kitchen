package com.iyaadam.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
implements DishAdapter.OnDishClickListener {

private RecyclerView dishesRecycler;
private DishAdapter adapter;
private List<Dish> dishes;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initializeDishes();
    setupNavigation();
    setupActionButtons();
}

private void initializeDishes() {
    dishesRecycler = findViewById(R.id.dishes_recycler);

    if (dishesRecycler == null) {
        Toast.makeText(
                this,
                "The menu could not be loaded. Please try again.",
                Toast.LENGTH_LONG
        ).show();
        return;
    }

    dishesRecycler.setLayoutManager(
            new GridLayoutManager(this, 2)
    );

    dishes = createSampleDishes();

    adapter = new DishAdapter(this, dishes, this);
    dishesRecycler.setAdapter(adapter);
}

private void setupNavigation() {
    setClickListener(R.id.ic_home, v ->
            Toast.makeText(
                    MainActivity.this,
                    "Welcome to Chicken and Co.!",
                    Toast.LENGTH_SHORT
            ).show()
    );

    setClickListener(R.id.ic_search, v -> openMenu());

    setClickListener(R.id.ic_cart_nav, v -> openCart());

    setClickListener(R.id.ic_orders, v ->
            startActivity(
                    new Intent(
                            MainActivity.this,
                            MusicPlayerActivity.class
                    )
            )
    );

    setClickListener(R.id.ic_person, v ->
            startActivity(
                    new Intent(
                            MainActivity.this,
                            ProfileActivity.class
                    )
            )
    );
}

private void setupActionButtons() {
    setClickListener(R.id.btn_order_now, v -> openMenu());

    setClickListener(R.id.btn_view_all, v -> openMenu());

    setClickListener(R.id.ic_cart_header, v -> openCart());
}

private void setClickListener(
        int viewId,
        View.OnClickListener listener
) {
    View view = findViewById(viewId);

    if (view != null) {
        view.setOnClickListener(listener);
    }
}

private void openMenu() {
    startActivity(
            new Intent(
                    MainActivity.this,
                    MenuActivity.class
            )
    );
}

private void openCart() {
    startActivity(
            new Intent(
                    MainActivity.this,
                    CartActivity.class
            )
    );
}

private List<Dish> createSampleDishes() {
    List<Dish> list = new ArrayList<>();

    list.add(new Dish(
            "1", "Fufu & Ewedu", 2500,
            "Traditional African delicacy",
            "images", 4.7, 152, 20, true
    ));

    list.add(new Dish(
            "2", "Jollof Rice & Chicken", 3000,
            "Classic Nigerian taste",
            "images8", 4.8, 198, 25, true
    ));

    list.add(new Dish(
            "3", "Goat Meat Pepper Sauce", 3500,
            "Tender goat in spicy sauce",
            "images13", 4.6, 87, 30, true
    ));

    list.add(new Dish(
            "4", "Creamy Pasta & Chicken", 3500,
            "Rich and satisfying",
            "images234", 4.5, 124, 15, true
    ));

    list.add(new Dish(
            "5", "Moi Moi", 1500,
            "Steamed bean pudding",
            "images10", 4.4, 76, 20, true
    ));

    list.add(new Dish(
            "6", "Pepper Soup", 2000,
            "Hot, flavourful broth",
            "images11", 4.3, 95, 15, true
    ));

    list.add(new Dish(
            "7", "Pounded Yam & Egusi", 4000,
            "Smooth yam with rich egusi soup",
            "images13", 4.9, 213, 25, true
    ));

    list.add(new Dish(
            "8", "Rice & Vegetables", 2200,
            "Rice tossed with mixed vegetables",
            "images15", 4.2, 64, 18, true
    ));

    return list;
}

@Override
public void onDishClick(Dish dish) {
    addDishToCart(dish);
}

@Override
public void onAddToCart(Dish dish) {
    addDishToCart(dish);
}

private void addDishToCart(Dish dish) {
    CartManager.getInstance().addToCart(dish);

    Toast.makeText(
            this,
            dish.name + " added to your basket",
            Toast.LENGTH_SHORT
    ).show();
}

}
