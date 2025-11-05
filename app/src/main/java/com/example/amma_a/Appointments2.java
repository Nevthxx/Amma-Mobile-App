package com.example.amma_a;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Appointments2 extends AppCompatActivity {

    private TextView tvPregnancyDate;
    private Button btnPickDate;
    private RecyclerView recyclerVisits;

    private Date pregnancyDate;
    private List<ClinicVisit> visitList;
    private ClinicAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments2);

        tvPregnancyDate = findViewById(R.id.tvPregnancyDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        recyclerVisits = findViewById(R.id.recyclerVisits);

        recyclerVisits.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        visitList = getSriLankaClinicVisits();
        adapter = new ClinicAdapter(visitList);
        recyclerVisits.setAdapter(adapter);

        loadDataFromFirebase();

        btnPickDate.setOnClickListener(v -> showDatePicker());
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

            // Save pregnancy date in Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("pregnancyDate", formatDate(pregnancyDate));
            db.collection("users").document(uid).set(userData, SetOptions.merge());

            // Calculate due dates and save visits
            for (int i = 0; i < visitList.size(); i++) {
                ClinicVisit v = visitList.get(i);
                int midWeek = (v.weekStart + v.weekEnd) / 2;
                v.dueDate = addWeeks(pregnancyDate, midWeek);

                Map<String, Object> visitData = new HashMap<>();
                visitData.put("name", v.name);
                visitData.put("done", v.done);
                visitData.put("dueDate", formatDate(v.dueDate));

                db.collection("users").document(uid)
                        .collection("clinicVisits").document("visit_" + i)
                        .set(visitData, SetOptions.merge());
            }

            adapter.notifyDataSetChanged();
        }, y, m, d).show();
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

                                for (ClinicVisit v : visitList) {
                                    int midWeek = (v.weekStart + v.weekEnd) / 2;
                                    v.dueDate = addWeeks(pregnancyDate, midWeek);
                                }

                                adapter.notifyDataSetChanged();
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                });

        db.collection("users").document(uid)
                .collection("clinicVisits").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId();
                        int idx = Integer.parseInt(id.replace("visit_", ""));
                        if (idx >= 0 && idx < visitList.size()) {
                            ClinicVisit v = visitList.get(idx);
                            Boolean done = doc.getBoolean("done");
                            v.done = done != null && done;
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private List<ClinicVisit> getSriLankaClinicVisits() {
        List<ClinicVisit> list = new ArrayList<>();
        list.add(new ClinicVisit("1st visit", 6, 8));
        list.add(new ClinicVisit("2nd visit", 12, 14));
        list.add(new ClinicVisit("3rd visit", 18, 20));
        list.add(new ClinicVisit("4th visit", 22, 24));
        list.add(new ClinicVisit("5th visit", 26, 28));
        list.add(new ClinicVisit("6th visit", 30, 32));
        list.add(new ClinicVisit("7th visit", 34, 36));
        list.add(new ClinicVisit("8th visit", 38, 40));
        return list;
    }

    private Date addWeeks(Date date, int weeks) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, weeks * 7);
        return cal.getTime();
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    // --- ClinicVisit model ---
    public static class ClinicVisit {
        public String name;
        public int weekStart;
        public int weekEnd;
        public boolean done = false;
        public Date dueDate;

        public ClinicVisit(String name, int weekStart, int weekEnd) {
            this.name = name;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
        }
    }

    // --- Adapter ---
    class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.VH> {

        private final List<ClinicVisit> list;

        public ClinicAdapter(List<ClinicVisit> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_appointments2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ClinicVisit v = list.get(position);
            holder.tvName.setText(v.name);
            holder.tvDue.setText(v.dueDate != null ?
                    "Due: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(v.dueDate)
                    : "Due: --");

            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(v.done);

            holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                v.done = isChecked;
                db.collection("users").document(uid)
                        .collection("clinicVisits").document("visit_" + position)
                        .update("done", isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDue;
            CheckBox cbDone;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvVisitName);
                tvDue = itemView.findViewById(R.id.tvDueDate);
                cbDone = itemView.findViewById(R.id.cbDone);
            }
        }
    }
}
