package com.superior.mindforgeai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ExtendedFloatingActionButton fabNew;
    private View headerSection;
    private TextView tvHistoryTitle, tvHistorySubtitle;

    private String filterPlaylistId;   // null = show all
    private String filterPlaylistName; // display name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        db = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Read optional playlist filter from intent
        filterPlaylistId = getIntent().getStringExtra("PLAYLIST_ID");
        filterPlaylistName = getIntent().getStringExtra("PLAYLIST_NAME");

        initializeViews();

        // Update title if filtering by playlist
        if (filterPlaylistName != null && tvHistoryTitle != null) {
            tvHistoryTitle.setText(filterPlaylistName);
            if (tvHistorySubtitle != null) tvHistorySubtitle.setText("Your saved notes");
        }

        if (headerSection != null) headerSection.setAlpha(0f);
        if (rvHistory != null) rvHistory.setAlpha(0f);
        if (fabNew != null) { fabNew.setAlpha(0f); fabNew.setTranslationY(50f); }

        handleInsets();
        setupRecyclerView();
        setupSwipeToDelete();
        loadHistory();

        fabNew.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        headerSection.postDelayed(this::runEntranceAnimations, 150);
    }

    private void runEntranceAnimations() {
        if (isFinishing()) return;
        DecelerateInterpolator interp = new DecelerateInterpolator(1.2f);
        if (headerSection != null) {
            headerSection.setTranslationY(-30f);
            headerSection.animate().alpha(1f).translationY(0).setDuration(600).setInterpolator(interp).start();
        }
        if (rvHistory != null) rvHistory.animate().alpha(1f).setDuration(800).start();
        if (fabNew != null) fabNew.animate().alpha(1f).translationY(0).setDuration(600).setStartDelay(400).setInterpolator(interp).start();
    }

    private void handleInsets() {
        View root = findViewById(R.id.historyRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, 0, bars.right, bars.bottom);
                return insets;
            });
        }
    }

    private void initializeViews() {
        rvHistory = findViewById(R.id.rvHistory);
        fabNew = findViewById(R.id.fabNew);
        headerSection = findViewById(R.id.headerSection);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvHistorySubtitle = findViewById(R.id.tvHistorySubtitle);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(new ArrayList<>(), new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(HistoryEntity entity) {
                Intent intent = new Intent(HistoryActivity.this, ContentActivity.class);
                intent.putExtra("TOPIC", entity.getTopic());
                intent.putExtra("AI_RESULT", entity.getAiResult());
                intent.putExtra("DOC_ID", entity.getId());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        // Long-press → Add to playlist
        adapter.setOnItemLongClickListener(entity -> showAddToPlaylistDialog(entity));

        // Delete button
        adapter.setOnItemDeleteListener(entity -> new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("Delete Note")
                .setMessage("Delete \"" + entity.getTopic() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    int pos = findEntityPosition(entity);
                    if (pos >= 0) adapter.removeAt(pos);
                    deleteFromStorage(entity);
                })
                .setNegativeButton("Cancel", null)
                .show());

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void showAddToPlaylistDialog(HistoryEntity entity) {
        new Thread(() -> {
            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
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
                        String pName = playlists.get(which).getName();
                        new Thread(() -> {
                            // Save to local Room DB
                            db.historyDao().setPlaylist(entity.getId(), pid);
                            // Also persist to Firestore so it survives app restart
                            if (mAuth.getCurrentUser() != null) {
                                firestore.collection("users")
                                        .document(mAuth.getCurrentUser().getUid())
                                        .collection("history")
                                        .document(entity.getId())
                                        .update("playlistId", pid)
                                        .addOnFailureListener(ex ->
                                                Log.e(TAG, "Firestore playlistId update failed", ex));
                            }
                            runOnUiThread(() -> Toast.makeText(this,
                                    "Added to " + pName, Toast.LENGTH_SHORT).show());
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }).start();
    }

    private void loadHistory() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        if (mAuth.getCurrentUser() != null && filterPlaylistId == null && filterPlaylistName == null) {
            syncFromFirestore(uid);
        } else {
            loadFromRoom(uid);
        }
    }

    private void syncFromFirestore(String uid) {
        firestore.collection("users").document(uid).collection("history").get()
                .addOnSuccessListener(querySnapshot -> {
                    new Thread(() -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                String id = doc.getString("id");
                                String topic = doc.getString("topic");
                                String description = doc.getString("description");
                                String style = doc.getString("style");
                                int depth = doc.getLong("depth") != null ? doc.getLong("depth").intValue() : 1;
                                String aiResult = doc.getString("aiResult");
                                long createdAt = doc.getLong("createdAt") != null ? doc.getLong("createdAt") : System.currentTimeMillis();
                            if (id != null && topic != null) {
                                    HistoryEntity e = new HistoryEntity(id, uid, topic,
                                            description != null ? description : "",
                                            style != null ? style : "Beginner", depth,
                                            aiResult != null ? aiResult : "", createdAt);
                                    // Preserve playlistId from Firestore so collection assignments survive restarts
                                    String storedPlaylistId = doc.getString("playlistId");
                                    if (storedPlaylistId != null && !storedPlaylistId.isEmpty()) {
                                        e.setPlaylistId(storedPlaylistId);
                                    } else {
                                        // Keep existing local playlistId if Firestore has none
                                        HistoryEntity existing = db.historyDao().getById(id);
                                        if (existing != null && existing.getPlaylistId() != null) {
                                            e.setPlaylistId(existing.getPlaylistId());
                                        }
                                    }
                                    db.historyDao().insert(e);
                                }
                            } catch (Exception e) { Log.e(TAG, "Sync error", e); }
                        }
                        runOnUiThread(() -> loadFromRoom(uid));
                    }).start();
                })
                .addOnFailureListener(e -> loadFromRoom(uid));
    }

    private void loadFromRoom(String uid) {
        new Thread(() -> {
            List<HistoryEntity> entities;
            if (filterPlaylistId != null) {
                // Filter by specific playlist
                entities = db.historyDao().getByPlaylistId(uid, filterPlaylistId);
            } else if ("All Notes".equals(filterPlaylistName)) {
                entities = db.historyDao().getAllByUid(uid);
            } else {
                entities = db.historyDao().getAllByUid(uid);
            }
            runOnUiThread(() -> {
                if (entities.isEmpty()) {
                    Toast.makeText(this, "No notes here yet", Toast.LENGTH_SHORT).show();
                    adapter.setItems(entities);
                } else {
                    adapter.setItems(entities);
                }
            });
        }).start();
    }

    private void deleteFromStorage(HistoryEntity entity) {
        new Thread(() -> {
            db.historyDao().delete(entity);
            if (mAuth.getCurrentUser() != null) {
                firestore.collection("users").document(mAuth.getCurrentUser().getUid())
                        .collection("history").document(entity.getId()).delete();
            }
        }).start();
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
    }

    private int findEntityPosition(HistoryEntity entity) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            HistoryEntity e = adapter.getEntity(i);
            if (e != null && e.getId().equals(entity.getId())) return i;
        }
        return -1;
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                HistoryEntity entity = adapter.getEntity(pos);
                if (entity != null) { adapter.removeAt(pos); deleteFromStorage(entity); }
                else adapter.notifyItemChanged(pos);
            }
            @Override public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                if (vh instanceof HistoryAdapter.HeaderViewHolder) return 0;
                return super.getSwipeDirs(rv, vh);
            }
        }).attachToRecyclerView(rvHistory);
    }
}
