package com.superior.mindforgeai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
                        addSectionCard(inflater, containerView,
                                section.optString("heading", ""),
                                section.optString("content", ""), i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        containerView.addView(AddButtonFactory.create(requireContext(), "+ Add Section", () -> {
            if (getActivity() instanceof AddContentCallback) {
                ((AddContentCallback) getActivity()).onAddExplanation();
            }
        }));

        return view;
    }

    private void addSectionCard(LayoutInflater inflater, LinearLayout container,
                                String heading, String content, int index) {
        boolean hasHeading = heading != null && !heading.trim().isEmpty();
        boolean hasContent = content != null && !content.trim().isEmpty();

        // Skip entirely empty cards
        if (!hasHeading && !hasContent) return;

        View card = inflater.inflate(R.layout.item_section_card, container, false);
        TextView tvHeading = card.findViewById(R.id.tvSectionHeading);
        TextView tvContent = card.findViewById(R.id.tvSectionContent);
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteCard);

        if (hasHeading) {
            tvHeading.setVisibility(View.VISIBLE);
            tvHeading.setText(heading.trim());
        } else {
            tvHeading.setVisibility(View.GONE);
        }

        if (hasContent) {
            tvContent.setVisibility(View.VISIBLE);
            tvContent.setText(MarkdownRenderer.render(content.trim()));
        } else {
            tvContent.setVisibility(View.GONE);
        }

        final int deleteIndex = index;
        btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext(), R.style.MindForge_Dialog)
                .setTitle("Delete Section")
                .setMessage("Remove \"" + (hasHeading ? heading.trim() : "this section") + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (getActivity() instanceof DeleteContentCallback) {
                        ((DeleteContentCallback) getActivity()).onDeleteExplanation(deleteIndex);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show());

        // Staggered entrance animation with null-safety
        card.setAlpha(0);
        card.postDelayed(() -> {
            if (getContext() == null) return;
            card.setAlpha(1);
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_up);
            card.startAnimation(anim);
        }, index * 80L);

        container.addView(card, container.getChildCount() - 1);
    }
}
