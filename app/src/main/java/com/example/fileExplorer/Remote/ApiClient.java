package com.example.fileexplorer.Remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.example.fileexplorer.BuildConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static volatile Retrofit retrofit = null;
    private static volatile RfeApiService apiService = null;


    private ApiClient() {
    }


    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);


                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(5, TimeUnit.MINUTES)
                            .writeTimeout(10, TimeUnit.MINUTES)
                            .build();

                    Gson gson = new GsonBuilder().setLenient().registerTypeAdapter(Instant.class, new InstantTypeAdapter()).create();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.BASE_URL + "/")
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }


    public static RfeApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    apiService = getRetrofit().create(RfeApiService.class);
                }
            }
        }
        return apiService;
    }


    private static final class InstantTypeAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString()); // e.g. "2026-07-13T14:30:00Z"
            }

        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if(in.peek() == JsonToken.NULL)
            {
                in.nextNull();
                return null;
            }
            return Instant.parse(in.nextString());
        }
    }
}
