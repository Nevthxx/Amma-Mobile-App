package com.example.amma_a;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class notification extends AppCompatActivity {

    private static final String TAG = "Scans";

    private TextView tvPregnancyDate;
    private Button btnPickDate;
    private RecyclerView recyclerScans;

    private Date pregnancyDate;
    private List<Scan> scanList;
    private ScanAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scans);

        tvPregnancyDate = findViewById(R.id.tvPregnancyDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        recyclerScans = findViewById(R.id.recyclerScans);
        recyclerScans.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not signed in. Cannot write to Firestore.");
            finish();
            return;
        }
        uid = auth.getCurrentUser().getUid();

        scanList = getScans();
        adapter = new ScanAdapter(scanList);
        recyclerScans.setAdapter(adapter);

        // Load pregnancy date first, then load scans
        loadPregnancyDateAndScans();

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
            String formattedDate = sdf.format(pregnancyDate);

            tvPregnancyDate.setText("Pregnancy Start Date: " + formattedDate);

            // Save pregnancy date in Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("pregnancyDate", formattedDate);
            db.collection("users").document(uid).set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Pregnancy date saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save pregnancy date", e));

            // Update scan due dates & save
            saveScanBaseData();
            adapter.notifyDataSetChanged();

        }, y, m, d).show();
    }

    private void loadPregnancyDateAndScans() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("pregnancyDate")) {
                        String dateStr = doc.getString("pregnancyDate");
                        try {
                            pregnancyDate = sdf.parse(dateStr);
                            tvPregnancyDate.setText("Pregnancy Start Date: " + dateStr);
                        } catch (ParseException e) {
                            Log.e(TAG, "Invalid date format in Firestore", e);
                        }
                    }
                    // After loading date, load scan states
                    loadDoneStatesFromFirebase();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load pregnancy date", e);
                    loadDoneStatesFromFirebase(); // still load scans
                });
    }

    private void saveScanBaseData() {
        if (pregnancyDate == null) return;

        for (int i = 0; i < scanList.size(); i++) {
            final int index = i;
            Scan s = scanList.get(i);
            s.dueDate = addWeeks(pregnancyDate, s.week);

            Map<String, Object> data = new HashMap<>();
            data.put("name", s.name);
            data.put("purpose", s.purpose);
            data.put("details", s.details);
            data.put("dueDate", sdf.format(s.dueDate));

            db.collection("users").document(uid)
                    .collection("scans").document("scan_" + index)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved scan_" + index))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save scan_" + index, e));
        }
    }

    private void loadDoneStatesFromFirebase() {
        db.collection("users").document(uid)
                .collection("scans").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId();
                        int idx;
                        try {
                            idx = Integer.parseInt(id.replace("scan_", ""));
                        } catch (Exception e) { continue; }
                        if (idx >= 0 && idx < scanList.size()) {
                            Scan s = scanList.get(idx);
                            Boolean done = doc.getBoolean("done");
                            s.done = done != null && done;

                            String name = doc.getString("name");
                            String purpose = doc.getString("purpose");
                            String details = doc.getString("details");
                            if (name != null) s.name = name;
                            if (purpose != null) s.purpose = purpose;
                            if (details != null) s.details = details;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    saveScanBaseData(); // ensure due dates saved after load
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load scan data", e));
    }

    private List<Scan> getScans() {
        List<Scan> list = new ArrayList<>();
        list.add(new Scan("First Trimester Scan", 12,
                "Confirm gestational age and establish an accurate due date.",
                "An early ultrasound between 11–13 weeks is most reliable.", false));
        list.add(new Scan("Second Trimester Scan", 19,
                "Assess fetal anatomy and detect structural abnormalities.",
                "Typically performed around 18–22 weeks.", false));
        list.add(new Scan("Third Trimester Scan", 32,
                "Monitor fetal growth and position.",
                "Ultrasound at 28, 32, 36 weeks to track growth.", false));
        return list;
    }

    private Date addWeeks(Date date, int weeks) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, weeks * 7);
        return cal.getTime();
    }

    // --- Scan model ---
    public static class Scan {
        public String name;
        public int week;
        public String purpose;
        public String details;
        public boolean done = false;
        public Date dueDate;

        public Scan() {}
        public Scan(String name, int week, String purpose, String details, boolean done) {
            this.name = name;
            this.week = week;
            this.purpose = purpose;
            this.details = details;
            this.done = done;
        }
    }

    // --- Adapter ---
    class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.VH> {
        private final List<Scan> list;
        ScanAdapter(List<Scan> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scans, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Scan s = list.get(position);
            holder.tvName.setText(s.name);
            holder.tvPurpose.setText(s.purpose);
            holder.tvDetails.setText(s.details);
            holder.tvDue.setText(s.dueDate != null ? "Due: " + sdf.format(s.dueDate) : "Due: --");

            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(s.done);

            holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                s.done = isChecked;
                db.collection("users").document(uid)
                        .collection("scans").document("scan_" + position)
                        .update("done", isChecked)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update done for scan_" + position, e));
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPurpose, tvDetails, tvDue;
            CheckBox cbDone;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvScanName);
                tvPurpose = itemView.findViewById(R.id.tvPurpose);
                tvDetails = itemView.findViewById(R.id.tvDetails);
                tvDue = itemView.findViewById(R.id.tvDueDate);
                cbDone = itemView.findViewById(R.id.cbDone);
            }
        }
    }
}
