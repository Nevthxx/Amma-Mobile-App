package com.example.amma_a;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class grid_view extends AppCompatActivity {

    CardView appointment, countdown, record, info, vaccine, notifications;
    Button logoutButton;
    TextView tvNotificationTitle, tvNotificationMessage;

    FirebaseFirestore db;
    FirebaseAuth auth;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_view);

        appointment = findViewById(R.id.appointment);
        countdown = findViewById(R.id.countdown);
        record = findViewById(R.id.record);
        info = findViewById(R.id.info);
        vaccine = findViewById(R.id.vaccine);
        notifications = findViewById(R.id.notifications);
        logoutButton = findViewById(R.id.logout_button);

        tvNotificationTitle = findViewById(R.id.tvNotificationTitle);
        tvNotificationMessage = findViewById(R.id.tvNotificationMessage);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : "demoUser";

        // --- Navigation ---
        appointment.setOnClickListener(v -> startActivity(new Intent(this, Appointment.class)));
        countdown.setOnClickListener(v -> startActivity(new Intent(this, Countdown.class)));
        record.setOnClickListener(v -> startActivity(new Intent(this, JourneyJournal.class)));
        info.setOnClickListener(v -> startActivity(new Intent(this, InfoHub.class)));
        vaccine.setOnClickListener(v -> startActivity(new Intent(this, VaccinePlanner.class)));

        // Logout
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(grid_view.this, LoginActivity.class));
            finish();
        });

        // --- Load upcoming appointment for notification ---
        loadNextAppointment();
    }

    private void loadNextAppointment() {
        db.collection("users").document(uid)
                .collection("appointments")
                .orderBy("date") // assuming "date" field exists
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        String dateStr = doc.getString("date"); // yyyy-MM-dd
                        String title = doc.getString("title");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            Date appointmentDate = sdf.parse(dateStr);

                            // Calculate 1 day before
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(appointmentDate);
                            cal.add(Calendar.DAY_OF_YEAR, -1);
                            Date reminderDate = cal.getTime();

                            // Today
                            Date today = new Date();
                            if (isSameDay(reminderDate, today)) {
                                tvNotificationTitle.setText("Upcoming Appointment");
                                tvNotificationMessage.setText(title + " tomorrow!");
                            } else {
                                tvNotificationTitle.setText("Next Reminder");
                                tvNotificationMessage.setText("No upcoming reminders");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        tvNotificationTitle.setText("Next Reminder");
                        tvNotificationMessage.setText("No upcoming reminders");
                    }
                });
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(d1);
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
