package com.superior.mindforgeai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

public class MindMapFragment extends Fragment {

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

        TreeMindMapView treeMindMapView = new TreeMindMapView(requireContext());
        ViewGroup treeContainer = view.findViewById(R.id.treeContainer);
        treeContainer.addView(treeMindMapView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (getArguments() != null) {
            String jsonData = getArguments().getString("mind_map_json");
            if (jsonData != null && !jsonData.isEmpty()) {
                try {
                    treeMindMapView.renderFromJson(new JSONObject(jsonData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return view;
    }
}
