package com.example.mastermate.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit smsruRetrofit = null;
    private static SmsRuApi smsRuApi = null;
    private static Retrofit uploadRetrofit = null;
    private static OkHttpClient okHttpClient = null;

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }

    private static Retrofit getSmsRuClient() {
        if (smsruRetrofit == null) {
            smsruRetrofit = new Retrofit.Builder()
                    .baseUrl(SmsRuApi.BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return smsruRetrofit;
    }
    public static SmsRuApi getSmsRuApi() {
        if (smsRuApi == null) {
            smsRuApi = getSmsRuClient().create(SmsRuApi.class);
        }
        return smsRuApi;
    }

    private static Retrofit getUploadClient() {
        if (uploadRetrofit == null) {
            uploadRetrofit = new Retrofit.Builder()
                    .baseUrl("http://localhost/")
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return uploadRetrofit;
    }
}