package com.sern.app_ila;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Intent Home;
    Button buttonContinua;

    public static final String SHARED_PREFS = "sharedPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Home = new Intent(MainActivity.this, Home.class);

        if(sharedPreferences.getBoolean("primoAccesso", true) || sharedPreferences.getBoolean("ritorno", true)){

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("ritorno", false);
            editor.apply();

            setContentView(R.layout.activity_main);
            buttonContinua = findViewById(R.id.buttonContinua);

            buttonContinua.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Home);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("primoAccesso", false);
                    editor.apply();

                }
            });
        }else{
            startActivity(Home);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }


    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}