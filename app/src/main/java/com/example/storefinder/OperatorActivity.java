package com.example.storefinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.storefinder.database.StoreDbHelper;
import com.example.storefinder.models.Product;
import com.example.storefinder.models.StoreSection;
import com.example.storefinder.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Активность для оператора магазина.
 * Позволяет загружать схему магазина, добавлять разделы и товары.
 */
public class OperatorActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_IMPORT_CSV = 2;

    private ImageView mapImageView;
    private Button loadMapButton;
    private Button addSectionButton;
    private Button importProductsButton;
    private Button addProductButton;

    private Bitmap originalMapBitmap;
    private Bitmap currentMapBitmap;
    private StoreDbHelper dbHelper;
    private long currentMapId = -1;
    private List<StoreSection> sections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator);

        dbHelper = new StoreDbHelper(this);

        mapImageView = findViewById(R.id.map_image_view);
        loadMapButton = findViewById(R.id.load_map_button);
        addSectionButton = findViewById(R.id.add_section_button);
        importProductsButton = findViewById(R.id.import_products_button);
        addProductButton = findViewById(R.id.add_product_button);

        // Настройка слушателей для кнопок
        loadMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            }
        });

        addSectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (originalMapBitmap == null) {
                    Toast.makeText(OperatorActivity.this,
                            "Сначала загрузите схему магазина", Toast.LENGTH_SHORT).show();
                    return;
                }

                showAddSectionDialog();
            }
        });

        importProductsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMapId == -1) {
                    Toast.makeText(OperatorActivity.this,
                            "Сначала загрузите схему магазина", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                startActivityForResult(intent, REQUEST_IMPORT_CSV);
            }
        });

        addProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sections.isEmpty()) {
                    Toast.makeText(OperatorActivity.this,
                            "Сначала добавьте разделы магазина", Toast.LENGTH_SHORT).show();
                    return;
                }

                showAddProductDialog();
            }
        });

        // Настройка обработчика касаний для ImageView
        mapImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && originalMapBitmap != null) {
                    float touchX = event.getX();
                    float touchY = event.getY();

                    // Пересчет координат касания с учетом размера изображения
                    float scaleX = (float) originalMapBitmap.getWidth() / mapImageView.getWidth();
                    float scaleY = (float) originalMapBitmap.getHeight() / mapImageView.getHeight();

                    float actualX = touchX * scaleX;
                    float actualY = touchY * scaleY;

                    showAddSectionDialog(actualX, actualY);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Uri imageUri = data.getData();
                try {
                    originalMapBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    String imagePath = FileUtils.saveImageToInternalStorage(this, originalMapBitmap);

                    // Запрос имени схемы магазина
                    showSaveMapDialog(imagePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this,
                            "Не удалось загрузить изображение: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMPORT_CSV && data != null) {
                Uri csvUri = data.getData();
                try {
                    String csvContent = FileUtils.readTextFromUri(this, csvUri);
                    int importedCount = dbHelper.importProductsFromCSV(csvContent, currentMapId);

                    Toast.makeText(this,
                            "Успешно импортировано товаров: " + importedCount,
                            Toast.LENGTH_SHORT).show();

                    // Обновить список разделов с новыми товарами
                    loadSections();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this,
                            "Ошибка чтения файла: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Показывает диалог для сохранения схемы магазина.
     * @param imagePath Путь к сохраненному изображению
     */
    private void showSaveMapDialog(final String imagePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_map, null);
        builder.setView(view);

        final EditText mapNameEditText = view.findViewById(R.id.map_name_edit_text);
        Button saveButton = view.findViewById(R.id.save_map_button);

        final AlertDialog dialog = builder.create();
        dialog.show();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mapName = mapNameEditText.getText().toString().trim();
                if (mapName.isEmpty()) {
                    mapNameEditText.setError("Введите название схемы");
                    return;
                }

                currentMapId = dbHelper.saveStoreMap(mapName, imagePath);
                if (currentMapId != -1) {
                    Toast.makeText(OperatorActivity.this,
                            "Схема успешно сохранена", Toast.LENGTH_SHORT).show();

                    // Отображение загруженной схемы
                    currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);
                    mapImageView.setImageBitmap(currentMapBitmap);

                    // Активация кнопок для работы со схемой
                    addSectionButton.setEnabled(true);
                    importProductsButton.setEnabled(true);
                } else {
                    Toast.makeText(OperatorActivity.this,
                            "Не удалось сохранить схему", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            }
        });
    }

    /**
     * Показывает диалог для добавления раздела магазина.
     */
    private void showAddSectionDialog() {
        showAddSectionDialog(-1, -1);
    }

    /**
     * Показывает диалог для добавления раздела магазина с заданными координатами.
     * @param x Координата X точки на схеме
     * @param y Координата Y точки на схеме
     */
    private void showAddSectionDialog(final float x, final float y) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_section, null);
        builder.setView(view);

        final EditText sectionNameEditText = view.findViewById(R.id.section_name_edit_text);
        final EditText sectionXEditText = view.findViewById(R.id.section_x_edit_text);
        final EditText sectionYEditText = view.findViewById(R.id.section_y_edit_text);
        Button addButton = view.findViewById(R.id.add_section_button);

        // Если координаты были переданы, устанавливаем их в поля ввода
        if (x >= 0 && y >= 0) {
            sectionXEditText.setText(String.valueOf(x));
            sectionYEditText.setText(String.valueOf(y));
        }

        final AlertDialog dialog = builder.create();
        dialog.show();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sectionName = sectionNameEditText.getText().toString().trim();
                if (sectionName.isEmpty()) {
                    sectionNameEditText.setError("Введите название раздела");
                    return;
                }

                float sectionX, sectionY;
                try {
                    sectionX = Float.parseFloat(sectionXEditText.getText().toString());
                    sectionY = Float.parseFloat(sectionYEditText.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(OperatorActivity.this,
                            "Введите корректные координаты", Toast.LENGTH_SHORT).show();
                    return;
                }

                StoreSection section = new StoreSection();
                section.setName(sectionName);
                section.setX(sectionX);
                section.setY(sectionY);

                long sectionId = dbHelper.addSection(section, currentMapId);
                if (sectionId != -1) {
                    section.setId(sectionId);
                    sections.add(section);

                    Toast.makeText(OperatorActivity.this,
                            "Раздел успешно добавлен", Toast.LENGTH_SHORT).show();

                    // Отрисовка метки раздела на схеме
                    drawSectionMarker(section);
                } else {
                    Toast.makeText(OperatorActivity.this,
                            "Не удалось добавить раздел", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            }
        });
    }

    /**
     * Показывает диалог для добавления товара.
     */
    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(view);

        final EditText productNameEditText = view.findViewById(R.id.product_name_edit_text);
        final Spinner sectionSpinner = view.findViewById(R.id.section_spinner);
        Button addButton = view.findViewById(R.id.add_product_button);

        // Заполнение спиннера разделами
        ArrayAdapter<StoreSection> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(adapter);

        final AlertDialog dialog = builder.create();
        dialog.show();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productName = productNameEditText.getText().toString().trim();
                if (productName.isEmpty()) {
                    productNameEditText.setError("Введите название товара");
                    return;
                }

                StoreSection selectedSection = (StoreSection) sectionSpinner.getSelectedItem();
                if (selectedSection == null) {
                    Toast.makeText(OperatorActivity.this,
                            "Выберите раздел магазина", Toast.LENGTH_SHORT).show();
                    return;
                }

                Product product = new Product();
                product.setName(productName);
                product.setSectionId(selectedSection.getId());

                long productId = dbHelper.addProduct(product);
                if (productId != -1) {
                    Toast.makeText(OperatorActivity.this,
                            "Товар успешно добавлен", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OperatorActivity.this,
                            "Не удалось добавить товар", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            }
        });
    }

    /**
     * Отрисовывает метку раздела на схеме магазина.
     * @param section Раздел магазина
     */
    private void drawSectionMarker(StoreSection section) {
        if (currentMapBitmap == null) return;

        // Создание копии текущего bitmap для рисования
        Bitmap newBitmap = currentMapBitmap.copy(currentMapBitmap.getConfig(), true);
        Canvas canvas = new Canvas(newBitmap);

        // Настройка кисти для рисования
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Рисование круга
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(section.getX(), section.getY(), 15, paint);

        // Рисование рамки круга
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawCircle(section.getX(), section.getY(), 15, paint);

        // Отображение текста с названием раздела
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        canvas.drawText(section.getName(), section.getX() + 20, section.getY(), paint);

        // Обновление изображения
        currentMapBitmap = newBitmap;
        mapImageView.setImageBitmap(currentMapBitmap);
    }

    /**
     * Загружает список разделов магазина из базы данных.
     */
    private void loadSections() {
        sections = dbHelper.getAllSections(currentMapId);

        if (originalMapBitmap != null) {
            // Перерисовываем схему с метками всех разделов
            currentMapBitmap = originalMapBitmap.copy(originalMapBitmap.getConfig(), true);

            for (StoreSection section : sections) {
                drawSectionMarker(section);
            }

            mapImageView.setImageBitmap(currentMapBitmap);
        }
    }
}