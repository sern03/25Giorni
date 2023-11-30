package com.sern.app_ila;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Home extends AppCompatActivity implements DateAdapter.OnDateClickListener {

    SharedPreferences sharedPreferences;
    ImageButton buttonInfo;
    RecyclerView dateRecyclerView;

    SwipeRefreshLayout swipeRefreshLayout;

    ProgressBar progressBar, progressBar2;

    LocalDate currentDate;

    TextView textViewCounter, textViewTextTitolo;

    DatabaseReference percorsoDay;


    //dayly
    TextView textViewDayOfMonth, textViewSottotitolo,
            textViewAugurio,
            textViewTesto;
    Button buttonLink;
    ImageView imageView;
    String bitmapString, link, testo, testoAugurio;
    ScrollView scrollView;
    Bitmap imageBitmap;

    boolean clickPerMessaggio = false;

    ArrayList<String> listaMessaggiDataNonSbloccata = new ArrayList<>();

    public static final String SHARED_PREFS = "sharedPrefs";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        progressBar2 = findViewById(R.id.progressBar2);
        textViewCounter = findViewById(R.id.textViewCounter);
        textViewTextTitolo = findViewById(R.id.textViewTextTitolo);

        textViewDayOfMonth = findViewById(R.id.textViewDayOfMonth);
        textViewSottotitolo = findViewById(R.id.textViewSottotitolo);
        textViewAugurio = findViewById(R.id.textViewAugurio);
        buttonLink = findViewById(R.id.buttonLink);
        textViewTesto = findViewById(R.id.textViewTesto);
        imageView = findViewById(R.id.imageView);
        scrollView = findViewById(R.id.scrollView);

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Errore di connessione", Toast.LENGTH_SHORT).show();
        }

        populateArrayMessaggi();

        buttonInfo = findViewById(R.id.buttonInfo);
        buttonInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ritorno", true);
                editor.apply();

                Intent Info = new Intent(Home.this, MainActivity.class);
                startActivity(Info);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        dateRecyclerView = findViewById(R.id.dateRecyclerView);
        initializeDatePicker();

        // ottengo il giorno corrente e se è supportato procedo
        currentDate = LocalDate.now();
        LocalDate dataNatale2023 = LocalDate.of(2023, 12, 25);
        LocalDate inizioAvvento = LocalDate.of(2023, 12, 1);

        int currentDayOfMonth = currentDate.getDayOfMonth();

        //AZIONI TITOLO
        if(dataNatale2023.isAfter(currentDate)){ //natale deve arrivare
            textViewCounter.setText(String.valueOf(ChronoUnit.DAYS.between(currentDate, dataNatale2023)));
        }else if(dataNatale2023.isBefore(currentDate)){  //passato
            textViewCounter.setText("");
            textViewTextTitolo.setText("Natale 2023 finito \uD83E\uDD72 \uD83C\uDF84 ");
            textViewTextTitolo.setTextSize(22);
        }else{        //giorno di natale
            textViewCounter.setText("");
            textViewTextTitolo.setText("È Natale!! \uD83C\uDF81 \uD83C\uDF84");
        }


        //AZIONI loadDay()
        if(currentDate.isAfter(inizioAvvento) || currentDate.isEqual(inizioAvvento)
                && currentDate.isBefore(dataNatale2023) || currentDate.isEqual(dataNatale2023)){  //se la data è supportata
            //applicazione in esecuzioni nei giorni di avvendo esatti
            dateRecyclerView.smoothScrollToPosition(currentDayOfMonth);

            loadDay(String.valueOf(currentDayOfMonth), false);


        }else{  //data non supportata

            if(currentDate.isBefore(inizioAvvento)){   //se si usa l'app prima dell'inizio del calendario
                loadDay("1", true);
            }else if(currentDate.isAfter(inizioAvvento)){  //se si usa l'app dopo la fine del calendario
                loadDay("25", false);   //carica l'ultimo giorno del calendario
            }

        }

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadDay(String.valueOf(currentDayOfMonth), false);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        buttonLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!clickPerMessaggio){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                }else{
                    loadDay(String.valueOf(26), true);
                    clickPerMessaggio = false;
                }

            }
        });

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }


    private void loadDay(String dayOfMonth, boolean callDaClick){
        if(!clickPerMessaggio) textViewDayOfMonth.setText(dayOfMonth);

        textViewAugurio.setText("");

        //(questo controllo è utile solo nel caso in cui si clicchi una data nel calendario o si apra prima dell'1)
        if(currentDate.isBefore(LocalDate.of(2023, 12, Integer.parseInt(dayOfMonth)))){
            //se la data cliccata (dayOfMonth) è maggiore a quella corrente -> data non sbloccata
            swipeRefreshLayout.setEnabled(true);  //lo riattivo

            buttonLink.setVisibility(View.GONE);
            scrollView.setVisibility(View.INVISIBLE);

            if(callDaClick){    //se si ha cliccato su una data
                int counterMessaggi = sharedPreferences.getInt("counterTentativi", 0);
                testoAugurio = listaMessaggiDataNonSbloccata.get(counterMessaggi);     //stampa il prossimo messaggio
                textViewAugurio.setText(testoAugurio);

                if(counterMessaggi == 6){
                    buttonLink.setVisibility(View.VISIBLE);
                    clickPerMessaggio = true;
                }

                if(counterMessaggi < 25){   //ci sono 24 messaggi in totale
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("counterTentativi", counterMessaggi+1);   //counter per messaggi quando si prova ad aprire una data bloccata
                    editor.apply();
                }

            }

        }else {                                         //data supportata
            textViewDayOfMonth.setText(dayOfMonth);
            progressBar.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(false);  //lo disabilito per lo scorrimento della scrollView

            link = "";
            buttonLink.setVisibility(View.GONE);
            testoAugurio = "";
            textViewAugurio.setText(testoAugurio);
            scrollView.setVisibility(View.INVISIBLE);

            if (!isNetworkAvailable()) {                                //se non c'è connessione
                Toast.makeText(this, "Errore di connessione\nimpossibile aggiornare", Toast.LENGTH_SHORT).show();

                populateFromLocal(dayOfMonth);

            } else {                                          //c'è connessione
                populateFromLocal(dayOfMonth);
                progressBar2.setVisibility(View.VISIBLE);

                percorsoDay = FirebaseDatabase.getInstance().getReference().child(String.valueOf(dayOfMonth));

                percorsoDay.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.child("Modificato").getValue(Boolean.class)) {

                            percorsoDay.child("Modificato").setValue(false)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            bitmapString = snapshot.child("Foto").getValue().toString();
                                            link = snapshot.child("Link").getValue().toString();
                                            testo = snapshot.child("Testo").getValue().toString();
                                            testoAugurio = snapshot.child("TestoAugurio").getValue().toString();

                                            populateDay();

                                            progressBar2.setVisibility(View.INVISIBLE);

                                            saveOnLocal();

                                        }
                                    });
                        } else {
                            progressBar2.setVisibility(View.INVISIBLE);
                        }

                    }

                    private void saveOnLocal() {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(dayOfMonth + "/Foto", bitmapString);
                        editor.putString(dayOfMonth + "/Link", link);
                        editor.putString(dayOfMonth + "/Testo", testo);
                        editor.putString(dayOfMonth + "/TestoAugurio", testoAugurio);
                        editor.apply();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Home.this, "Errore di connessione", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        }

    }

    private void populateFromLocal(String dayOfMonth) {    //carica il giorno da locale
        bitmapString = sharedPreferences.getString(dayOfMonth + "/Foto", "");
        link = sharedPreferences.getString(dayOfMonth + "/Link", "");
        testo = sharedPreferences.getString(dayOfMonth + "/Testo", "");
        testoAugurio = sharedPreferences.getString(dayOfMonth + "/TestoAugurio", "");

        populateDay();
    }

    private void populateDay() {           //carica giorno
        textViewTesto.setText(Html.fromHtml("<br>" + testo));
        textViewAugurio.setText(Html.fromHtml(testoAugurio));

        if(!bitmapString.matches("")){ // se è stata condivisa una foto
            convertiBitmap();
        }else{                   //bitmap non condiviso
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
        }

        if(!link.matches("")){   // se è stato condiviso un link
            buttonLink.setVisibility(View.VISIBLE);
        }else{        //link non condiviso
            buttonLink.setVisibility(View.GONE);
        }

        scrollView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void convertiBitmap() {
        try {
            byte [] encodeByte = Base64.decode(bitmapString,Base64.DEFAULT);
            imageBitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            imageView.setImageBitmap(imageBitmap);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setVisibility(View.VISIBLE);
        }catch (Exception ignored){

        }

    }

    private void populateArrayMessaggi() {
        listaMessaggiDataNonSbloccata.add("Questo giorno è ancora bloccato");
        listaMessaggiDataNonSbloccata.add("È inutile che ci provi");
        listaMessaggiDataNonSbloccata.add("Smettila di cliccare altre date");
        listaMessaggiDataNonSbloccata.add("Sono bloccate");
        listaMessaggiDataNonSbloccata.add("Vuoi continuare ancora per molto?");
        listaMessaggiDataNonSbloccata.add("...");
        listaMessaggiDataNonSbloccata.add("Va bene dai...");
        listaMessaggiDataNonSbloccata.add("Scemo chi legge");
        listaMessaggiDataNonSbloccata.add("Dai faceva ridere");
        listaMessaggiDataNonSbloccata.add("E niente insisti...");
        listaMessaggiDataNonSbloccata.add("Provaci ancora e non ti parlo più");
        listaMessaggiDataNonSbloccata.add("ok");
        listaMessaggiDataNonSbloccata.add("");
        listaMessaggiDataNonSbloccata.add("");
        listaMessaggiDataNonSbloccata.add("Daiiii");
        listaMessaggiDataNonSbloccata.add("Vabe");
        listaMessaggiDataNonSbloccata.add("Ci rinuncio");
        listaMessaggiDataNonSbloccata.add("Davvero sei arrivata fino a qui");
        listaMessaggiDataNonSbloccata.add("Credevo mollassi prima");
        listaMessaggiDataNonSbloccata.add("Ti amo");
        listaMessaggiDataNonSbloccata.add("Tanto");
        listaMessaggiDataNonSbloccata.add("Ora la smetti di provarci?");
        listaMessaggiDataNonSbloccata.add("Vabbo");
        listaMessaggiDataNonSbloccata.add("Ora vado davvero");
        listaMessaggiDataNonSbloccata.add("Ciau");
        listaMessaggiDataNonSbloccata.add("Devi aspettareeeee");
    }

    private void initializeDatePicker(){
        dateRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<String> dateList = new ArrayList<>();
        List<String> daysOfWeek = new ArrayList<>();
        String dayOfWeek;
        daysOfWeek.add("Lun");
        daysOfWeek.add("Mar");
        daysOfWeek.add("Mer");
        daysOfWeek.add("Gio");
        daysOfWeek.add("Ven");
        daysOfWeek.add("Sab");
        daysOfWeek.add("Dom");

        int i2 = 4;
        for (int i = 1; i <= 25; i++) {

            if(i2 == 7) i2 = 0;
            dayOfWeek = daysOfWeek.get(i2);

            dateList.add(i + " " + dayOfWeek);

            i2++;
        }
        DateAdapter dateAdapter = new DateAdapter(dateList, this);
        dateRecyclerView.setAdapter(dateAdapter);

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDateClick(String date) {
        loadDay(date, true);
    }
}




class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private final List<String> dateList;
    private final OnDateClickListener onDateClickListener;

    public DateAdapter(List<String> dateList, OnDateClickListener onDateClickListener) {
        this.dateList = dateList;
        this.onDateClickListener = onDateClickListener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        String date = dateList.get(position);
        holder.bind(date);
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public class DateViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView textViewDayOfWeek, textViewNum;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDayOfWeek = itemView.findViewById(R.id.textViewDayOfWeek);
            textViewNum = itemView.findViewById(R.id.textViewNum);
            itemView.setOnClickListener(this);
        }

        public void bind(String date) {
            textViewDayOfWeek.setText(date.split(" ")[1]);
            textViewNum.setText(date.split(" ")[0]);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition() + 1;
            if (position != RecyclerView.NO_POSITION) {
                onDateClickListener.onDateClick(String.valueOf(position));
            }
        }
    }

    public interface OnDateClickListener {
        void onDateClick(String date);
    }
}