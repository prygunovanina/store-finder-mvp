package com.example.storefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Главная активность приложения.
 * Позволяет выбрать роль: оператор магазина или покупатель.
 */
public class MainActivity extends AppCompatActivity {

    private Button operatorButton;
    private Button customerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация кнопок
        operatorButton = findViewById(R.id.operator_button);
        customerButton = findViewById(R.id.customer_button);

        // Настройка слушателей для кнопок
        operatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход на экран оператора магазина
                Intent intent = new Intent(MainActivity.this, OperatorActivity.class);
                startActivity(intent);
            }
        });

        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход на экран покупателя
                Intent intent = new Intent(MainActivity.this, CustomerActivity.class);
                startActivity(intent);
            }
        });
    }
}