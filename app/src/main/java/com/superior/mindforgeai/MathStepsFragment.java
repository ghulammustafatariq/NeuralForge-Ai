package com.superior.mindforgeai;

import android.graphics.Color;
import android.os.Bundle;
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

public class MathStepsFragment extends Fragment {

    public static MathStepsFragment newInstance(String stepsJson, String formulasJson) {
        MathStepsFragment fragment = new MathStepsFragment();
        Bundle args = new Bundle();
        args.putString("steps_json", stepsJson);
        args.putString("formulas_json", formulasJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_steps, container, false);
        LinearLayout containerView = view.findViewById(R.id.stepsContainer);

        if (getArguments() != null) {
            String stepsJson = getArguments().getString("steps_json");
            String formulasJson = getArguments().getString("formulas_json");

            try {
                int totalItems = 0;
                if (stepsJson != null && !stepsJson.isEmpty()) {
                    JSONArray steps = new JSONArray(stepsJson);
                    for (int i = 0; i < steps.length(); i++) {
                        JSONObject step = steps.optJSONObject(i);
                        if (step == null) continue;
                        int num = step.optInt("step", i + 1);
                        String text = step.optString("text", "");
                        View card = inflater.inflate(R.layout.item_step_card, containerView, false);
                        ((TextView) card.findViewById(R.id.tvStepNumber)).setText(String.valueOf(num));
                        ((TextView) card.findViewById(R.id.tvStepText)).setText(text);
                        
                        animateCardEntrance(card, totalItems++);
                        containerView.addView(card);
                    }
                }

                if (formulasJson != null && !formulasJson.isEmpty()) {
                    JSONArray formulas = new JSONArray(formulasJson);
                    for (int i = 0; i < formulas.length(); i++) {
                        JSONObject formula = formulas.optJSONObject(i);
                        if (formula == null) continue;
                        View card = inflater.inflate(R.layout.item_formula_card, containerView, false);
                        ((TextView) card.findViewById(R.id.tvFormula)).setText(formula.optString("formula", ""));
                        ((TextView) card.findViewById(R.id.tvFormulaMeaning)).setText(formula.optString("meaning", ""));
                        
                        animateCardEntrance(card, totalItems++);
                        containerView.addView(card);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AddContentCallback cb = getActivity() instanceof AddContentCallback ? (AddContentCallback) getActivity() : null;

        MaterialButton btnAddStep = new MaterialButton(requireContext());
        btnAddStep.setText("+ Add Step");
        btnAddStep.setTextColor(Color.parseColor("#6800FF"));
        btnAddStep.setStrokeColorResource(android.R.color.transparent);
        btnAddStep.setBackgroundColor(Color.TRANSPARENT);
        btnAddStep.setGravity(Gravity.CENTER);
        btnAddStep.setTextSize(14);
        btnAddStep.setOnClickListener(v -> { if (cb != null) cb.onAddStep(); });
        containerView.addView(btnAddStep);

        MaterialButton btnAddFormula = new MaterialButton(requireContext());
        btnAddFormula.setText("+ Add Formula");
        btnAddFormula.setTextColor(Color.parseColor("#6800FF"));
        btnAddFormula.setStrokeColorResource(android.R.color.transparent);
        btnAddFormula.setBackgroundColor(Color.TRANSPARENT);
        btnAddFormula.setGravity(Gravity.CENTER);
        btnAddFormula.setTextSize(14);
        btnAddFormula.setOnClickListener(v -> { if (cb != null) cb.onAddFormula(); });
        containerView.addView(btnAddFormula);

        return view;
    }

    private void animateCardEntrance(View card, int index) {
        card.setAlpha(0);
        card.postDelayed(() -> {
            card.setAlpha(1);
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_up);
            card.startAnimation(anim);
        }, index * 100L);
    }
}
