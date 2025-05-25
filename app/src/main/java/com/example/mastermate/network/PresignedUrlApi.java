package com.example.mastermate.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface PresignedUrlApi {
    @GET
    Call<PresignedUrlResponse> getPresignedUrl(@Url String fullUrl, @Query("object_key") String objectKey);
}