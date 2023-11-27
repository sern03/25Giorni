package com.sern.app_ila;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Home extends AppCompatActivity implements DateAdapter.OnDateClickListener {

    ImageButton buttonInfo;
    RecyclerView dateRecyclerView;

    ProgressBar progressBar;

    String selectedDate;

    TextView textViewCounter, textViewTextTitolo;

    DatabaseReference percorsoDay;

    LocalDate currentDate;


            //dayly
    TextView textViewDayOfMonth, textViewSottotitolo;

    public static final String SHARED_PREFS = "sharedPrefs";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);
        textViewCounter = findViewById(R.id.textViewCounter);
        textViewTextTitolo = findViewById(R.id.textViewTextTitolo);

        textViewDayOfMonth = findViewById(R.id.textViewDayOfMonth);
        textViewSottotitolo = findViewById(R.id.textViewSottotitolo);

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
        int currentDayOfMonth = currentDate.getDayOfMonth(),
            currentMonth = currentDate.getMonthValue(),
            currentYear = currentDate.getYear();

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




        if(currentMonth == 12 && currentYear == 2023 && currentDayOfMonth <= 25 && currentDayOfMonth >= 1){  //se la data è supportata
            dateRecyclerView.smoothScrollToPosition(currentDayOfMonth);

            loadDay(String.valueOf(currentDayOfMonth));

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("ultimoGiorno", currentDayOfMonth);
            editor.apply();

        }else{      //se la data non è supportata, mostra l'ultima data disponibile (1 di default, 25 se superato natale)
            int lastDay = sharedPreferences.getInt("ultimoGiorno", 1);

            textViewDayOfMonth.setText(String.valueOf(lastDay));

            loadDay(String.valueOf(lastDay));

        }








    }

    private void loadDay(String dayOfMonth){
        percorsoDay = FirebaseDatabase.getInstance().getReference().child(String.valueOf(dayOfMonth));
        textViewDayOfMonth.setText(dayOfMonth);

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
        loadDay(date);
    }
}




class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private List<String> dateList;
    private OnDateClickListener onDateClickListener;

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