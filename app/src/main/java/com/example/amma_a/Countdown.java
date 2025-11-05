package com.example.amma_a;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Countdown extends AppCompatActivity {

    private TextView tvPregnancyDate, tvDueDate, tvCurrentWeek, tvTimeRemaining;
    private Button btnPickPregnancyDate;

    private Date pregnancyDate;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        tvPregnancyDate = findViewById(R.id.tvPregnancyDate);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvCurrentWeek = findViewById(R.id.tvCurrentWeek);
        tvTimeRemaining = findViewById(R.id.tvTimeRemaining);
        btnPickPregnancyDate = findViewById(R.id.btnPickPregnancyDate);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        // Load saved pregnancy date if exists
        loadDataFromFirebase();

        btnPickPregnancyDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        if (pregnancyDate != null) cal.setTime(pregnancyDate);

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            pregnancyDate = cal.getTime();
            tvPregnancyDate.setText("Pregnancy Start Date: " + formatDate(pregnancyDate));

            saveDateToFirebase(pregnancyDate);
            calculateCountdown(pregnancyDate);
        }, y, m, d).show();
    }

    private void calculateCountdown(Date startDate) {
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(startDate);
        dueCal.add(Calendar.DAY_OF_YEAR, 280); // 40 weeks

        Date today = new Date();
        long elapsedDays = (today.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
        int currentWeek = (int) (elapsedDays / 7);

        long remainingDays = (dueCal.getTimeInMillis() - today.getTime()) / (1000 * 60 * 60 * 24);
        int remainingWeeks = (int) (remainingDays / 7);
        int remainingExtraDays = (int) (remainingDays % 7);

        tvDueDate.setText("Due Date: " + formatDate(dueCal.getTime()));
        tvCurrentWeek.setText("Current Week: " + currentWeek);
        tvTimeRemaining.setText("Time until delivery: " + remainingWeeks + " weeks and " + remainingExtraDays + " days");
    }

    private void saveDateToFirebase(Date date) {
        Map<String, Object> data = new HashMap<>();
        data.put("pregnancyDate", formatDate(date));

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(Countdown.this, "Date saved to Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(Countdown.this, "Error saving date", Toast.LENGTH_SHORT).show());
    }

    private void loadDataFromFirebase() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedDate = document.getString("pregnancyDate");
                        if (savedDate != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                pregnancyDate = sdf.parse(savedDate);
                                tvPregnancyDate.setText("Pregnancy Start Date: " + formatDate(pregnancyDate));
                                calculateCountdown(pregnancyDate);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }
}
