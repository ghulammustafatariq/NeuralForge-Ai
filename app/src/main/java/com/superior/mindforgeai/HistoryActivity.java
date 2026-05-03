package com.superior.mindforgeai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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

        initializeViews();
        
        // Hide initially for smooth entrance
        if (headerSection != null) headerSection.setAlpha(0f);
        if (rvHistory != null) rvHistory.setAlpha(0f);
        if (fabNew != null) {
            fabNew.setAlpha(0f);
            fabNew.setTranslationY(50f);
        }

        handleInsets();
        setupRecyclerView();
        setupSwipeToDelete();
        loadHistory();

        fabNew.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Run animations after short delay
        headerSection.postDelayed(this::runEntranceAnimations, 150);
    }

    private void runEntranceAnimations() {
        if (isFinishing()) return;
        
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.2f);

        if (headerSection != null) {
            headerSection.setTranslationY(-30f);
            headerSection.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(600)
                    .setInterpolator(interpolator)
                    .start();
        }

        if (rvHistory != null) {
            rvHistory.animate()
                    .alpha(1f)
                    .setDuration(800)
                    .start();
        }

        if (fabNew != null) {
            fabNew.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(600)
                    .setStartDelay(400)
                    .setInterpolator(interpolator)
                    .start();
        }
    }

    private void handleInsets() {
        View historyRoot = findViewById(R.id.historyRoot);
        if (historyRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(historyRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void initializeViews() {
        rvHistory = findViewById(R.id.rvHistory);
        fabNew = findViewById(R.id.fabNew);
        headerSection = findViewById(R.id.headerSection);
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

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        if (mAuth.getCurrentUser() != null) {
            syncFromFirestore(uid);
        } else {
            loadFromRoom(uid);
        }
    }

    private void syncFromFirestore(String uid) {
        firestore.collection("users").document(uid)
                .collection("history")
                .get()
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
                                    HistoryEntity entity = new HistoryEntity(id, uid, topic, description != null ? description : "",
                                            style != null ? style : "Beginner", depth, aiResult != null ? aiResult : "", createdAt);
                                    db.historyDao().insert(entity);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to sync history item", e);
                            }
                        }
                        runOnUiThread(() -> loadFromRoom(uid));
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore sync failed", e);
                    loadFromRoom(uid);
                });
    }

    private void loadFromRoom(String uid) {
        new Thread(() -> {
            List<HistoryEntity> entities = db.historyDao().getAllByUid(uid);
            runOnUiThread(() -> {
                if (entities.isEmpty()) {
                    setEmptyState();
                } else {
                    adapter.setItems(entities);
                }
            });
        }).start();
    }

    private void setEmptyState() {
        List<HistoryEntity> empty = new ArrayList<>();
        adapter.setItems(empty);
        Toast.makeText(this, "No forged history yet", Toast.LENGTH_SHORT).show();
    }

    private void deleteFromStorage(HistoryEntity entity) {
        new Thread(() -> {
            db.historyDao().delete(entity);
            if (mAuth.getCurrentUser() != null) {
                firestore.collection("users").document(mAuth.getCurrentUser().getUid())
                        .collection("history").document(entity.getId()).delete();
            }
        }).start();
        Toast.makeText(this, "Forge deleted", Toast.LENGTH_SHORT).show();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                HistoryEntity entity = adapter.getEntity(position);
                if (entity != null) {
                    adapter.removeAt(position);
                    deleteFromStorage(entity);
                } else {
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof HistoryAdapter.HeaderViewHolder) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvHistory);
    }
}
