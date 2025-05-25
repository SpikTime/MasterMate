package com.example.mastermate.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SmsRuApi {

    String BASE_URL = "https://sms.ru/";

    @GET("sms/send")
    Call<SmsRuSendResponse> sendSms(
            @Query("api_id") String apiId,
            @Query("to") String to,
            @Query("msg") String msg,
            @Query("json") int json,
            @Query("from") String from,
            @Query("test") Integer test
    );
}