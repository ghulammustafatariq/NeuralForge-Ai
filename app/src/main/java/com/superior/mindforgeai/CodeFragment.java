package com.superior.mindforgeai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

public class CodeFragment extends Fragment {

    public static CodeFragment newInstance(String codeJson) {
        CodeFragment fragment = new CodeFragment();
        Bundle args = new Bundle();
        args.putString("code_json", codeJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_code, container, false);
        LinearLayout containerView = view.findViewById(R.id.codeContainer);

        if (getArguments() != null) {
            String codeJson = getArguments().getString("code_json");
            if (codeJson != null && !codeJson.isEmpty()) {
                try {
                    JSONArray snippets = new JSONArray(codeJson);
                    if (snippets.length() == 0) {
                        showNoContent(inflater, containerView, "No code snippets for this topic");
                    } else {
                        int maxSnippets = Math.min(snippets.length(), 3);
                        for (int i = 0; i < maxSnippets; i++) {
                            JSONObject snippet = snippets.optJSONObject(i);
                            if (snippet == null) continue;
                            addCodeCard(inflater, containerView,
                                    snippet.optString("heading", "Code Example"),
                                    snippet.optString("code", ""),
                                    snippet.optString("output", ""));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showNoContent(inflater, containerView, "Error loading code snippets");
                }
            } else {
                showNoContent(inflater, containerView, "No code snippets for this topic");
            }
        }

        AddContentCallback cb = getActivity() instanceof AddContentCallback ? (AddContentCallback) getActivity() : null;

        MaterialButton btnAdd = new MaterialButton(requireContext());
        btnAdd.setText("+ Add Code");
        btnAdd.setTextColor(Color.parseColor("#6800FF"));
        btnAdd.setStrokeColorResource(android.R.color.transparent);
        btnAdd.setBackgroundColor(Color.TRANSPARENT);
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setTextSize(14);
        btnAdd.setOnClickListener(v -> { if (cb != null) cb.onAddCode(); });
        containerView.addView(btnAdd);

        return view;
    }

    private void addCodeCard(LayoutInflater inflater, LinearLayout container, String heading, String code, String output) {
        View card = inflater.inflate(R.layout.item_code_flashcard, container, false);
        ((TextView) card.findViewById(R.id.tvCodeHeading)).setText(heading);
        ((TextView) card.findViewById(R.id.tvCodeContent)).setText(code);
        ((TextView) card.findViewById(R.id.tvCodeOutput)).setText(output);
        ImageButton btnCopy = card.findViewById(R.id.btnCopyCode);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("Code", code));
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
        });
        container.addView(card, container.getChildCount() - 1);
    }

    private void showNoContent(LayoutInflater inflater, LinearLayout container, String message) {
        View card = inflater.inflate(R.layout.item_section_card, container, false);
        ((TextView) card.findViewById(R.id.tvSectionHeading)).setText("Code");
        ((TextView) card.findViewById(R.id.tvSectionContent)).setText(message);
        container.addView(card);
    }
}
