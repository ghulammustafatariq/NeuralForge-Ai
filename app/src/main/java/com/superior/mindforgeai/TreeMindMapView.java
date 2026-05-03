package com.superior.mindforgeai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TreeMindMapView extends FrameLayout {

    private final List<Node> allNodes = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float nodeWidth, nodeHeight, levelSpacing, verticalSpacing, padding;
    private Node root;

    public TreeMindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TreeMindMapView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        linePaint.setColor(Color.parseColor("#666800FF"));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void renderFromJson(JSONObject treeJson) {
        removeAllViews();
        allNodes.clear();
        root = null;

        float density = getResources().getDisplayMetrics().density;
        nodeWidth = 150f * density;
        nodeHeight = 48f * density;
        levelSpacing = 200f * density;
        verticalSpacing = 90f * density;
        padding = 120f * density;
        float halfNodeW = nodeWidth / 2f;

        try {
            JSONObject rootJson = treeJson.optJSONObject("root");
            if (rootJson == null) return;

            root = new Node(rootJson.optString("name", "Topic"),
                    rootJson.optString("detail", ""), true);

            JSONArray childrenJson = treeJson.optJSONArray("children");
            if (childrenJson != null) {
                for (int i = 0; i < childrenJson.length(); i++) {
                    parseChild(root, childrenJson.optJSONObject(i));
                }
            }

            layoutTree(root, padding + halfNodeW, padding * 2);
            createViews(root);

            float maxY = findMaxY(root);
            float maxX = findMaxX(root);

            setMinimumHeight((int)(maxY + padding));
            setMinimumWidth((int)(maxX + padding));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseChild(Node parent, JSONObject childJson) {
        if (childJson == null) return;
        Node child = new Node(childJson.optString("name", "Node"),
                childJson.optString("detail", ""), false);
        child.parent = parent;
        parent.children.add(child);

        JSONArray grandChildren = childJson.optJSONArray("children");
        if (grandChildren != null) {
            for (int i = 0; i < grandChildren.length(); i++) {
                parseChild(child, grandChildren.optJSONObject(i));
            }
        }
    }

    private void layoutTree(Node node, float startX, float startY) {
        node.x = startX;
        node.y = startY;
        allNodes.add(node);

        if (!node.children.isEmpty()) {
            float totalHeight = node.children.size() * verticalSpacing;
            float startChildY = startY - (totalHeight / 2f) + (verticalSpacing / 2f);

            for (int i = 0; i < node.children.size(); i++) {
                float childX = startX + levelSpacing;
                float childY = startChildY + i * verticalSpacing;
                layoutTree(node.children.get(i), childX, childY);
            }
        }
    }

    private void createViews(Node node) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_node_card, this, false);
        MaterialCardView card = (MaterialCardView) cardView;
        TextView tvText = cardView.findViewById(R.id.tvNodeText);
        tvText.setText(node.name);

        if (node.isRoot) {
            card.setCardBackgroundColor(Color.parseColor("#6800FF"));
            card.setStrokeColor(Color.parseColor("#6800FF"));
            tvText.setTextColor(Color.WHITE);
            tvText.setMaxLines(3);
            cardView.setMinimumWidth((int)(nodeWidth));
        }

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cardView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        float cardW = cardView.getMeasuredWidth();
        float cardH = cardView.getMeasuredHeight();
        if (cardW == 0) cardW = nodeWidth;
        if (cardH == 0) cardH = nodeHeight;
        node.cardW = cardW;
        node.cardH = cardH;
        params.leftMargin = (int)(node.x - cardW / 2);
        params.topMargin = (int)(node.y - cardH / 2);
        cardView.setLayoutParams(params);

        node.cardView = cardView;
        addView(cardView);

        cardView.setOnClickListener(v -> {
            if (!node.children.isEmpty()) {
                toggleNode(node);
            }
        });

        cardView.setOnLongClickListener(v -> {
            showNodePopup(node);
            return true;
        });

        for (Node child : node.children) {
            createViews(child);
        }
    }

    private void toggleNode(Node node) {
        if (node.children.isEmpty()) return;

        boolean willExpand = !node.expanded;
        node.expanded = willExpand;

        if (willExpand && node.parent != null) {
            for (Node sibling : node.parent.children) {
                if (sibling != node && sibling.expanded) {
                    sibling.expanded = false;
                    hideAllDescendants(sibling);
                }
            }
        }

        for (Node child : node.children) {
            child.cardView.setVisibility(willExpand ? VISIBLE : GONE);
            if (!willExpand) {
                child.expanded = false;
                hideAllDescendants(child);
            }
        }
        invalidate();
    }

    private void hideAllDescendants(Node node) {
        node.expanded = false;
        for (Node child : node.children) {
            child.cardView.setVisibility(GONE);
            hideAllDescendants(child);
        }
    }

    private void showNodePopup(Node node) {
        Context ctx = getContext();
        LinearLayout popupLayout = new LinearLayout(ctx);
        popupLayout.setOrientation(LinearLayout.VERTICAL);
        popupLayout.setBackgroundColor(Color.WHITE);
        popupLayout.setElevation(12f);
        popupLayout.setPadding(20, 16, 20, 16);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(node.name);
        tvTitle.setTextColor(Color.parseColor("#6800FF"));
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 8);

        TextView tvDetail = new TextView(ctx);
        String detail = node.detail;
        if (detail == null || detail.isEmpty()) {
            detail = "No additional detail for this node.";
        }
        tvDetail.setText(detail);
        tvDetail.setTextColor(Color.parseColor("#1D1D1F"));
        tvDetail.setTextSize(13);
        tvDetail.setMaxWidth((int)(280 * getResources().getDisplayMetrics().density));

        popupLayout.addView(tvTitle);
        popupLayout.addView(tvDetail);

        PopupWindow popup = new PopupWindow(popupLayout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popup.setElevation(12);
        popup.setOutsideTouchable(true);

        popupLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int popupW = popupLayout.getMeasuredWidth();
        int popupH = popupLayout.getMeasuredHeight();

        int[] loc = new int[2];
        node.cardView.getLocationOnScreen(loc);
        int x = loc[0] + node.cardView.getWidth() / 2 - popupW / 2;
        int y = loc[1] - popupH - 12;
        if (y < 80) y = loc[1] + node.cardView.getHeight() + 12;

        popup.showAtLocation(node.cardView, Gravity.NO_GRAVITY, x, y);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        float halfW = nodeWidth / 2f;
        for (Node node : allNodes) {
            for (Node child : node.children) {
                if (!child.cardView.isShown() || !node.cardView.isShown()) continue;

                float px = node.x + node.cardW / 2f;
                float py = node.y;
                float cx = child.x - child.cardW / 2f;
                float cy = child.y;

                float midX = (px + cx) / 2f;
                canvas.drawLine(px, py, midX, py, linePaint);
                canvas.drawLine(midX, py, midX, cy, linePaint);
                canvas.drawLine(midX, cy, cx, cy, linePaint);
            }
        }
    }

    private float findMaxY(Node node) {
        float max = node.y;
        for (Node child : node.children) {
            max = Math.max(max, findMaxY(child));
        }
        return max;
    }

    private float findMaxX(Node node) {
        float max = node.x;
        for (Node child : node.children) {
            max = Math.max(max, findMaxX(child));
        }
        return max;
    }

    static class Node {
        String name, detail;
        boolean isRoot, expanded = true;
        Node parent;
        List<Node> children = new ArrayList<>();
        View cardView;
        float x, y;
        float cardW, cardH;

        Node(String name, String detail, boolean isRoot) {
            this.name = name;
            this.detail = detail;
            this.isRoot = isRoot;
        }
    }
}
