package com.example.storefinder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Утилитарный класс для работы с файлами.
 */
public class FileUtils {

    /**
     * Сохраняет изображение во внутреннюю память приложения.
     * @param context Контекст приложения
     * @param bitmap Изображение для сохранения
     * @return Путь к сохраненному файлу
     * @throws IOException Если возникла ошибка при записи файла
     */
    public static String saveImageToInternalStorage(Context context, Bitmap bitmap) throws IOException {
        String filename = "store_map_" + UUID.randomUUID().toString() + ".png";
        File file = new File(context.getFilesDir(), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }

        return file.getAbsolutePath();
    }

    /**
     * Читает текст из URI файла.
     * @param context Контекст приложения
     * @param uri URI файла для чтения
     * @return Содержимое файла в виде строки
     * @throws IOException Если возникла ошибка при чтении файла
     */
    public static String readTextFromUri(Context context, Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Копирует файл из одного места в другое.
     * @param source Исходный файл
     * @param destination Файл назначения
     * @throws IOException Если возникла ошибка при копировании
     */
    public static void copyFile(File source, File destination) throws IOException {
        try (InputStream in = new java.io.FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}