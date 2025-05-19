package com.example.storefinder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.storefinder.models.Product;
import com.example.storefinder.models.StoreSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Вспомогательный класс для работы с базой данных приложения.
 * Отвечает за создание таблиц и выполнение CRUD операций.
 */
public class StoreDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "store.db";
    private static final int DATABASE_VERSION = 1;

    // Таблица схем магазинов
    private static final String TABLE_STORE_MAPS = "store_maps";
    private static final String COLUMN_MAP_ID = "id";
    private static final String COLUMN_MAP_NAME = "name";
    private static final String COLUMN_MAP_IMAGE_PATH = "image_path";

    // Таблица разделов магазина
    private static final String TABLE_SECTIONS = "sections";
    private static final String COLUMN_SECTION_ID = "id";
    private static final String COLUMN_SECTION_NAME = "name";
    private static final String COLUMN_SECTION_X = "x";
    private static final String COLUMN_SECTION_Y = "y";
    private static final String COLUMN_SECTION_MAP_ID = "map_id";

    // Таблица товаров
    private static final String TABLE_PRODUCTS = "products";
    private static final String COLUMN_PRODUCT_ID = "id";
    private static final String COLUMN_PRODUCT_NAME = "name";
    private static final String COLUMN_PRODUCT_SECTION_ID = "section_id";

    public StoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы схем магазинов
        String createMapTable = "CREATE TABLE " + TABLE_STORE_MAPS + " (" +
                COLUMN_MAP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MAP_NAME + " TEXT, " +
                COLUMN_MAP_IMAGE_PATH + " TEXT)";
        db.execSQL(createMapTable);

        // Создание таблицы разделов магазина
        String createSectionTable = "CREATE TABLE " + TABLE_SECTIONS + " (" +
                COLUMN_SECTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SECTION_NAME + " TEXT, " +
                COLUMN_SECTION_X + " REAL, " +
                COLUMN_SECTION_Y + " REAL, " +
                COLUMN_SECTION_MAP_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_SECTION_MAP_ID + ") REFERENCES " +
                TABLE_STORE_MAPS + "(" + COLUMN_MAP_ID + "))";
        db.execSQL(createSectionTable);

        // Создание таблицы товаров
        String createProductTable = "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                COLUMN_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRODUCT_NAME + " TEXT, " +
                COLUMN_PRODUCT_SECTION_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_PRODUCT_SECTION_ID + ") REFERENCES " +
                TABLE_SECTIONS + "(" + COLUMN_SECTION_ID + "))";
        db.execSQL(createProductTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении схемы базы данных удаляем старые таблицы и создаем новые
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STORE_MAPS);
        onCreate(db);
    }

    /**
     * Сохраняет путь к изображению схемы магазина в базу данных.
     * @param name Название схемы
     * @param imagePath Путь к файлу изображения
     * @return Идентификатор добавленной схемы
     */
    public long saveStoreMap(String name, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAP_NAME, name);
        values.put(COLUMN_MAP_IMAGE_PATH, imagePath);

        long id = db.insert(TABLE_STORE_MAPS, null, values);
        db.close();
        return id;
    }

    /**
     * Получает путь к изображению схемы магазина.
     * @param mapId Идентификатор схемы
     * @return Путь к файлу изображения
     */
    public String getStoreMapPath(long mapId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String imagePath = null;

        Cursor cursor = db.query(TABLE_STORE_MAPS,
                new String[]{COLUMN_MAP_IMAGE_PATH},
                COLUMN_MAP_ID + "=?",
                new String[]{String.valueOf(mapId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            imagePath = cursor.getString(cursor.getColumnIndex(COLUMN_MAP_IMAGE_PATH));
            cursor.close();
        }

        db.close();
        return imagePath;
    }

    /**
     * Добавляет новый раздел магазина.
     * @param section Объект раздела магазина
     * @param mapId Идентификатор схемы магазина
     * @return Идентификатор добавленного раздела
     */
    public long addSection(StoreSection section, long mapId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SECTION_NAME, section.getName());
        values.put(COLUMN_SECTION_X, section.getX());
        values.put(COLUMN_SECTION_Y, section.getY());
        values.put(COLUMN_SECTION_MAP_ID, mapId);

        long id = db.insert(TABLE_SECTIONS, null, values);
        db.close();
        return id;
    }

    /**
     * Получает список всех разделов магазина для конкретной схемы.
     * @param mapId Идентификатор схемы магазина
     * @return Список разделов
     */
    public List<StoreSection> getAllSections(long mapId) {
        List<StoreSection> sectionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SECTIONS +
                " WHERE " + COLUMN_SECTION_MAP_ID + " = " + mapId;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                StoreSection section = new StoreSection();
                section.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_SECTION_ID)));
                section.setName(cursor.getString(cursor.getColumnIndex(COLUMN_SECTION_NAME)));
                section.setX(cursor.getFloat(cursor.getColumnIndex(COLUMN_SECTION_X)));
                section.setY(cursor.getFloat(cursor.getColumnIndex(COLUMN_SECTION_Y)));

                sectionList.add(section);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return sectionList;
    }

    /**
     * Добавляет новый товар в базу данных.
     * @param product Объект товара
     * @return Идентификатор добавленного товара
     */
    public long addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_NAME, product.getName());
        values.put(COLUMN_PRODUCT_SECTION_ID, product.getSectionId());

        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return id;
    }

    /**
     * Получает список всех товаров в указанном разделе магазина.
     * @param sectionId Идентификатор раздела
     * @return Список товаров
     */
    public List<Product> getProductsBySection(long sectionId) {
        List<Product> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE " + COLUMN_PRODUCT_SECTION_ID + " = " + sectionId;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Product product = new Product();
                product.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_PRODUCT_ID)));
                product.setName(cursor.getString(cursor.getColumnIndex(COLUMN_PRODUCT_NAME)));
                product.setSectionId(cursor.getLong(cursor.getColumnIndex(COLUMN_PRODUCT_SECTION_ID)));

                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return productList;
    }

    /**
     * Ищет товары по названию (частичное совпадение).
     * @param query Поисковый запрос
     * @return Список найденных товаров
     */
    public List<Product> searchProducts(String query) {
        List<Product> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE " + COLUMN_PRODUCT_NAME + " LIKE '%" + query + "%'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Product product = new Product();
                product.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_PRODUCT_ID)));
                product.setName(cursor.getString(cursor.getColumnIndex(COLUMN_PRODUCT_NAME)));
                product.setSectionId(cursor.getLong(cursor.getColumnIndex(COLUMN_PRODUCT_SECTION_ID)));

                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return productList;
    }

    /**
     * Получает информацию о разделе магазина по идентификатору.
     * @param sectionId Идентификатор раздела
     * @return Объект раздела магазина
     */
    public StoreSection getSectionById(long sectionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        StoreSection section = null;

        Cursor cursor = db.query(TABLE_SECTIONS,
                new String[]{COLUMN_SECTION_ID, COLUMN_SECTION_NAME, COLUMN_SECTION_X, COLUMN_SECTION_Y},
                COLUMN_SECTION_ID + "=?",
                new String[]{String.valueOf(sectionId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            section = new StoreSection();
            section.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_SECTION_ID)));
            section.setName(cursor.getString(cursor.getColumnIndex(COLUMN_SECTION_NAME)));
            section.setX(cursor.getFloat(cursor.getColumnIndex(COLUMN_SECTION_X)));
            section.setY(cursor.getFloat(cursor.getColumnIndex(COLUMN_SECTION_Y)));
            cursor.close();
        }

        db.close();
        return section;
    }

    /**
     * Импортирует список товаров из строки (CSV формат).
     * @param csvData Строка с данными в формате "название товара,раздел магазина"
     * @param mapId Идентификатор схемы магазина
     * @return Количество успешно импортированных товаров
     */
    public int importProductsFromCSV(String csvData, long mapId) {
        int importedCount = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            String[] lines = csvData.split("\n");
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String productName = parts[0].trim();
                    String sectionName = parts[1].trim();

                    // Найти или создать раздел
                    long sectionId = findSectionByName(sectionName, mapId);
                    if (sectionId == -1) {
                        // Если раздел не существует, пропускаем этот товар
                        continue;
                    }

                    // Добавить новый товар
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_PRODUCT_NAME, productName);
                    values.put(COLUMN_PRODUCT_SECTION_ID, sectionId);

                    db.insert(TABLE_PRODUCTS, null, values);
                    importedCount++;
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }

        return importedCount;
    }

    /**
     * Находит раздел по имени для указанной схемы магазина.
     * @param sectionName Название раздела
     * @param mapId Идентификатор схемы магазина
     * @return Идентификатор раздела или -1, если раздел не найден
     */
    private long findSectionByName(String sectionName, long mapId) {
        SQLiteDatabase db = this.getReadableDatabase();
        long sectionId = -1;

        Cursor cursor = db.query(TABLE_SECTIONS,
                new String[]{COLUMN_SECTION_ID},
                COLUMN_SECTION_NAME + "=? AND " + COLUMN_SECTION_MAP_ID + "=?",
                new String[]{sectionName, String.valueOf(mapId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            sectionId = cursor.getLong(cursor.getColumnIndex(COLUMN_SECTION_ID));
            cursor.close();
        }

        return sectionId;
    }
}