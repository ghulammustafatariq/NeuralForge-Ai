package com.superior.mindforgeai;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ContentActivity extends AppCompatActivity implements AddContentCallback {

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

    private static final String[] TAB_LABELS = {"Mind Map", "Explanation", "Steps", "Code", "Charts"};

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
        
        switchFragment(0);
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
            popup.getMenu().add(0, 0, 0, "Mind Map");
            popup.getMenu().add(0, 1, 0, "Explanation");
            popup.getMenu().add(0, 2, 0, "Steps + Formulas");
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

        fabChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatBotActivity.class);
            intent.putExtra("TOPIC", topic);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        });
    }

    private void switchFragment(int index) {
        if (currentTab == index) return;
        
        Fragment fragment;
        switch (index) {
            case 0:
                fragment = MindMapFragment.newInstance(getJson("mind_map_tree"));
                break;
            case 1:
                fragment = ExplanationFragment.newInstance(getJson("explanation"));
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
                        switchFragment(1);
                        Toast.makeText(this, "Section added", Toast.LENGTH_SHORT).show();
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
                        switchFragment(2);
                        Toast.makeText(this, "Step added", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { e.printStackTrace(); }
                });
    }

    @Override
    public void onAddFormula() {
        showWriteOrGenerate("Add Formula", "What formula and meaning should be added?",
                "Formula (e.g. E = mc²)", "Meaning / explanation", (formula, meaning) -> {
                    try {
                        JSONArray arr = aiResult.optJSONArray("formulas");
                        if (arr == null) arr = new JSONArray();
                        JSONObject obj = new JSONObject();
                        obj.put("formula", formula);
                        obj.put("meaning", meaning);
                        arr.put(obj);
                        aiResult.put("formulas", arr);
                        saveUpdatedContent();
                        switchFragment(2);
                        Toast.makeText(this, "Formula added", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { e.printStackTrace(); }
                });
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
                switchFragment(3);
                Toast.makeText(this, "Code added", Toast.LENGTH_SHORT).show();
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
                switchFragment(4);
                Toast.makeText(this, "Chart added", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // ---- Dialog helpers ----

    private void showWriteOrGenerate(String title, String generatePrompt, String field1Hint,
                                     String field2Hint, AppendAction action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("How would you like to add content?")
                .setPositiveButton("✏️ Write My Own", (d, w) -> showWriteDialog(title, field1Hint, field2Hint, action))
                .setNegativeButton("🤖 Generate from AI", (d, w) -> showGenerateDialog(title, generatePrompt, action))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showWriteDialog(String title, String field1Hint, String field2Hint, AppendAction action) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText et1 = new EditText(this);
        et1.setHint(field1Hint);
        layout.addView(et1);

        if (field2Hint != null) {
            EditText et2 = new EditText(this);
            et2.setHint(field2Hint);
            et2.setMinHeight(120);
            et2.setGravity(Gravity.TOP);
            layout.addView(et2);

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton("Add", (d, w) -> {
                        String v1 = et1.getText().toString().trim();
                        String v2 = et2.getText().toString().trim();
                        if (!v1.isEmpty()) action.apply(v1, v2);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(layout)
                    .setPositiveButton("Add", (d, w) -> {
                        String v1 = et1.getText().toString().trim();
                        if (!v1.isEmpty()) action.apply(v1, null);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showWriteOrGenerateCode(AppendCodeAction action) {
        new AlertDialog.Builder(this)
                .setTitle("Add Code Snippet")
                .setMessage("How would you like to add code?")
                .setPositiveButton("✏️ Write My Own", (d, w) -> {
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(40, 20, 40, 10);

                    EditText etHeading = new EditText(this);
                    etHeading.setHint("Heading (e.g. Java: Quick Sort)");
                    layout.addView(etHeading);

                    EditText etCode = new EditText(this);
                    etCode.setHint("Code");
                    etCode.setMinHeight(120);
                    etCode.setGravity(Gravity.TOP);
                    etCode.setTypeface(android.graphics.Typeface.MONOSPACE);
                    layout.addView(etCode);

                    EditText etOutput = new EditText(this);
                    etOutput.setHint("Expected Output");
                    layout.addView(etOutput);

                    new AlertDialog.Builder(this)
                            .setTitle("Write Code")
                            .setView(layout)
                            .setPositiveButton("Add", (dd, ww) -> {
                                String h = etHeading.getText().toString().trim();
                                String c = etCode.getText().toString().trim();
                                String o = etOutput.getText().toString().trim();
                                if (!h.isEmpty() && !c.isEmpty()) action.apply(h, c, o);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("🤖 Generate from AI", (d, w) -> {
                    EditText promptInput = new EditText(this);
                    promptInput.setHint("Describe the code you want (e.g. Python sorting algorithm)");
                    promptInput.setPadding(30, 20, 30, 20);
                    promptInput.setMinHeight(100);

                    new AlertDialog.Builder(this)
                            .setTitle("Generate Code")
                            .setView(promptInput)
                            .setPositiveButton("Generate", (dd, ww) -> {
                                String p = promptInput.getText().toString().trim();
                                if (!p.isEmpty()) generateSection("code", p, codeResult -> {
                                    try {
                                        JSONObject obj = new JSONObject(codeResult);
                                        action.apply(obj.optString("heading", "Code"),
                                                obj.optString("code", ""), obj.optString("output", ""));
                                    } catch (Exception e) { e.printStackTrace(); }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showWriteOrGenerateChart(AppendChartAction action) {
        new AlertDialog.Builder(this)
                .setTitle("Add Chart Data")
                .setMessage("What type of chart?")
                .setPositiveButton("✏️ Enter Data", (d, w) -> {
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(40, 20, 40, 10);

                    EditText etType = new EditText(this);
                    etType.setHint("Chart type: bar, line, or scatter");
                    layout.addView(etType);

                    EditText etLabels = new EditText(this);
                    etLabels.setHint("Labels (comma-separated, e.g. A,B,C,D,E)");
                    layout.addView(etLabels);

                    EditText etValues = new EditText(this);
                    etValues.setHint("Values (comma-separated, e.g. 10,25,15,30,20)");
                    layout.addView(etValues);

                    new AlertDialog.Builder(this)
                            .setTitle("Chart Data")
                            .setView(layout)
                            .setPositiveButton("Add", (dd, ww) -> {
                                String t = etType.getText().toString().trim().toLowerCase();
                                String l = etLabels.getText().toString().trim();
                                String v = etValues.getText().toString().trim();
                                if (!t.isEmpty() && !l.isEmpty() && !v.isEmpty()) action.apply(t, l, v);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGenerateDialog(String title, String promptHint, AppendAction action) {
        EditText promptInput = new EditText(this);
        promptInput.setHint(promptHint);
        promptInput.setPadding(30, 20, 30, 20);
        promptInput.setMinHeight(100);

        new AlertDialog.Builder(this)
                .setTitle("Generate: " + title)
                .setView(promptInput)
                .setPositiveButton("Generate", (d, w) -> {
                    String prompt = promptInput.getText().toString().trim();
                    if (!prompt.isEmpty()) {
                        generateSection("general", prompt, result -> {
                            action.apply(result, "");
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateSection(String type, String userPrompt, GenerateCallback callback) {
        DeepSeekService service = new DeepSeekService();
        service.generateSection(this, topic, type, userPrompt, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ContentActivity.this, "Generate failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    String content = json.getJSONArray("choices").getJSONObject(0)
                            .getJSONObject("message").getString("content").trim();
                    if (content.startsWith("```")) content = content.substring(content.indexOf("\n") + 1);
                    if (content.endsWith("```")) content = content.substring(0, content.lastIndexOf("\n"));
                    String finalContent = content.trim();
                    runOnUiThread(() -> callback.onResult(finalContent));
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ContentActivity.this, "Parse error", Toast.LENGTH_SHORT).show());
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
