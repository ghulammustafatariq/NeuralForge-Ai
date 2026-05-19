package com.superior.mindforgeai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ContentActivity extends AppCompatActivity implements AddContentCallback, DeleteContentCallback {

    private static final String TAG = "ContentActivity";

    private FloatingActionButton fabChat;
    private ImageButton btnBack, btnOverflow;
    private TextView tvContentTitle;
    private String topic;
    private JSONObject aiResult;
    private BottomSheetDialog nodeDetailDialog;
    private int currentTab = -1;
    private String docId;
    private AppDatabase db;
    private FirebaseFirestore firestore;
    private TextView[] tabViews;
    private androidx.appcompat.app.AlertDialog decisionDialogRef;

    private static final String[] TAB_LABELS = {"Explanation", "Map", "Formulas", "Code", "Charts"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_content);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        db = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        handleInsets();
        parseIntent();
        initializeViews();
        setupListeners();

        tvContentTitle.setText(topic);
        
        // Initial entrance animations
        runEntranceAnimations();
        
        switchFragment(0); // Start on Explanation
    }

    private void runEntranceAnimations() {
        Animation fallDown = AnimationUtils.loadAnimation(this, R.anim.fall_down);
        findViewById(R.id.topBar).startAnimation(fallDown);
        findViewById(R.id.navScrollView).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
    }

    private void handleInsets() {
        View root = findViewById(R.id.contentRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }
    }

    private void parseIntent() {
        topic = getIntent().getStringExtra("TOPIC");
        if (topic == null) topic = "Knowledge";
        docId = getIntent().getStringExtra("DOC_ID");

        String aiJson = getIntent().getStringExtra("AI_RESULT");
        if (aiJson != null && !aiJson.isEmpty()) {
            try {
                aiResult = new JSONObject(aiJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnOverflow = findViewById(R.id.btnOverflow);
        tvContentTitle = findViewById(R.id.tvContentTitle);
        fabChat = findViewById(R.id.fabChat);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ContentActivity.this, btnOverflow, Gravity.END);
            popup.getMenu().add(0, 0, 0, "Explanation");
            popup.getMenu().add(0, 1, 0, "Mind Map");
            popup.getMenu().add(0, 2, 0, "Formulas");
            popup.getMenu().add(0, 3, 0, "Code");
            popup.getMenu().add(0, 4, 0, "Charts");

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id >= 0 && id <= 4) {
                    switchFragment(id);
                }
                return true;
            });
            popup.show();
        });

        tabViews = new TextView[]{
                findViewById(R.id.tab0),
                findViewById(R.id.tab1),
                findViewById(R.id.tab2),
                findViewById(R.id.tab3),
                findViewById(R.id.tab4)
        };

        for (int i = 0; i < tabViews.length; i++) {
            final int index = i;
            tabViews[i].setOnClickListener(v -> switchFragment(index));
        }

        fabChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatBotActivity.class);
            intent.putExtra("TOPIC", topic);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        });
    }

    private void switchFragment(int index) {
        if (currentTab == index) return;
        forceLoadFragment(index);
    }

    /** Force reload the given tab even if it's the current one (used after adding content) */
    private void forceRefreshFragment() {
        int idx = currentTab;
        currentTab = -1;
        forceLoadFragment(idx);
    }

    private void forceLoadFragment(int index) {
        Fragment fragment;
        switch (index) {
            case 0:
                fragment = ExplanationFragment.newInstance(getJson("explanation"));
                break;
            case 1:
                fragment = MindMapFragment.newInstance(getJson("mind_map_tree"));
                break;
            case 2:
                fragment = MathStepsFragment.newInstance(getJson("logical_steps"), getJson("formulas"));
                break;
            case 3:
                fragment = CodeFragment.newInstance(getJson("code_snippets"));
                break;
            case 4:
                fragment = ChartFragment.newInstance(getJson("chart_data"));
                break;
            default:
                return;
        }

        highlightTab(index);

        tvContentTitle.setText(topic + " • " + TAB_LABELS[index]);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        
        // Add decent transition animation
        if (currentTab != -1) {
            if (index > currentTab) {
                ft.setCustomAnimations(R.anim.slide_in_right, android.R.anim.fade_out);
            } else {
                ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out);
            }
        }
        
        currentTab = index;
        ft.replace(R.id.fragmentContainer, fragment).commit();
    }

    private void highlightTab(int index) {
        if (tabViews == null) return;
        for (int i = 0; i < tabViews.length; i++) {
            if (i == index) {
                tabViews[i].getBackground().setTint(getColor(R.color.brand_purple_alpha15));
                tabViews[i].setTextColor(getColor(R.color.brand_purple));
                tabViews[i].setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            } else {
                tabViews[i].getBackground().setTint(getColor(R.color.surface_input));
                tabViews[i].setTextColor(getColor(R.color.text_sub));
                tabViews[i].setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL));
            }
        }
    }

    private String getJson(String key) {
        if (aiResult == null) return null;
        try {
            Object obj = aiResult.opt(key);
            if (obj == null) return null;
            if (obj instanceof JSONArray) return ((JSONArray) obj).toString();
            if (obj instanceof JSONObject) return ((JSONObject) obj).toString();
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void saveUpdatedContent() {
        if (aiResult == null || docId == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        String json = aiResult.toString();

        new Thread(() -> {
            HistoryEntity entity = new HistoryEntity(docId, uid, topic, "", "", 0, json, System.currentTimeMillis());
            db.historyDao().insert(entity);
        }).start();

        Map<String, Object> updates = new HashMap<>();
        updates.put("aiResult", json);
        firestore.collection("users").document(uid)
                .collection("history").document(docId)
                .update(updates);
    }

    // ---- AddContentCallback methods ----

    @Override
    public void onAddExplanation() {
        showWriteOrGenerate("Explanation Section", "What should this section be about?",
                "Heading (e.g. Key Benefits)", "Content...", (heading, content) -> {
                    try {
                        JSONArray arr = aiResult.optJSONArray("explanation");
                        if (arr == null) arr = new JSONArray();
                        JSONObject obj = new JSONObject();
                        obj.put("heading", heading);
                        obj.put("content", content);
                        arr.put(obj);
                        aiResult.put("explanation", arr);
                        saveUpdatedContent();
                        forceRefreshFragment();
                        showSuccessSnackbar("Section added successfully");
                    } catch (Exception e) { e.printStackTrace(); }
                });
    }

    @Override
    public void onAddStep() {
        showWriteOrGenerate("Add Step", "What logical step should be added?",
                "Step text", null, (text, unused) -> {
                    try {
                        JSONArray arr = aiResult.optJSONArray("logical_steps");
                        if (arr == null) arr = new JSONArray();
                        JSONObject obj = new JSONObject();
                        obj.put("step", arr.length() + 1);
                        obj.put("text", text);
                        arr.put(obj);
                        aiResult.put("logical_steps", arr);
                        saveUpdatedContent();
                        forceRefreshFragment();
                        showSuccessSnackbar("Step added successfully");
                    } catch (Exception e) { e.printStackTrace(); }
                });
    }

    @Override
    public void onAddFormula() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_decision, null);

        ((TextView) dialogView.findViewById(R.id.dialogTitle)).setText("Add Formula");

        MaterialButton btnWrite = dialogView.findViewById(R.id.btnPrimary);
        btnWrite.setText("Write My Own");
        btnWrite.setOnClickListener(v -> {
            dismissDialog(dialogView);
            showWriteDialog("Add Formula", "Formula in LaTeX (e.g. E = mc^{2})",
                    "Meaning / explanation", (formula, meaning) -> insertFormula(formula, meaning));
        });

        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnSecondary);
        btnGenerate.setOnClickListener(v -> {
            dismissDialog(dialogView);
            View genView = LayoutInflater.from(this).inflate(R.layout.dialog_input_single, null);
            TextInputEditText promptInput = genView.findViewById(R.id.etField1);
            ((com.google.android.material.textfield.TextInputLayout) genView.findViewById(R.id.tilField1))
                    .setHint("Describe the formula (e.g. logistic regression sigmoid)");
            new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                    .setTitle("Generate Formula")
                    .setView(genView)
                    .setPositiveButton("Generate", (dd, ww) -> {
                        String p = promptInput.getText().toString().trim();
                        if (!p.isEmpty()) showLoadingAndGenerate("Formula", "formula", p, result -> {
                            try {
                                JSONObject obj = new JSONObject(result);
                                insertFormula(obj.optString("formula", ""), obj.optString("meaning", ""));
                            } catch (Exception e) {
                                insertFormula(result.trim(), "");
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dismissDialog(dialogView));

        decisionDialogRef = new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setView(dialogView)
                .show();
    }

    private void insertFormula(String formula, String meaning) {
        try {
            JSONArray arr = aiResult.optJSONArray("formulas");
            if (arr == null) arr = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("formula", formula);
            obj.put("meaning", meaning);
            arr.put(obj);
            aiResult.put("formulas", arr);
            saveUpdatedContent();
            forceRefreshFragment();
            showSuccessSnackbar("Formula added successfully");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onAddCode() {
        showWriteOrGenerateCode((heading, code, output) -> {
            try {
                JSONArray arr = aiResult.optJSONArray("code_snippets");
                if (arr == null) arr = new JSONArray();
                JSONObject obj = new JSONObject();
                obj.put("heading", heading);
                obj.put("code", code);
                obj.put("output", output);
                arr.put(obj);
                aiResult.put("code_snippets", arr);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Code added successfully");
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public void onAddChart() {
        showWriteOrGenerateChart((type, labels, values) -> {
            try {
                JSONObject chartData = aiResult.optJSONObject("chart_data");
                if (chartData == null) chartData = new JSONObject();
                JSONObject chart = new JSONObject();
                chart.put("labels", new JSONArray(TextUtils.split(labels, ",")));
                JSONArray vals = new JSONArray();
                for (String v : TextUtils.split(values, ",")) {
                    vals.put(Double.parseDouble(v.trim()));
                }
                chart.put("values", vals);
                chartData.put(type, chart);
                aiResult.put("chart_data", chartData);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Chart added successfully");
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showSuccessSnackbar(String msg) {
        View root = findViewById(R.id.contentRoot);
        if (root != null) {
            Snackbar snack = Snackbar.make(root, "✓  " + msg, Snackbar.LENGTH_SHORT);
            snack.setBackgroundTint(getColor(R.color.brand_purple));
            snack.setTextColor(getColor(R.color.text_on_brand));
            snack.show();
        }
    }

    // ---- DeleteContentCallback methods ----

    @Override
    public void onDeleteExplanation(int index) {
        try {
            JSONArray arr = aiResult.optJSONArray("explanation");
            if (arr != null && index >= 0 && index < arr.length()) {
                arr = removeFromArray(arr, index);
                aiResult.put("explanation", arr);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Section deleted");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDeleteStep(int index) {
        try {
            JSONArray arr = aiResult.optJSONArray("logical_steps");
            if (arr != null && index >= 0 && index < arr.length()) {
                arr = removeFromArray(arr, index);
                // Renumber remaining steps
                for (int i = 0; i < arr.length(); i++) {
                    arr.getJSONObject(i).put("step", i + 1);
                }
                aiResult.put("logical_steps", arr);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Step deleted");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDeleteFormula(int index) {
        try {
            JSONArray arr = aiResult.optJSONArray("formulas");
            if (arr != null && index >= 0 && index < arr.length()) {
                arr = removeFromArray(arr, index);
                aiResult.put("formulas", arr);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Formula deleted");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDeleteCode(int index) {
        try {
            JSONArray arr = aiResult.optJSONArray("code_snippets");
            if (arr != null && index >= 0 && index < arr.length()) {
                arr = removeFromArray(arr, index);
                aiResult.put("code_snippets", arr);
                saveUpdatedContent();
                forceRefreshFragment();
                showSuccessSnackbar("Code deleted");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JSONArray removeFromArray(JSONArray arr, int index) throws Exception {
        JSONArray newArr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            if (i != index) newArr.put(arr.get(i));
        }
        return newArr;
    }

    public void onMindMapChanged() {
        saveUpdatedContent();
    }

    public void updateMindMapTree(JSONObject treeJson) {
        if (aiResult == null) return;
        try {
            aiResult.put("mind_map_tree", treeJson);
            saveUpdatedContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addCurrentNoteToPlaylist() {
        if (docId == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        new Thread(() -> {
            List<PlaylistEntity> playlists = db.playlistDao().getAllByUid(uid);
            if (playlists.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Create a collection first", Toast.LENGTH_SHORT).show());
                return;
            }
            String[] names = new String[playlists.size()];
            for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).getEmoji() + "  " + playlists.get(i).getName();
            runOnUiThread(() -> new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                    .setTitle("Add to Collection")
                    .setItems(names, (d, which) -> {
                        String pid = playlists.get(which).getId();
                        new Thread(() -> {
                            db.historyDao().setPlaylist(docId, pid);
                            firestore.collection("users").document(uid)
                                    .collection("history").document(docId)
                                    .update("playlistId", pid);
                            runOnUiThread(() -> Toast.makeText(ContentActivity.this,
                                    "Added to " + playlists.get(which).getName(), Toast.LENGTH_SHORT).show());
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }).start();
    }

    // ---- Dialog helpers ----

    private void showWriteOrGenerate(String title, String generatePrompt, String field1Hint,
                                     String field2Hint, AppendAction action) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_decision, null);

        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        tvTitle.setText(title);

        MaterialButton btnWrite = dialogView.findViewById(R.id.btnPrimary);
        btnWrite.setText("Write My Own");
        btnWrite.setOnClickListener(v -> {
            dismissDialog(dialogView);
            showWriteDialog(title, field1Hint, field2Hint, action);
        });

        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnSecondary);
        btnGenerate.setOnClickListener(v -> {
            dismissDialog(dialogView);
            showGenerateDialog(title, generatePrompt, action);
        });

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dismissDialog(dialogView));

        decisionDialogRef = new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setView(dialogView)
                .show();
    }

    private void dismissDialog(View dialogView) {
        if (decisionDialogRef != null && decisionDialogRef.isShowing()) {
            decisionDialogRef.dismiss();
        }
    }

    private void showWriteDialog(String title, String field1Hint, String field2Hint, AppendAction action) {
        View dialogView = LayoutInflater.from(this).inflate(
                field2Hint != null ? R.layout.dialog_input_double : R.layout.dialog_input_single, null);

        TextInputEditText et1 = dialogView.findViewById(R.id.etField1);
        ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField1)).setHint(field1Hint);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton("Cancel", null);

        if (field2Hint != null) {
            TextInputEditText et2 = dialogView.findViewById(R.id.etField2);
            ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField2)).setHint(field2Hint);
            builder.setPositiveButton("Add", (d, w) -> {
                String v1 = et1.getText().toString().trim();
                String v2 = et2.getText().toString().trim();
                if (!v1.isEmpty()) action.apply(v1, v2);
            });
        } else {
            builder.setPositiveButton("Add", (d, w) -> {
                String v1 = et1.getText().toString().trim();
                if (!v1.isEmpty()) action.apply(v1, null);
            });
        }

        builder.show();
    }

    private void showWriteOrGenerateCode(AppendCodeAction action) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_decision, null);

        ((TextView) dialogView.findViewById(R.id.dialogTitle)).setText("Add Code Snippet");

        MaterialButton btnWrite = dialogView.findViewById(R.id.btnPrimary);
        btnWrite.setText("Write My Own");
        btnWrite.setOnClickListener(v -> {
            dismissDialog(dialogView);
            View codeView = LayoutInflater.from(this).inflate(R.layout.dialog_input_code, null);
            TextInputEditText etHeading = codeView.findViewById(R.id.etField1);
            TextInputEditText etCode = codeView.findViewById(R.id.etField2);
            TextInputEditText etOutput = codeView.findViewById(R.id.etField3);
            ((com.google.android.material.textfield.TextInputLayout) codeView.findViewById(R.id.tilField1)).setHint("Heading (e.g. Java: Quick Sort)");
            ((com.google.android.material.textfield.TextInputLayout) codeView.findViewById(R.id.tilField2)).setHint("Code");
            ((com.google.android.material.textfield.TextInputLayout) codeView.findViewById(R.id.tilField3)).setHint("Expected Output");

            new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                    .setTitle("Write Code")
                    .setView(codeView)
                    .setPositiveButton("Add", (dd, ww) -> {
                        String h = etHeading.getText().toString().trim();
                        String c = etCode.getText().toString().trim();
                        String o = etOutput.getText().toString().trim();
                        if (!h.isEmpty() && !c.isEmpty()) action.apply(h, c, o);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnSecondary);
        btnGenerate.setOnClickListener(v -> {
            dismissDialog(dialogView);
            View genView = LayoutInflater.from(this).inflate(R.layout.dialog_input_single, null);
            TextInputEditText promptInput = genView.findViewById(R.id.etField1);
            ((com.google.android.material.textfield.TextInputLayout) genView.findViewById(R.id.tilField1))
                    .setHint("Describe the code you want (e.g. Python sorting algorithm)");

            new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                    .setTitle("Generate Code")
                    .setView(genView)
                    .setPositiveButton("Generate", (dd, ww) -> {
                        String p = promptInput.getText().toString().trim();
                        if (!p.isEmpty()) showLoadingAndGenerate("Code", "code", p, codeResult -> {
                            try {
                                JSONObject obj = new JSONObject(codeResult);
                                action.apply(obj.optString("heading", "Code"),
                                        obj.optString("code", ""), obj.optString("output", ""));
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dismissDialog(dialogView));

        decisionDialogRef = new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setView(dialogView)
                .show();
    }

    private void showWriteOrGenerateChart(AppendChartAction action) {
        new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("Add Chart Data")
                .setMessage("What type of chart?")
                .setPositiveButton("Enter Data", (d, w) -> {
                    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_double, null);
                    TextInputEditText etType = dialogView.findViewById(R.id.etField1);
                    ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField1)).setHint("Chart type: bar, line, or scatter");
                    TextInputEditText etLabels = dialogView.findViewById(R.id.etField2);
                    ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField2)).setHint("Labels (comma-separated, e.g. A,B,C,D,E)");

                    new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                            .setTitle("Chart Data")
                            .setView(dialogView)
                            .setPositiveButton("Next: Values", (dd, ww) -> {
                                String t = etType.getText().toString().trim().toLowerCase();
                                String l = etLabels.getText().toString().trim();
                                if (!t.isEmpty() && !l.isEmpty()) {
                                    showChartValuesDialog(t, l, action);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChartValuesDialog(String type, String labels, AppendChartAction action) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_single, null);
        TextInputEditText etValues = dialogView.findViewById(R.id.etField1);
        ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField1))
                .setHint("Values (comma-separated, e.g. 10,25,15,30,20)");

        new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("Enter Values")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String v = etValues.getText().toString().trim();
                    if (!v.isEmpty()) action.apply(type, labels, v);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGenerateDialog(String title, String promptHint, AppendAction action) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_single, null);
        TextInputEditText promptInput = dialogView.findViewById(R.id.etField1);
        ((com.google.android.material.textfield.TextInputLayout) dialogView.findViewById(R.id.tilField1))
                .setHint(promptHint);

        new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("Generate with AI")
                .setView(dialogView)
                .setPositiveButton("Generate", (d, w) -> {
                    String prompt = promptInput.getText().toString().trim();
                    if (!prompt.isEmpty()) {
                        showLoadingAndGenerate(title, "general", prompt, result -> {
                            // Try parsing as JSON {heading, content} first
                            try {
                                JSONObject obj = new JSONObject(result);
                                String h = obj.optString("heading", "");
                                String c = obj.optString("content", "");
                                if (!h.isEmpty() && !c.isEmpty()) {
                                    action.apply(h, c);
                                    return;
                                }
                            } catch (Exception ignored) {}
                            // Plain text → use prompt as heading, AI text as content
                            String heading = prompt.length() > 50
                                    ? prompt.substring(0, 47) + "..."
                                    : prompt;
                            // Capitalize first letter
                            heading = heading.substring(0, 1).toUpperCase() + heading.substring(1);
                            action.apply(heading, result);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a non-cancellable progress dialog (matching app theme, no extra colors)
     * while the AI request is in flight, then dismisses it on completion.
     */
    private void showLoadingAndGenerate(String label, String type, String prompt, GenerateCallback cb) {
        // Check network first so user doesn't wait 30s for a timeout
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        if (ni == null || !ni.isConnected()) {
            View root = findViewById(R.id.contentRoot);
            if (root != null) {
                Snackbar.make(root, "⚠  No internet connection. Please check your network.",
                                Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.text_main))
                        .setTextColor(getColor(R.color.surface_card))
                        .show();
            }
            return;
        }

        androidx.appcompat.app.AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("Generating…")
                .setMessage("AI is creating " + label.toLowerCase() + ". Please wait.")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        generateSection(type, prompt, result -> {
            // ALWAYS dismiss the loading dialog — success or failure
            if (!isFinishing() && loadingDialog.isShowing()) loadingDialog.dismiss();

            if (result != null) {
                cb.onResult(result);
            } else {
                // Show error feedback instead of freezing
                View root = findViewById(R.id.contentRoot);
                if (root != null) {
                    Snackbar.make(root, "⚠  Generation failed. Try again.", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getColor(R.color.text_main))
                            .setTextColor(getColor(R.color.surface_card))
                            .show();
                }
            }
        });
    }

    private void generateSection(String type, String userPrompt, GenerateCallback callback) {
        DeepSeekService service = new DeepSeekService();
        service.generateSection(this, topic, type, userPrompt, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ContentActivity", "Generate failed", e);
                // ALWAYS call the callback so the loading dialog can dismiss
                runOnUiThread(() -> callback.onResult(null));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = null;
                try {
                    body = response.body().string();
                    Log.d("ContentActivity", "API response code: " + response.code());
                    Log.d("ContentActivity", "API response body (first 500): " + body.substring(0, Math.min(body.length(), 500)));

                    if (!response.isSuccessful()) {
                        Log.e("ContentActivity", "API error! HTTP " + response.code() + ": " + body);
                        runOnUiThread(() -> callback.onResult(null));
                        return;
                    }

                    JSONObject json = new JSONObject(body);
                    String content = json.getJSONArray("choices").getJSONObject(0)
                            .getJSONObject("message").getString("content").trim();
                    if (content.startsWith("```")) content = content.substring(content.indexOf("\n") + 1);
                    if (content.endsWith("```")) content = content.substring(0, content.lastIndexOf("\n"));
                    String finalContent = content.trim();
                    runOnUiThread(() -> callback.onResult(finalContent));
                } catch (Exception e) {
                    Log.e("ContentActivity", "Parse error, body was: " + (body != null ? body.substring(0, Math.min(body.length(), 500)) : "null"), e);
                    runOnUiThread(() -> callback.onResult(null));
                } finally {
                    response.close();
                }
            }
        });
    }

    public void showNodeDetail(String nodeName, String detail) {
        if (nodeDetailDialog != null && nodeDetailDialog.isShowing()) {
            nodeDetailDialog.dismiss();
        }
        nodeDetailDialog = new BottomSheetDialog(this, R.style.MindForge_BottomSheetDialog);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_node_detail, null);
        ((TextView) sheetView.findViewById(R.id.tvDetailTitle)).setText(nodeName);
        ((TextView) sheetView.findViewById(R.id.tvDetailContent)).setText(
                detail.isEmpty() ? getString(R.string.node_detail_template).replace("%1$s", nodeName) : detail);
        sheetView.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> nodeDetailDialog.dismiss());
        nodeDetailDialog.setContentView(sheetView);
        nodeDetailDialog.show();
    }

    interface AppendAction { void apply(String v1, String v2); }
    interface AppendCodeAction { void apply(String heading, String code, String output); }
    interface AppendChartAction { void apply(String type, String labels, String values); }
    interface GenerateCallback { void onResult(String result); }
}
