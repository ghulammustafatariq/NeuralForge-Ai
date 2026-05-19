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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
                        for (int i = 0; i < snippets.length(); i++) {
                            JSONObject snippet = snippets.optJSONObject(i);
                            if (snippet == null) continue;
                            addCodeCard(inflater, containerView,
                                    snippet.optString("heading", "Code Example"),
                                    snippet.optString("code", ""),
                                    snippet.optString("output", ""), i);
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

        containerView.addView(AddButtonFactory.create(requireContext(), "+ Add Code", () -> {
            if (cb != null) cb.onAddCode();
        }));

        return view;
    }

    private void addCodeCard(LayoutInflater inflater, LinearLayout container, String heading, String code, String output, int index) {
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
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteCard);
        final int codeIndex = index;
        btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext(), R.style.MindForge_Dialog)
                .setTitle("Delete Code")
                .setMessage("Remove \"" + heading + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (getActivity() instanceof DeleteContentCallback) {
                        ((DeleteContentCallback) getActivity()).onDeleteCode(codeIndex);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show());
        container.addView(card, container.getChildCount() - 1);
    }

    private void showNoContent(LayoutInflater inflater, LinearLayout container, String message) {
        View card = inflater.inflate(R.layout.item_section_card, container, false);
        ((TextView) card.findViewById(R.id.tvSectionHeading)).setText("Code");
        ((TextView) card.findViewById(R.id.tvSectionContent)).setText(message);
        container.addView(card);
    }
}
