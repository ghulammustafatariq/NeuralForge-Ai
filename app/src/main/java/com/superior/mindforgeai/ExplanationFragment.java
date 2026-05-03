package com.superior.mindforgeai;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

public class ExplanationFragment extends Fragment {

    public static ExplanationFragment newInstance(String explanationJson) {
        ExplanationFragment fragment = new ExplanationFragment();
        Bundle args = new Bundle();
        args.putString("explanation_json", explanationJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explanation, container, false);
        LinearLayout containerView = view.findViewById(R.id.explanationContainer);

        if (getArguments() != null) {
            String jsonData = getArguments().getString("explanation_json");
            if (jsonData != null && !jsonData.isEmpty()) {
                try {
                    JSONArray sections = new JSONArray(jsonData);
                    for (int i = 0; i < sections.length(); i++) {
                        JSONObject section = sections.optJSONObject(i);
                        if (section == null) continue;
                        addSectionCard(inflater, containerView, section.optString("heading", ""), section.optString("content", ""), i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        MaterialButton btnAdd = new MaterialButton(requireContext());
        btnAdd.setText("+ Add Section");
        btnAdd.setTextColor(Color.parseColor("#6800FF"));
        btnAdd.setStrokeColorResource(android.R.color.transparent);
        btnAdd.setBackgroundColor(Color.TRANSPARENT);
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setTextSize(14);
        btnAdd.setOnClickListener(v -> {
            if (getActivity() instanceof AddContentCallback) {
                ((AddContentCallback) getActivity()).onAddExplanation();
            }
        });
        containerView.addView(btnAdd);

        return view;
    }

    private void addSectionCard(LayoutInflater inflater, LinearLayout container, String heading, String content, int index) {
        View card = inflater.inflate(R.layout.item_section_card, container, false);
        ((TextView) card.findViewById(R.id.tvSectionHeading)).setText(heading);
        Spanned formatted = Html.fromHtml(content.replace("\n", "<br/>"), Html.FROM_HTML_MODE_LEGACY);
        ((TextView) card.findViewById(R.id.tvSectionContent)).setText(formatted);
        
        // Decent entrance animation with staggered delay
        card.setAlpha(0);
        card.postDelayed(() -> {
            card.setAlpha(1);
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_up);
            card.startAnimation(anim);
        }, index * 100L);

        container.addView(card, container.getChildCount() - 1);
    }
}
