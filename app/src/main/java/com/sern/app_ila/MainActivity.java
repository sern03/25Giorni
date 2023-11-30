package com.sern.app_ila;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {

    Intent Home;
    Button buttonContinua;

    SharedPreferences sharedPreferences;

    public static final String SHARED_PREFS = "sharedPrefs";


    int giorniMancantiDaVerificare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Home = new Intent(MainActivity.this, Home.class);

        if(sharedPreferences.getBoolean("primoAccesso", true) || sharedPreferences.getBoolean("ritorno", true)){  //primo accesso

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("ritorno", false);
            editor.apply();

            setContentView(R.layout.activity_main);
            buttonContinua = findViewById(R.id.buttonContinua);

            buttonContinua.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("primoAccesso", false);
                    editor.apply();

                    if (isNetworkAvailable()) {
                        verificaParallelismoDatabase();
                    }else{
                        avviaHome();
                    }



                }
            });
        }else{                                       //non è il primo accesso

            if (isNetworkAvailable()) {
                verificaParallelismoDatabase();
            }else{
                avviaHome();
            }

        }


    }

    private void verificaParallelismoDatabase() {   //verifica se i dati selvati in locale sono gli stessi del db
        LocalDate currentDate = LocalDate.now();
        LocalDate inizioAvvento = LocalDate.of(2023, 12, 1);
        LocalDate dataNatale2023 = LocalDate.of(2023, 12, 25);

        if(currentDate.isAfter(inizioAvvento) || currentDate.isEqual(inizioAvvento)){         //se l'avvento è iniziato


            DatabaseReference percorsoDay = FirebaseDatabase.getInstance().getReference();
            int giornoMax;

            if(currentDate.isBefore(dataNatale2023) || currentDate.isEqual(dataNatale2023)){   //se stai entro le date dell'avvento
                giornoMax = currentDate.getDayOfMonth();   //il giorno massimo è quello corrente
            }else{    //avvento finito
                giornoMax = 25;
            }

            giorniMancantiDaVerificare = giornoMax;

            for(int i = 1; i<=giornoMax; i++){
                if(!sharedPreferences.contains(String.valueOf(i)+"/TestoAugurio")){
                    if(i==1) setContentView(R.layout.activity_riprogrammazione_db);          //activity di caricamento (solo una volta ovviamente)
                    //se su sharedPreferences il giorno che dovrebbe essere già stato scaricato, non c'è
                    int finalI = i;
                    percorsoDay.child(String.valueOf(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(!snapshot.child("Modificato").getValue(Boolean.class)){    //se il flag "modificato" è false
                                percorsoDay.child(String.valueOf(finalI)).child("Modificato").setValue(true) //rendilo "true"
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                giorniMancantiDaVerificare--;
                                                if(giorniMancantiDaVerificare == 0){
                                                    avviaHome();
                                                }
                                            }
                                        });
                            }else{               //se il flag "modificato" è true
                                giorniMancantiDaVerificare--;
                                if(giorniMancantiDaVerificare == 0){
                                    avviaHome();
                                }
                            }



                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });

                }else{
                    giorniMancantiDaVerificare--;

                    if(giorniMancantiDaVerificare == 0){
                        avviaHome();
                    }

                }
            }

        }else{
            avviaHome();
        }

    }

    private void avviaHome() {
        startActivity(Home);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}