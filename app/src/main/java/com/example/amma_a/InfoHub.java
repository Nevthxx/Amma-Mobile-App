package com.example.amma_a;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoHub extends AppCompatActivity {

    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_hub);

        tvContent = findViewById(R.id.tvContent);

        String tips = "1. Importance of Prenatal Care\n" +
                "- Attend regular check-ups with your healthcare provider.\n" +
                "- Monitor your blood pressure, weight, and baby's growth.\n" +
                "- Early detection of potential complications can ensure a safer pregnancy.\n\n" +
                "2. Healthy Nutrition\n" +
                "- Eat a variety of fruits, vegetables, whole grains, and lean proteins.\n" +
                "- Include foods rich in iron, calcium, folic acid, and vitamins.\n" +
                "- Drink plenty of water; avoid sugary drinks and excessive caffeine.\n" +
                "- Limit processed foods, high-salt foods, and raw or undercooked items.\n\n" +
                "3. Supplements\n" +
                "- Folic acid: Helps prevent neural tube defects (start before conception if possible).\n" +
                "- Iron: Supports healthy blood production.\n" +
                "- Calcium and Vitamin D: Strengthens bones for both mother and baby.\n" +
                "- Always consult your doctor before taking any supplements.\n\n" +
                "4. Physical Activity\n" +
                "- Gentle exercises such as walking, swimming, or prenatal yoga are recommended.\n" +
                "- Avoid high-risk activities that may cause falls or injuries.\n" +
                "- Exercise helps with energy levels, mood, and preparation for labor.\n\n" +
                "5. Lifestyle Tips\n" +
                "- Avoid alcohol, tobacco, and recreational drugs.\n" +
                "- Limit exposure to harmful chemicals and environmental toxins.\n" +
                "- Prioritize adequate sleep and stress management.\n" +
                "- Practice good hygiene to prevent infections.\n\n" +
                "6. Warning Signs to Watch For\n" +
                "- Seek immediate medical attention if you experience:\n" +
                "  • Severe abdominal pain or cramping\n" +
                "  • Vaginal bleeding or fluid leakage\n" +
                "  • Sudden swelling of face, hands, or feet\n" +
                "  • Severe headaches or vision changes\n" +
                "  • Fever or persistent vomiting\n\n" +
                "7. Preparing for Labor\n" +
                "- Take childbirth education classes if available.\n" +
                "- Discuss birth plan options with your healthcare provider.\n" +
                "- Pack a hospital bag ahead of time.\n" +
                "- Learn about postpartum care for both you and your baby.\n\n" +
                "8. Emotional Well-being\n" +
                "- Mood swings are normal; talk to your doctor if you feel persistently sad or anxious.\n" +
                "- Stay connected with family and friends.\n" +
                "- Join prenatal support groups for advice and encouragement.\n\n" +
                "9. Vaccinations and Health Checks\n" +
                "- Stay updated on recommended vaccinations (e.g., flu, Tdap).\n" +
                "- Regularly monitor blood sugar and blood pressure.\n" +
                "- Attend any specialist appointments if advised.";

        // Apply pink color to all numbered headings (e.g., "1. Something")
        SpannableStringBuilder ssb = new SpannableStringBuilder(tips);
        Pattern pattern = Pattern.compile("(?m)^[0-9]+\\.\\s.*"); // Matches lines starting with "1. ", "2. ", etc.
        Matcher matcher = pattern.matcher(tips);

        int pinkColor = Color.parseColor("#E91E63"); // You can change to your preferred pink shade

        while (matcher.find()) {
            ssb.setSpan(new ForegroundColorSpan(pinkColor),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvContent.setText(ssb);
    }
}

