package com.superior.mindforgeai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChartFragment extends Fragment {

    private ChipGroup chipGroupChartType;
    private Chip chipBar, chipLine, chipScatter;
    private FrameLayout chartContainer;
    private JSONObject chartData;
    private Chart currentChart;

    public static ChartFragment newInstance(String chartJson) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString("chart_json", chartJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        chipGroupChartType = view.findViewById(R.id.chipGroupChartType);
        chipBar = view.findViewById(R.id.chipBar);
        chipLine = view.findViewById(R.id.chipLine);
        chipScatter = view.findViewById(R.id.chipScatter);
        chartContainer = view.findViewById(R.id.chartView);

        chipBar.setChecked(true);

        if (getArguments() != null) {
            String chartJson = getArguments().getString("chart_json");
            if (chartJson != null && !chartJson.isEmpty()) {
                try {
                    chartData = new JSONObject(chartJson);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        chipGroupChartType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == chipBar.getId()) renderBarChart();
            else if (checkedId == chipLine.getId()) renderLineChart();
            else if (checkedId == chipScatter.getId()) renderScatterChart();
        });

        renderBarChart();

        AddContentCallback cb = getActivity() instanceof AddContentCallback ? (AddContentCallback) getActivity() : null;

        MaterialButton btnAdd = new MaterialButton(requireContext());
        btnAdd.setText("+ Add Chart");
        btnAdd.setTextColor(Color.parseColor("#6800FF"));
        btnAdd.setStrokeColorResource(android.R.color.transparent);
        btnAdd.setBackgroundColor(Color.TRANSPARENT);
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setTextSize(14);
        btnAdd.setOnClickListener(v -> { if (cb != null) cb.onAddChart(); });

        ((android.view.ViewGroup) view).addView(btnAdd);

        return view;
    }

    private void removeCurrentChart() { chartContainer.removeAllViews(); currentChart = null; }

    private void renderBarChart() {
        if (chartData == null) return;
        removeCurrentChart();
        JSONObject bd = chartData.optJSONObject("bar");
        if (bd == null) return;
        BarChart chart = new BarChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        styleChart(chart);
        List<BarEntry> entries = new ArrayList<>();
        JSONArray values = bd.optJSONArray("values");
        if (values != null) for (int i = 0; i < values.length(); i++) entries.add(new BarEntry(i, (float) values.optDouble(i, 0)));
        BarDataSet ds = new BarDataSet(entries, "Comparison");
        ds.setColor(Color.parseColor("#6800FF"));
        ds.setValueTextColor(Color.parseColor("#6800FF"));
        chart.setData(new BarData(ds));
        chart.animateY(1000);
        chartContainer.addView(chart);
        currentChart = chart;
    }

    private void renderLineChart() {
        if (chartData == null) return;
        removeCurrentChart();
        JSONObject ld = chartData.optJSONObject("line");
        if (ld == null) return;
        LineChart chart = new LineChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        styleChart(chart);
        List<Entry> entries = new ArrayList<>();
        JSONArray values = ld.optJSONArray("values");
        if (values != null) for (int i = 0; i < values.length(); i++) entries.add(new Entry(i, (float) values.optDouble(i, 0)));
        LineDataSet ds = new LineDataSet(entries, "Trend");
        ds.setColor(Color.parseColor("#6800FF"));
        ds.setLineWidth(3f);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor("#6800FF"));
        ds.setFillAlpha(30);
        ds.setCircleColor(Color.parseColor("#6800FF"));
        ds.setValueTextColor(Color.parseColor("#6800FF"));
        chart.setData(new LineData(ds));
        chart.animateY(1000);
        chartContainer.addView(chart);
        currentChart = chart;
    }

    private void renderScatterChart() {
        if (chartData == null) return;
        removeCurrentChart();
        JSONObject sd = chartData.optJSONObject("scatter");
        if (sd == null) return;
        ScatterChart chart = new ScatterChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        styleChart(chart);
        List<Entry> entries = new ArrayList<>();
        JSONArray values = sd.optJSONArray("values");
        if (values != null) for (int i = 0; i < values.length(); i++) entries.add(new Entry(i, (float) values.optDouble(i, 0)));
        ScatterDataSet ds = new ScatterDataSet(entries, "Distribution");
        ds.setColor(Color.parseColor("#6800FF"));
        ds.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        ds.setScatterShapeSize(12f);
        ds.setValueTextColor(Color.parseColor("#6800FF"));
        chart.setData(new ScatterData(ds));
        chart.animateY(1000);
        chartContainer.addView(chart);
        currentChart = chart;
    }

    private void styleChart(Chart chart) {
        chart.getDescription().setEnabled(false);
        chart.setBackgroundColor(Color.parseColor("#FFF9EB"));
        chart.getLegend().setTextColor(Color.parseColor("#1D1D1F"));
        chart.invalidate();
    }
}
