package com.example.storefinder;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.storefinder.database.StoreDbHelper;
import com.example.storefinder.models.Product;
import com.example.storefinder.models.StoreSection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Активность покупателя.
 * Позволяет искать товары и видеть их расположение на схеме магазина.
 */
public class CustomerActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView productsListView;
    private ImageView mapImageView;
    private Button pasteButton;

    private StoreDbHelper dbHelper;
    private ArrayAdapter<Product> productAdapter;
    private List<Product> productsList = new ArrayList<>();
    private Bitmap originalMapBitmap;
    private Bitmap currentMapBitmap;
    private long currentMapId = 1; // По умолчанию первая схема магазина

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        dbHelper = new StoreDbHelper(this);

        searchEditText = findViewById(R.id.search_edit_text);
        productsListView = findViewById(R.id.products_list_view);
        mapImageView = findViewById(R.id.customer_map_image_view);
        pasteButton = findViewById(R.id.paste_button);

        // Настройка адаптера для списка товаров
        productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, productsList);
        productsListView.setAdapter(productAdapter);

        // Загрузка карты магазина
        String imagePath = dbHelper.getStoreMapPath(currentMapId);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                originalMapBitmap = BitmapFactory.decodeFile(imagePath);
                currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);
                mapImageView.setImageBitmap(currentMapBitmap);
            }
        }

        // Настройка поиска по мере ввода текста
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchProducts(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Настройка обработчика выбора товара из списка
        productsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product selectedProduct = productsList.get(position);
                highlightProductSection(selectedProduct);
            }
        });

        // Настройка кнопки для вставки из буфера обмена
        pasteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteFromClipboard();
            }
        });
    }

    /**
     * Выполняет поиск товаров по заданному запросу.
     * @param query Поисковый запрос
     */
    private void searchProducts(String query) {
        productsList.clear();
        productsList.addAll(dbHelper.searchProducts(query));
        productAdapter.notifyDataSetChanged();
    }

    /**
     * Выделяет раздел магазина с указанным товаром на схеме.
     * @param product Выбранный товар
     */
    private void highlightProductSection(Product product) {
        if (originalMapBitmap == null) return;

        // Получаем информацию о разделе магазина
        StoreSection section = dbHelper.getSectionById(product.getSectionId());
        if (section == null) {
            Toast.makeText(this,
                    "Не удалось найти раздел для товара", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сбрасываем текущую схему к оригиналу
        currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);
        Canvas canvas = new Canvas(currentMapBitmap);

        // Настройка кисти для рисования
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Рисование увеличенного круга для выделения раздела
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(128); // Полупрозрачный
        canvas.drawCircle(section.getX(), section.getY(), 30, paint);

        // Рисование рамки круга
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setAlpha(255); // Полностью непрозрачный
        canvas.drawCircle(section.getX(), section.getY(), 30, paint);

        // Отображение текста с названием раздела
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(40);
        paint.setAlpha(255);
        canvas.drawText(section.getName() + " - " + product.getName(),
                section.getX() + 40, section.getY(), paint);

        // Обновление изображения
        mapImageView.setImageBitmap(currentMapBitmap);

        Toast.makeText(this,
                "Товар " + product.getName() + " находится в разделе: " + section.getName(),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Вставляет список товаров из буфера обмена и анализирует его.
     */
    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) {
                processProductList(text.toString());
            } else {
                Toast.makeText(this,
                        "Буфер обмена не содержит текст", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,
                    "Буфер обмена пуст", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обрабатывает список товаров из буфера обмена.
     * @param text Текст из буфера обмена
     */
    private void processProductList(String text) {
        String[] lines = text.split("\n");
        List<Product> foundProducts = new ArrayList<>();
        Map<Long, List<Product>> sectionProducts = new HashMap<>();

        // Очистка текущей схемы
        if (originalMapBitmap != null) {
            currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);
        }

        for (String line : lines) {
            String productName = line.trim();
            if (!productName.isEmpty()) {
                List<Product> matches = dbHelper.searchProducts(productName);
                if (!matches.isEmpty()) {
                    Product product = matches.get(0); // Берем первое совпадение
                    foundProducts.add(product);

                    // Группировка товаров по разделам
                    long sectionId = product.getSectionId();
                    if (!sectionProducts.containsKey(sectionId)) {
                        sectionProducts.put(sectionId, new ArrayList<Product>());
                    }
                    sectionProducts.get(sectionId).add(product);
                }
            }
        }

        if (foundProducts.isEmpty()) {
            Toast.makeText(this,
                    "Не найдено товаров из вашего списка", Toast.LENGTH_SHORT).show();
            return;
        }

        // Обновляем список найденных товаров
        productsList.clear();
        productsList.addAll(foundProducts);
        productAdapter.notifyDataSetChanged();

        // Выделяем все разделы магазина с найденными товарами
        highlightMultipleSections(sectionProducts);

        Toast.makeText(this,
                "Найдено " + foundProducts.size() + " товаров", Toast.LENGTH_SHORT).show();
    }

    /**
     * Выделяет несколько разделов магазина на карте.
     * @param sectionProducts Карта соответствия разделов и товаров
     */
    private void highlightMultipleSections(Map<Long, List<Product>> sectionProducts) {
        if (originalMapBitmap == null) return;

        // Сбрасываем текущую схему к оригиналу
        currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);
        Canvas canvas = new Canvas(currentMapBitmap);

        // Настройка кисти для рисования
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Цвета для разных разделов
        int[] colors = new int[] {
                Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };
        int colorIndex = 0;

        // Отображаем каждый раздел на карте
        for (Map.Entry<Long, List<Product>> entry : sectionProducts.entrySet()) {
            long sectionId = entry.getKey();
            List<Product> products = entry.getValue();

            StoreSection section = dbHelper.getSectionById(sectionId);
            if (section != null) {
                // Выбор цвета
                int color = colors[colorIndex % colors.length];
                colorIndex++;

                // Рисование круга для выделения раздела
                paint.setColor(color);
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha(128); // Полупрозрачный
                canvas.drawCircle(section.getX(), section.getY(), 30, paint);

                // Рисование рамки круга
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                paint.setAlpha(255);
                canvas.drawCircle(section.getX(), section.getY(), 30, paint);

                // Отображение текста с названием раздела
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(30);
                paint.setAlpha(255);

                StringBuilder productList = new StringBuilder();
                for (Product product : products) {
                    if (productList.length() > 0) productList.append(", ");
                    productList.append(product.getName());
                }

                String text = section.getName() + ": " + productList.toString();
                // Ограничиваем длину текста, чтобы он не выходил за пределы экрана
                if (text.length() > 25) {
                    text = text.substring(0, 22) + "...";
                }

                canvas.drawText(text, section.getX() + 40, section.getY(), paint);
            }
        }

        // Обновление изображения
        mapImageView.setImageBitmap(currentMapBitmap);
    }
}