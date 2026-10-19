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
                    "Dish list could not be loaded.",
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

        // Home tab
        setClickListener(R.id.ic_home, v ->
                Toast.makeText(
                        MainActivity.this,
                        "Welcome to DD Kitchen!",
                        Toast.LENGTH_SHORT
                ).show()
        );

        // Search tab — use the ID in the current XML.
        setClickListener(R.id.ic_search, v -> openMenu());

        // Cart tab
        setClickListener(R.id.ic_cart_nav, v -> openCart());

        // Orders/music tab — preserve the existing music feature.
        setClickListener(R.id.ic_orders, v ->
                startActivity(
                        new Intent(
                                MainActivity.this,
                                MusicPlayerActivity.class
                        )
                )
        );

        // Customer profile
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

        // Featured food banner
        setClickListener(R.id.btn_order_now, v -> openMenu());

        // View all dishes
        setClickListener(R.id.btn_view_all, v -> openMenu());

        // Cart button in the header
        setClickListener(R.id.ic_cart_header, v -> openCart());

        /*
         * The latest activity_main.xml does not contain
         * ic_notification or ic_search_nav.
         * Their listeners have therefore been removed to
         * prevent the previous compilation errors.
         *
         * The search action is handled by ic_search.
         * The cart action is handled by ic_cart_nav and
         * ic_cart_header.
         */
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
                "1",
                "Fufu & Ewedu",
                2500,
                "Traditional African delicacy",
                "images",
                4.7,
                152,
                20,
                true
        ));

        list.add(new Dish(
                "2",
                "Jollof Rice & Chicken",
                3000,
                "Classic Nigerian taste",
                "images8",
                4.8,
                198,
                25,
                true
        ));

        list.add(new Dish(
                "3",
                "Goat Meat Pepper Sauce",
                3500,
                "Tender goat in spicy sauce",
                "images13",
                4.6,
                87,
                30,
                true
        ));

        list.add(new Dish(
                "4",
                "Creamy Pasta & Chicken",
                3500,
                "Rich and satisfying",
                "images234",
                4.5,
                124,
                15,
                true
        ));

        list.add(new Dish(
                "5",
                "Moi Moi",
                1500,
                "Steamed bean pudding",
                "images10",
                4.4,
                76,
                20,
                true
        ));

        list.add(new Dish(
                "6",
                "Pepper Soup",
                2000,
                "Hot flavorful broth",
                "images11",
                4.3,
                95,
                15,
                true
        ));

        list.add(new Dish(
                "7",
                "Pounded Yam & Egusi",
                4000,
                "Smooth yam with gravy",
                "images13",
                4.9,
                213,
                25,
                true
        ));

        list.add(new Dish(
                "8",
                "Rice & Vegetable",
                2200,
                "Mixed vegetable rice",
                "images15",
                4.2,
                64,
                18,
                true
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
                dish.name + " added to cart",
                Toast.LENGTH_SHORT
        ).show();
    }
}
