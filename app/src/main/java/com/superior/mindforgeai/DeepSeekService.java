package com.superior.mindforgeai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class DeepSeekService {

    private static final String TAG = "DeepSeekService";
    private static final String BASE_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL = "deepseek-chat";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;

    public DeepSeekService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void forgeContent(Context context, String topic, String description, String style,
                             int depth, boolean includeCode, boolean includeMath,
                             boolean includeVisuals, Callback callback) {

        String prompt = buildPrompt(topic, description, style, depth, includeCode, includeMath, includeVisuals);

        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL);

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", getSystemPrompt());

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(systemMsg);
            messages.put(userMsg);

            body.put("messages", messages);
            body.put("temperature", 0.5);
            body.put("max_tokens", 8192);
            body.put("stream", false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build request body", e);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("MindForgePrefs", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("apiKey", "");
        if (apiKey.isEmpty()) {
            apiKey = "YOUR_API_KEY_HERE";
        }

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        Log.d(TAG, "Sending request for topic: " + topic);
        client.newCall(request).enqueue(callback);
    }

    private String getSystemPrompt() {
        return "You are MindForge AI, an educational content engine. You generate structured knowledge for ANY topic — " +
               "biology, medicine, history, math, programming, cooking, physics, law, arts, engineering, everything. " +
               "NEVER default to AI/ML examples unless the topic is explicitly about AI.\n\n" +
               "Return ONLY valid JSON (no markdown, no code fences, no explanations outside the JSON). " +
               "The JSON must have exactly this structure:\n\n" +
               "{\n" +
               "  \"title\": \"Concise topic title\",\n" +
               "  \"explanation\": [\n" +
               "    {\"heading\": \"Overview\", \"content\": \"2-3 sentence overview. Use **bold** for key terms.\"},\n" +
               "    {\"heading\": \"Key Concepts\", \"content\": \"- concept 1<br/>- concept 2<br/>- concept 3\"},\n" +
               "    {\"heading\": \"How It Works\", \"content\": \"Clear paragraph explanation. Use **bold** and *italic*. Keep paragraphs 3-4 lines max. Use plain language.\"},\n" +
               "    {\"heading\": \"Real-World Examples\", \"content\": \"- example 1<br/>- example 2<br/>- example 3\"},\n" +
               "    {\"heading\": \"Summary\", \"content\": \"1-2 sentence takeaway\"}\n" +
               "  ],\n" +
               "  \"mind_map_tree\": {\n" +
               "    \"root\": {\"name\": \"Central Concept\", \"detail\": \"One sentence about this concept\"},\n" +
               "    \"children\": [\n" +
               "      {\n" +
               "        \"name\": \"Subtopic 1\",\n" +
               "        \"detail\": \"One sentence detail\",\n" +
               "        \"children\": [\n" +
               "          {\"name\": \"Detail 1a\", \"detail\": \"Short explanation\"},\n" +
               "          {\"name\": \"Detail 1b\", \"detail\": \"Short explanation\"}\n" +
               "        ]\n" +
               "      },\n" +
               "      {\n" +
               "        \"name\": \"Subtopic 2\",\n" +
               "        \"detail\": \"One sentence detail\",\n" +
               "        \"children\": [\n" +
               "          {\"name\": \"Detail 2a\", \"detail\": \"Short explanation\"}\n" +
               "        ]\n" +
               "      }\n" +
               "    ]\n" +
               "  },\n" +
               "  \"logical_steps\": [\n" +
               "    {\"step\": 1, \"text\": \"First step — describe clearly. Works for any domain: cooking steps, math proofs, medical procedures, etc.\"},\n" +
               "    {\"step\": 2, \"text\": \"Second step — use **bold** for key actions\"},\n" +
               "    {\"step\": 3, \"text\": \"Third step\"}\n" +
               "  ],\n" +
               "  \"formulas\": [\n" +
               "    {\"formula\": \"E = mc^{2}\", \"meaning\": \"What it represents and when to use it\"}\n" +
               "  ],\n" +
               "  \"code_snippets\": [\n" +
               "    {\"heading\": \"Python: Implementation Example\", \"language\": \"Python\", \"code\": \"def example():\\n    pass\", \"output\": \"Expected output\"}\n" +
               "  ],\n" +
               "  \"chart_data\": {\n" +
               "    \"bar\": {\"labels\": [\"A\",\"B\",\"C\",\"D\",\"E\"], \"values\": [10,25,15,30,20]},\n" +
               "    \"line\": {\"labels\": [\"Start\",\"Step1\",\"Step2\",\"Step3\",\"End\"], \"values\": [1,2,4,3,5]},\n" +
               "    \"scatter\": {\"labels\": [\"P1\",\"P2\",\"P3\",\"P4\",\"P5\"], \"values\": [1,4,2,5,3]}\n" +
               "  }\n" +
               "}\n\n" +
               "CRITICAL RULES:\n" +
               "1. explanation: Use **double asterisks** for bold and *single asterisks* for italic. Use <br/> for line breaks.\n" +
               "2. explanation MUST have exactly 5 sections: Overview, Key Concepts, How It Works, Real-World Examples, Summary\n" +
               "3. mind_map_tree MUST have root + 2-4 children, each with 1-3 grandchildren (3 levels max)\n" +
               "4. code_snippets: If topic HAS code (programming, algorithms, etc.), provide EXACTLY 2 relevant snippets with proper language heading. If topic has NO code (history, MBBS, cooking), set code_snippets to empty array []\n" +
               "5. chart_data: Create MEANINGFUL, TOPIC-RELEVANT data for ALL 3 chart types. DO NOT use placeholder values. Labels and values MUST relate to the specific topic. For example, for 'kinetic energy', bar chart labels could be objects (Car, Bike, Plane) and values their kinetic energy in Joules. For 'English literature', chart could compare number of works by different authors.\n" +
               "6. formulas: If topic has formulas (math, physics, chemistry, engineering), include them using proper LaTeX math syntax. Examples: E = mc^{2}, F = ma, a^{2} + b^{2} = c^{2}, \\frac{a}{b}, \\sqrt{x}, \\sum_{i=1}^{n} x_i, x_{n+1}. Use ^{...} for superscript and _{...} for subscript. Use \\frac{num}{den} for fractions. Use \\sqrt{...} for square roots. Do NOT wrap in $$ or \\( delimiters - just the raw LaTeX expression. If topic has NO formulas (history, literature), set to empty array []\n" +
               "7. logical_steps: Always provide 3-5 steps. Adapt to the topic domain. Use **bold** for key actions.\n" +
               "8. Style parameter: Beginner=simple language analogies, Professor=academic precise, ELI5=extreme simplicity\n" +
               "9. Never use AI/ML/Neural Network examples unless the topic itself is about AI.\n" +
               "10. Return ONLY the JSON object. No markdown. No code fences. Raw JSON only.";
    }

    private String buildPrompt(String topic, String description, String style,
                               int depth, boolean includeCode, boolean includeMath, boolean includeVisuals) {

        StringBuilder focus = new StringBuilder();
        List<String> areas = new ArrayList<>();
        if (includeCode) areas.add("Code/Programming");
        if (includeMath) areas.add("Math/Formulas");
        if (includeVisuals) areas.add("Charts/Visualizations");
        if (areas.isEmpty()) areas.add("General Knowledge");
        for (int i = 0; i < areas.size(); i++) {
            if (i > 0) focus.append(", ");
            focus.append(areas.get(i));
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("TOPIC: ").append(topic).append("\n");
        if (description != null && !description.isEmpty()) {
            prompt.append("CONTEXT: ").append(description).append("\n");
        }
        prompt.append("STYLE: ").append(style).append("\n");
        prompt.append("DEPTH: ").append(depth).append("%\n");
        prompt.append("FOCUS: ").append(focus.toString()).append("\n\n");
        prompt.append("Generate comprehensive educational content following the system instructions EXACTLY. ");
        prompt.append("Return ONLY raw JSON — no markdown, no code fences.");

        return prompt.toString();
    }

    public void generateSection(Context context, String topic, String type, String userPrompt, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL);

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            String sysContent;
            if ("formula".equals(type)) {
                sysContent = "You are MindForge AI. The user is learning about: " + topic +
                        ". They want a formula related to their request. " +
                        "Return ONLY a JSON object like: {\"formula\": \"\\\\frac{a}{b}\", \"meaning\": \"explanation\"}. " +
                        "The formula MUST be in raw LaTeX math syntax. " +
                        "Use \\\\frac{num}{den} for fractions, ^{exp} for superscripts, _{sub} for subscripts, " +
                        "\\\\sqrt{x} for roots, \\\\sum, \\\\prod, \\\\int for operators, \\\\left( \\\\right) for delimiters. " +
                        "Do NOT wrap in $ or $$ or \\\\( \\\\) delimiters. Just raw LaTeX. " +
                        "Return ONLY the JSON object, nothing else.";
            } else if ("code".equals(type)) {
                sysContent = "You are MindForge AI. The user is learning about: " + topic +
                        ". They want code. Return ONLY a JSON object like: " +
                        "{\"heading\": \"Python: Example\", \"code\": \"def f():\\n    pass\", \"output\": \"result\"}. " +
                        "Return ONLY JSON, no markdown, no code fences.";
            } else {
                sysContent = "You are MindForge AI. The user is learning about: " + topic +
                        ". They want to add a " + type + " section to their notes. " +
                        "Generate concise, helpful content based on their request. " +
                        "Return ONLY a JSON object like: {\"heading\": \"Title Here\", \"content\": \"Your content here\"}. " +
                        "Use **bold** for key terms and *italic* for emphasis. " +
                        "Keep it short and relevant. Return ONLY JSON, no markdown, no code fences.";
            }
            systemMsg.put("content", sysContent);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            JSONArray messages = new JSONArray();
            messages.put(systemMsg);
            messages.put(userMsg);

            body.put("messages", messages);
            body.put("temperature", 0.5);
            body.put("max_tokens", 2048);
            body.put("stream", false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build section request", e);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("MindForgePrefs", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("apiKey", "");
        if (apiKey.isEmpty()) apiKey = "YOUR_API_KEY_HERE";

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        Log.d(TAG, "Sending section request, type=" + type + ", topic=" + topic);
        Log.d(TAG, "Request body preview: " + body.toString().substring(0, Math.min(body.toString().length(), 300)));
        client.newCall(request).enqueue(callback);
    }
}
