package com.example.amma_a;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JourneyJournal extends AppCompatActivity {

    private RecyclerView recyclerQuestions;
    private QuestionAdapter adapter;
    private List<String> questions;
    private List<String> answers;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey_journal);

        recyclerQuestions = findViewById(R.id.recyclerQuestions);
        recyclerQuestions.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fill all your questions
        questions = new ArrayList<>();
        questions.add("Name");
        questions.add("Age");
        questions.add("Parity");
        questions.add("Number of living children");
        questions.add("Address");
        questions.add("Distance from hospital");
        questions.add("Occupation");
        questions.add("Pre pregnancy counselling received or not");
        questions.add("Folic acid taken before conceiving or not");
        questions.add("Was she vaccinated against rubella? If yes, when?");
        questions.add("First day of last menstrual period");
        questions.add("Problems in first trimester");
        questions.add("Period of pregnancy in first visit");
        questions.add("Tests done");
        questions.add("Dating USS between 10-14 weeks");
        questions.add("How many foetuses?");
        questions.add("Were you informed about the dates?");
        questions.add("Treatment taken (Folic acid 1mg or 5mg)");
        questions.add("Any other treatment");
        questions.add("Each visit period (second trimester)");
        questions.add("Blood pressure");
        questions.add("Growth of fundus");
        questions.add("Other observations");
        questions.add("Place of attendance");
        questions.add("Did midwife visit your home? How many times?");
        questions.add("Vitamins taken");
        questions.add("Any other medication");
        questions.add("Investigations done");
        questions.add("Anomaly Scan at 20 weeks?");
        questions.add("Clinic visits (place/by whom/period)");
        questions.add("Problems identified");
        questions.add("Hospital admissions? (reason)");
        questions.add("Tetanus injections taken? How many?");
        questions.add("Growth scans done?");
        questions.add("Visits (third trimester)");
        questions.add("Treatments taken");
        questions.add("Investigations done");
        questions.add("Any family problems");
        questions.add("Occupation (third trimester)");
        questions.add("Level of education");
        questions.add("Any psychological issues in past?");
        questions.add("Any allergy");
        questions.add("Family history of illness");
        questions.add("Overall impression of care received");
        questions.add("Suggestions to improve care");
        questions.add("Awareness of family planning");
        questions.add("Awareness of newborn care");

        answers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) answers.add("");

        adapter = new QuestionAdapter(questions, answers, uid);
        recyclerQuestions.setAdapter(adapter);

        loadAnswersFromFirestore();
    }

    private void loadAnswersFromFirestore() {
        db.collection("users").document(uid)
                .collection("questionnaire")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId();
                        int idx;
                        try {
                            idx = Integer.parseInt(id.replace("question_", ""));
                        } catch (Exception e) {
                            continue;
                        }
                        if (idx >= 0 && idx < answers.size()) {
                            String ans = doc.getString("answer");
                            answers.set(idx, ans != null ? ans : "");
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load answers", Toast.LENGTH_SHORT).show());
    }

    // --- Adapter ---
    class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QVH> {

        private final List<String> questions;
        private final List<String> answers;
        private final String uid;

        public QuestionAdapter(List<String> questions, List<String> answers, String uid) {
            this.questions = questions;
            this.answers = answers;
            this.uid = uid;
        }

        @NonNull
        @Override
        public QVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_question, parent, false);
            return new QVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull QVH holder, int position) {
            holder.tvQuestion.setText(questions.get(position));

            // Remove old listener before setting new text
            if (holder.textWatcher != null)
                holder.etAnswer.removeTextChangedListener(holder.textWatcher);

            holder.etAnswer.setText(answers.get(position));

            // Create and attach new TextWatcher
            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        answers.set(pos, s.toString());
                        Map<String, Object> data = new HashMap<>();
                        data.put("answer", s.toString());
                        db.collection("users").document(uid)
                                .collection("questionnaire")
                                .document("question_" + pos)
                                .set(data);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) { }
            };

            holder.etAnswer.addTextChangedListener(holder.textWatcher);
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }

        // --- ViewHolder ---
        class QVH extends RecyclerView.ViewHolder {
            TextView tvQuestion;
            EditText etAnswer;
            TextWatcher textWatcher; // 🟢 store reference to prevent duplication

            public QVH(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tvQuestion);
                etAnswer = itemView.findViewById(R.id.etAnswer);
            }
        }
    }
}
