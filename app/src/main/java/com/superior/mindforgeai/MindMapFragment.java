package com.superior.mindforgeai;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

public class MindMapFragment extends Fragment {

    private MindMapCanvasView mindMapCanvas;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private FloatingActionButton fabSearch;
    private View searchCard;
    private boolean searchVisible;

    public static MindMapFragment newInstance(String mindMapJson) {
        MindMapFragment fragment = new MindMapFragment();
        Bundle args = new Bundle();
        args.putString("mind_map_json", mindMapJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mind_map, container, false);

        mindMapCanvas = view.findViewById(R.id.mindMapCanvas);
        etSearch = view.findViewById(R.id.etSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        fabSearch = view.findViewById(R.id.fabSearch);
        searchCard = view.findViewById(R.id.searchCard);

        mindMapCanvas.setOnNodeInteractionListener(new MindMapCanvasView.OnNodeInteractionListener() {
            @Override
            public void onNodeLongPress(String name, String detail) {
                if (getActivity() instanceof ContentActivity) {
                    ((ContentActivity) getActivity()).showNodeDetail(name, detail);
                }
            }

            @Override
            public void onNodeChanged() {
            }
        });

        setupSearch();

        if (getArguments() != null) {
            String jsonData = getArguments().getString("mind_map_json");
            if (jsonData != null && !jsonData.isEmpty()) {
                try {
                    mindMapCanvas.renderFromJson(new JSONObject(jsonData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return view;
    }

    private void setupSearch() {
        fabSearch.setOnClickListener(v -> {
            searchVisible = !searchVisible;
            searchCard.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
            if (searchVisible) {
                etSearch.requestFocus();
            } else {
                mindMapCanvas.clearSearch();
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                mindMapCanvas.searchNode(query);
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mindMapCanvas.nextSearchResult();
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            mindMapCanvas.clearSearch();
        });
    }

    public void zoomToFit() {
        if (mindMapCanvas != null) {
            mindMapCanvas.zoomToFit();
        }
    }
}
