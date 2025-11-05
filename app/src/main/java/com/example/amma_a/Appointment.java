package com.example.amma_a;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class Appointment extends AppCompatActivity {

    CardView appointment, scans, reports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment); // Correct layout

        // Initialize CardViews
        appointment = findViewById(R.id.appointments2);
        scans = findViewById(R.id.scans);
        reports = findViewById(R.id.reports);

        // Set click listeners
        appointment.setOnClickListener(view -> {
            Intent intent = new Intent(Appointment.this, Appointments2.class);
            startActivity(intent);
        });

        scans.setOnClickListener(view -> {
            Intent intent = new Intent(Appointment.this, Scans.class);
            startActivity(intent);
        });

        reports.setOnClickListener(view -> {
            Intent intent = new Intent(Appointment.this, Reports.class);
            startActivity(intent);
        });
    }
}

