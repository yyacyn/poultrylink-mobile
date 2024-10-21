package com.example.splashscreen;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.airbnb.lottie.LottieAnimationView;

public class SplashScreenActivity extends AppCompatActivity {
    Integer t = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);

        // Memulai animasi
        lottieAnimationView.playAnimation();

        // Pindah Halaman
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreenActivity.this, PilihanLoginActivity.class); // dari mana ke halaman
                                                                                              // mana
                startActivity(i);
                finish();
            }
        },
                t);

    };
}