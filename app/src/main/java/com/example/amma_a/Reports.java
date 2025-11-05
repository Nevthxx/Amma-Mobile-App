package com.example.amma_a;

import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Reports extends AppCompatActivity {

    private RecyclerView recyclerLabVaccine;
    private LabVaccineAdapter adapter;
    private List<Item> itemList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private static final String TAG = "Reports";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        recyclerLabVaccine = findViewById(R.id.recyclerLabVaccine);
        recyclerLabVaccine.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not signed in. Cannot write to Firestore.");
            finish();
            return;
        }
        uid = auth.getCurrentUser().getUid();

        itemList = getLabTestsAndVaccines();
        adapter = new LabVaccineAdapter(itemList);
        recyclerLabVaccine.setAdapter(adapter);

        // Load "done" states first, then save base info
        loadDoneStatesFromFirebase();
    }

    private List<Item> getLabTestsAndVaccines() {
        List<Item> list = new ArrayList<>();

        // ---- Laboratory Tests ----
        list.add(new Item("Hemoglobin (Hb)", "1st visit, 28 weeks, 36 weeks", "Detect anemia"));
        list.add(new Item("Blood Group & Rhesus Factor", "1st visit", "Identify blood group"));
        list.add(new Item("Blood Sugar (Fasting/OGTT if risk)", "24–28 weeks", "Detect gestational diabetes"));
        list.add(new Item("VDRL / RPR", "1st visit", "Detect syphilis"));
        list.add(new Item("HIV Test", "1st visit", "Screen for HIV"));
        list.add(new Item("Hepatitis B (HBsAg)", "1st visit", "Screen for Hep B"));

        // ---- Vaccines ----
        list.add(new Item("Tetanus Toxoid (TT)", "1st dose: early pregnancy; 2nd dose: 4 weeks later", "Prevent neonatal tetanus"));
        list.add(new Item("Influenza / COVID", "Seasonal or risk-based", "Protect mother and baby"));

        return list;
    }

    // --- Save static info ONLY (without done)
    private void saveBaseItemsToFirestore() {
        for (int i = 0; i < itemList.size(); i++) {
            final int index = i;
            Item it = itemList.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("name", it.name);
            data.put("timing", it.timing);
            data.put("purpose", it.purpose);
            // do NOT include "done"

            db.collection("users").document(uid)
                    .collection("labvaccine").document("item_" + index)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved item_" + index))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save item_" + index, e));
        }
    }

    private void loadDoneStatesFromFirebase() {
        db.collection("users").document(uid)
                .collection("labvaccine").get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId(); // item_0, item_1...
                        int idx;
                        try {
                            idx = Integer.parseInt(id.replace("item_", ""));
                        } catch (Exception e) { continue; }
                        if (idx >= 0 && idx < itemList.size()) {
                            Item it = itemList.get(idx);
                            Boolean done = doc.getBoolean("done");
                            it.done = done != null && done;

                            String name = doc.getString("name");
                            String timing = doc.getString("timing");
                            String purpose = doc.getString("purpose");
                            if (name != null) it.name = name;
                            if (timing != null) it.timing = timing;
                            if (purpose != null) it.purpose = purpose;
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Only now save static info (merge)
                    saveBaseItemsToFirestore();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load labvaccine data", e));
    }

    // --- Model ---
    public static class Item {
        public String name;
        public String timing;
        public String purpose;
        public boolean done = false;

        public Item() {}
        public Item(String name, String timing, String purpose) {
            this.name = name;
            this.timing = timing;
            this.purpose = purpose;
        }
    }

    // --- Adapter ---
    class LabVaccineAdapter extends RecyclerView.Adapter<LabVaccineAdapter.VH> {
        private final List<Item> list;

        LabVaccineAdapter(List<Item> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reports, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Item it = list.get(position);
            holder.tvName.setText(it.name);
            holder.tvTiming.setText("Timing: " + it.timing);
            holder.tvPurpose.setText("Purpose: " + it.purpose);

            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(it.done);

            holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                it.done = isChecked;
                db.collection("users").document(uid)
                        .collection("labvaccine").document("item_" + position)
                        .update("done", isChecked)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update done for item_" + position, e));
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvTiming, tvPurpose;
            CheckBox cbDone;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvTiming = itemView.findViewById(R.id.tvTiming);
                tvPurpose = itemView.findViewById(R.id.tvPurpose);
                cbDone = itemView.findViewById(R.id.cbDone);
            }
        }
    }
}







