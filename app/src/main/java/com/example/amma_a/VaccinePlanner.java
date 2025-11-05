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

public class VaccinePlanner extends AppCompatActivity {

    private TextView tvBirthDate;
    private Button btnPickDate;
    private RecyclerView recyclerVaccines;

    private Date birthDate;
    private List<Vaccine> vaccineList;
    private VaccineAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccine_planner);

        tvBirthDate = findViewById(R.id.tvBirthDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        recyclerVaccines = findViewById(R.id.recyclerVaccines);

        recyclerVaccines.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        vaccineList = getSriLankaVaccines();
        adapter = new VaccineAdapter(vaccineList);
        recyclerVaccines.setAdapter(adapter);

        // Load user-specific data from Firebase
        loadDataFromFirebase();

        btnPickDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        if (birthDate != null) cal.setTime(birthDate);

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            birthDate = cal.getTime();
            tvBirthDate.setText("Birth Date: " + formatDate(birthDate));

            // Save birth date in Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("birthDate", formatDate(birthDate));
            db.collection("users").document(uid).set(userData, SetOptions.merge());

            // Calculate due dates and save vaccines
            for (int i = 0; i < vaccineList.size(); i++) {
                Vaccine v = vaccineList.get(i);
                v.dueDate = addDays(birthDate, v.daysAfterBirth);

                Map<String, Object> vaccineData = new HashMap<>();
                vaccineData.put("name", v.name);
                vaccineData.put("dueDate", formatDate(v.dueDate));
                vaccineData.put("done", v.done);

                db.collection("users").document(uid)
                        .collection("vaccines").document("vaccine_" + i)
                        .set(vaccineData);
            }

            adapter.notifyDataSetChanged();
        }, y, m, d).show();
    }

    private void loadDataFromFirebase() {
        // Load birth date
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String birth = document.getString("birthDate");
                        if (birth != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                birthDate = sdf.parse(birth);
                                tvBirthDate.setText("Birth Date: " + formatDate(birthDate));

                                for (Vaccine v : vaccineList) {
                                    v.dueDate = addDays(birthDate, v.daysAfterBirth);
                                }

                                adapter.notifyDataSetChanged();
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                });

        // Load vaccine checkboxes
        db.collection("users").document(uid)
                .collection("vaccines").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId(); // vaccine_0, vaccine_1...
                        int idx = Integer.parseInt(id.replace("vaccine_", ""));
                        if (idx >= 0 && idx < vaccineList.size()) {
                            Vaccine v = vaccineList.get(idx);
                            Boolean done = doc.getBoolean("done");
                            v.done = done != null && done;
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private List<Vaccine> getSriLankaVaccines() {
        List<Vaccine> list = new ArrayList<>();
        list.add(new Vaccine("BCG", 0, "At birth"));
        list.add(new Vaccine("Pentavalent + OPV (1st dose)", 60, "2 months"));
        list.add(new Vaccine("Pentavalent + OPV (2nd dose)", 120, "4 months"));
        list.add(new Vaccine("Pentavalent + OPV (3rd dose)", 180, "6 months"));
        list.add(new Vaccine("Measles (1st dose)", 270, "9 months"));
        list.add(new Vaccine("Japanese Encephalitis", 365, "12 months"));
        list.add(new Vaccine("DTP + OPV booster", 540, "18 months"));
        list.add(new Vaccine("Measles-Rubella (MR)", 1095, "3 years"));
        list.add(new Vaccine("DT + OPV booster", 1825, "5 years"));
        list.add(new Vaccine("aTd booster", 4015, "10–11 years"));
        return list;
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    // --- Vaccine model ---
    public static class Vaccine {
        public String name;
        public int daysAfterBirth;
        public String description;
        public boolean done = false;
        public Date dueDate;

        public Vaccine(String name, int daysAfterBirth, String description) {
            this.name = name;
            this.daysAfterBirth = daysAfterBirth;
            this.description = description;
        }
    }

    // --- Adapter ---
    class VaccineAdapter extends RecyclerView.Adapter<VaccineAdapter.VH> {

        private final List<Vaccine> list;

        public VaccineAdapter(List<Vaccine> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vaccine, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Vaccine v = list.get(position);
            holder.tvName.setText(v.name);
            holder.tvDesc.setText(v.description);
            holder.tvDue.setText(v.dueDate != null ?
                    "Due: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(v.dueDate)
                    : "Due: --");

            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(v.done);

            holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                v.done = isChecked;

                // Update Firestore for this user
                db.collection("users").document(uid)
                        .collection("vaccines").document("vaccine_" + position)
                        .update("done", isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvDue;
            CheckBox cbDone;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvVaccineName);
                tvDesc = itemView.findViewById(R.id.tvDescription);
                tvDue = itemView.findViewById(R.id.tvDueDate);
                cbDone = itemView.findViewById(R.id.cbDone);
            }
        }
    }
}

