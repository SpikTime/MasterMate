package com.example.mastermate.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mastermate.utils.ImageUploadCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageUploadManager {

    private static final String TAG = "ImageUploadManager";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private ImageUploadCallback callback;

    public ImageUploadManager(ImageUploadCallback callback) {
        this.callback = callback;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) return null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void uploadImage(final Drawable imageDrawable, final String uploadUrl, final String filenameOnServerParam, final String userId) {
        if (callback != null) {
            callback.onUploadStart();
        }

        final String finalFilenameOnServer = filenameOnServerParam;

        executorService.execute(() -> {
            Bitmap originalBitmap = drawableToBitmap(imageDrawable);
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to convert Drawable to Bitmap or Drawable is invalid.");
                if (callback != null) mainThreadHandler.post(() -> callback.onUploadFailure("Не удалось подготовить изображение."));
                return;
            }

            int maxSize = 1024;
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float bitmapRatio = (float) width / (float) height;
            if (width > maxSize || height > maxSize) {
                if (bitmapRatio > 1) { width = maxSize; height = (int) (width / bitmapRatio); }
                else { height = maxSize; width = (int) (height * bitmapRatio); }
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            byte[] imageBytes = stream.toByteArray();

            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing ByteArrayOutputStream", e);
            }

            if (imageBytes.length == 0) {
                Log.e(TAG, "Image byte array is empty after compression.");
                if (callback != null) mainThreadHandler.post(() -> callback.onUploadFailure("Изображение пустое."));
                return;
            }

            MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_file", finalFilenameOnServer,
                            RequestBody.create(imageBytes, MediaType.parse("image/png")));

            if (userId != null && !userId.isEmpty()) {
                multipartBodyBuilder.addFormDataPart("user_id", userId);
            }
            RequestBody requestBody = multipartBodyBuilder.build();

            Log.d(TAG, "Uploading " + imageBytes.length + " bytes to " + uploadUrl + " as " + finalFilenameOnServer + " for user: " + userId);

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .header("User-Agent", "MasterMateAppAndroid/1.0")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Upload Failed (Network): " + e.getMessage(), e);
                    if (callback != null) {
                        mainThreadHandler.post(() -> callback.onUploadFailure("Ошибка сети: " + e.getMessage()));
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    final String responseBodyStringFinal;
                    final boolean isResponseSuccessful = response.isSuccessful();
                    final int responseCode = response.code();

                    try {
                        if (response.body() != null) {
                            responseBodyStringFinal = response.body().string();
                        } else {
                            responseBodyStringFinal = "";
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                        final String errorReadingMsg = "Ошибка чтения ответа сервера.";
                        if (callback != null) {
                            mainThreadHandler.post(() -> callback.onUploadFailure(errorReadingMsg));
                        }
                        if (response.body() != null) response.body().close();
                        return;
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }

                    if (isResponseSuccessful) {
                        Log.i(TAG, "Upload Potentially Successful: " + responseCode + " - Response: " + responseBodyStringFinal);
                        if (callback != null) {
                            mainThreadHandler.post(() -> callback.onUploadSuccess(responseBodyStringFinal, finalFilenameOnServer));
                        }
                    } else {
                        Log.e(TAG, "Upload Failed (Server Error): " + responseCode + " - Response: " + responseBodyStringFinal);
                        if (callback != null) {
                            mainThreadHandler.post(() -> callback.onUploadFailure("Ошибка сервера: " + responseCode));
                        }
                    }
                }
            });
        });
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ImageUploadManager ExecutorService shutdown.");
        }
    }
}