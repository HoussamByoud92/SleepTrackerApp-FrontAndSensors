package com.sleeptracker.api;

import com.sleeptracker.model.ApiResponse;
import com.sleeptracker.model.SleepSession;
import com.sleeptracker.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ApiService {

    @POST("register.php")
    Call<ResponseBody> register(@Body User user);

    @POST("login.php")
    Call<User> login(@Body User user);

    @GET("get_user_sessions.php")
    Call<List<SleepSession>> getSessions(@Query("user_id") int userId);

    @POST("add_sleep_session.php")
    Call<ApiResponse> addSession(@Body SleepSession session);

}
