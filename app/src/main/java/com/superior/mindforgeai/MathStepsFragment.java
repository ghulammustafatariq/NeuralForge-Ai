package com.superior.mindforgeai;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
                        ((TextView) card.findViewById(R.id.tvStepText)).setText(MarkdownRenderer.render(text));
                        
                        final int stepIndex = i;
                        ImageButton btnDel = card.findViewById(R.id.btnDeleteCard);
                        btnDel.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext(), R.style.MindForge_Dialog)
                                .setTitle("Delete Step")
                                .setMessage("Remove step " + num + "?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    if (getActivity() instanceof DeleteContentCallback) {
                                        ((DeleteContentCallback) getActivity()).onDeleteStep(stepIndex);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show());
                        
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
                        String formulaText = formula.optString("formula", "");

                        // Render formula with KaTeX in WebView
                        WebView wv = card.findViewById(R.id.wvFormula);
                        loadFormulaInWebView(wv, formulaText);

                        ((TextView) card.findViewById(R.id.tvFormulaMeaning)).setText(formula.optString("meaning", ""));
                        
                        final int formIndex = i;
                        ImageButton btnDelF = card.findViewById(R.id.btnDeleteCard);
                        btnDelF.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext(), R.style.MindForge_Dialog)
                                .setTitle("Delete Formula")
                                .setMessage("Remove this formula?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    if (getActivity() instanceof DeleteContentCallback) {
                                        ((DeleteContentCallback) getActivity()).onDeleteFormula(formIndex);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show());
                        
                        animateCardEntrance(card, totalItems++);
                        containerView.addView(card);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AddContentCallback cb = getActivity() instanceof AddContentCallback ? (AddContentCallback) getActivity() : null;

        containerView.addView(AddButtonFactory.create(requireContext(), "+ Add Step", () -> {
            if (cb != null) cb.onAddStep();
        }));

        containerView.addView(AddButtonFactory.create(requireContext(), "+ Add Formula", () -> {
            if (cb != null) cb.onAddFormula();
        }));

        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadFormulaInWebView(WebView wv, String latex) {
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(false); // do NOT scale-to-fit; allow wider content
        settings.setUseWideViewPort(true);        // content can exceed screen width
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(false);

        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        wv.setVerticalScrollBarEnabled(false);
        wv.setHorizontalScrollBarEnabled(false);

        // Block navigation — only load our formula HTML
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true; // block all navigation
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Resize WebView height to match rendered formula content
                view.evaluateJavascript(
                    "(function(){return document.getElementById('m').offsetHeight + 24})()",
                    value -> {
                        try {
                            float h = Float.parseFloat(value);
                            float density = getResources().getDisplayMetrics().density;
                            int heightPx = (int) (h * density);
                            if (heightPx > 0) {
                                ViewGroup.LayoutParams lp = view.getLayoutParams();
                                lp.height = heightPx;
                                view.setLayoutParams(lp);
                            }
                        } catch (Exception ignored) {}
                    }
                );
            }
        });

        // Normalize the LaTeX string
        String normalized = normalizeLatex(latex);

        // Escape for embedding in a JavaScript string literal
        String jsEscaped = normalized
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n");

        // Detect dark mode
        boolean isDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        String textColor = isDark ? "#E0E0E0" : "#1D1D1F";
        String bgColor = isDark ? "#1F1F33" : "#F2F2F7";

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no'>" +
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css'>" +
                "<script src='https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js'></script>" +
                "<style>" +
                "*{margin:0;padding:0;box-sizing:border-box}" +
                "html{background:" + bgColor + ";overflow-x:auto;overflow-y:hidden}" +
                "body{background:" + bgColor + ";display:inline-block;min-width:100%;" +
                "padding:14px 16px;text-align:center;min-height:40px;white-space:nowrap}" +
                ".katex-display{margin:0!important}" +
                ".katex{font-size:1.21em;color:" + textColor + "}" +
                ".katex .mord,.katex .mop,.katex .mbin,.katex .mrel{color:" + textColor + "}" +
                "#m{text-align:center;word-break:break-word}" +
                ".fallback{font-family:monospace;font-size:16px;color:" + textColor + ";text-align:center}" +
                "</style></head><body>" +
                "<div id='m'></div>" +
                "<script>" +
                "try{" +
                "katex.render(\"" + jsEscaped + "\",document.getElementById('m')," +
                "{displayMode:true,throwOnError:false,trust:true,strict:false,macros:{" +
                "\"\\\\R\":\"\\\\mathbb{R}\"," +
                "\"\\\\N\":\"\\\\mathbb{N}\"," +
                "\"\\\\Z\":\"\\\\mathbb{Z}\"" +
                "}})" +
                "}catch(e){" +
                "document.getElementById('m').innerHTML='<span class=\"fallback\">" + jsEscaped + "</span>'" +
                "}" +
                "</script></body></html>";

        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
    }

    /**
     * Normalize LaTeX from the AI:
     * - Remove $...$ or $$...$$ delimiters
     * - Fix double-escaped backslashes (\\frac -> \frac)
     */
    private String normalizeLatex(String latex) {
        if (latex == null || latex.isEmpty()) return "";
        String s = latex.trim();

        // Strip $ or $$ delimiters
        if (s.startsWith("$$") && s.endsWith("$$")) {
            s = s.substring(2, s.length() - 2).trim();
        } else if (s.startsWith("$") && s.endsWith("$")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        // Strip \( \) or \[ \] delimiters
        if (s.startsWith("\\(") && s.endsWith("\\)")) {
            s = s.substring(2, s.length() - 2).trim();
        } else if (s.startsWith("\\[") && s.endsWith("\\]")) {
            s = s.substring(2, s.length() - 2).trim();
        }

        // Fix double-escaped backslashes from AI JSON output: \\frac → \frac
        // AI models often return double-backslashes inside JSON strings
        if (s.contains("\\\\")) {
            s = s.replace("\\\\", "\\");
        }

        return s;
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
