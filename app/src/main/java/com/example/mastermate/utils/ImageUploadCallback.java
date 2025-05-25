package com.example.mastermate.utils;

public interface ImageUploadCallback {
    void onUploadStart();
    void onUploadSuccess(String serverResponseJson, String uploadedFileName); // Передаем JSON-ответ и имя файла
    void onUploadFailure(String errorMessage);
}