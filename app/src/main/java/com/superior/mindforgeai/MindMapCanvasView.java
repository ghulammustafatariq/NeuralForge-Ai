package com.superior.mindforgeai;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MindMapCanvasView extends View {

    private static final float MIN_SCALE = 0.25f;
    private static final float MAX_SCALE = 3.5f;
    private static final int[] BRANCH_COLORS = {
            0xFF8C33FF, 0xFF7C4DFF, 0xFFE040FB, 0xFF448AFF, 0xFF18B2FF, 0xFFB388FF,
            0xFF00E5FF, 0xFFEA80FC, 0xFF82B1FF, 0xFFCCFF90
    };

    private float scale = 1f;
    private float offsetX, offsetY;
    private float density;

    private Node root;
    private final List<Node> allNodes = new ArrayList<>();
    private float totalWidth, totalHeight;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private Node touchedNode;
    private float downX, downY, lastX, lastY;
    private float dragStartWorldX, dragStartWorldY;
    private boolean isDragging;
    private long lastTapTime;
    private Node pendingSingleTapNode;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minimapBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minimapDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minimapVpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float dashPhase;
    private ValueAnimator dashAnimator;

    private int searchIndex = -1;
    private List<Node> searchResults = new ArrayList<>();
    private String searchQuery = "";

    private RectF minimapBounds = new RectF();
    private Node focusedNode;

    private OnNodeInteractionListener interactionListener;

    public interface OnNodeInteractionListener {
        void onNodeLongPress(String name, String detail);
        void onNodeChanged();
        void onNodeDeleteRequest(Node node);
        void onNodeDoubleTap(Node node);
    }

    public MindMapCanvasView(Context context) {
        super(context);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        init(context);
    }

    public MindMapCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        init(context);
    }

    private void init(Context context) {
        density = getResources().getDisplayMetrics().density;

        shadowPaint.setColor(0x00000000);
        shadowPaint.setShadowLayer(8 * density, 0, 4 * density, 0x26000000);

        nodeFillPaint.setStyle(Paint.Style.FILL);

        nodeStrokePaint.setStyle(Paint.Style.STROKE);
        nodeStrokePaint.setAntiAlias(true);

        textPaint.setColor(0xFF1D1D1F);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setAntiAlias(true);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(5 * density);
        glowPaint.setAntiAlias(true);
        glowPaint.setPathEffect(new DashPathEffect(new float[]{8 * density, 8 * density}, 0));

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        minimapBgPaint.setColor(0xF0FFFFFF);
        minimapBgPaint.setAntiAlias(true);
        minimapDotPaint.setColor(0xFF6800FF);
        minimapDotPaint.setStyle(Paint.Style.FILL);
        minimapDotPaint.setAntiAlias(true);
        minimapVpPaint.setColor(0x4D6800FF);
        minimapVpPaint.setStyle(Paint.Style.FILL);
        minimapVpPaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        dashAnimator = ValueAnimator.ofFloat(0, 20 * density);
        dashAnimator.setDuration(800);
        dashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        dashAnimator.setInterpolator(null);
        dashAnimator.addUpdateListener(a -> {
            dashPhase = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    public void setOnNodeInteractionListener(OnNodeInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void renderFromJson(JSONObject treeJson) {
        allNodes.clear();
        root = null;
        searchResults.clear();
        searchIndex = -1;
        searchQuery = "";

        try {
            JSONObject rootJson = treeJson.optJSONObject("root");
            if (rootJson == null) return;

            root = new Node(rootJson.optString("name", "Topic"),
                    rootJson.optString("detail", ""), true, 0);
            root.branchColor = 0xFF6800FF;

            JSONArray childrenJson = treeJson.optJSONArray("children");
            if (childrenJson != null) {
                for (int i = 0; i < childrenJson.length(); i++) {
                    JSONObject child = childrenJson.optJSONObject(i);
                    if (child != null) {
                        parseChild(root, child, i % BRANCH_COLORS.length);
                    }
                }
            }

            layoutTree();
            autoFit();

            dashAnimator.start();
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseChild(Node parent, JSONObject childJson, int colorIndex) {
        if (childJson == null) return;

        Node child = new Node(childJson.optString("name", "Node"),
                childJson.optString("detail", ""), false, parent.depth + 1);
        child.parent = parent;
        child.branchColor = BRANCH_COLORS[colorIndex];
        parent.children.add(child);
        allNodes.add(child);

        JSONArray grandChildren = childJson.optJSONArray("children");
        if (grandChildren != null) {
            for (int i = 0; i < grandChildren.length(); i++) {
                parseChild(child, grandChildren.optJSONObject(i), colorIndex);
            }
        }
    }

    private void layoutTree() {
        if (root == null) return;

        float levelSpacing = 210 * density;
        float startX = 80 * density;
        float startY = 800 * density;

        root.x = startX;
        root.y = startY;
        allNodes.add(0, root);

        if (!root.children.isEmpty()) {
            layoutSubtree(root, startX, levelSpacing, true);
        }

        float maxX = 0, maxY = 0, minY = Float.MAX_VALUE;
        for (Node n : allNodes) {
            float w = getNodeWidth(n);
            float h = getNodeHeight(n);
            maxX = Math.max(maxX, n.x + w);
            maxY = Math.max(maxY, n.y + h);
            minY = Math.min(minY, n.y - h);
        }
        totalWidth = Math.max(maxX + 200 * density, 1200 * density);
        totalHeight = Math.max(maxY - minY + 400 * density, 1600 * density);

        float offset = -minY + 200 * density;
        for (Node n : allNodes) {
            n.y += offset;
        }
    }

    private void layoutSubtree(Node node, float x, float levelSpacing, boolean absoluteY) {
        node.x = x;

        if (node.children.isEmpty()) {
            if (!absoluteY) node.y = 0;
            computeSubtreeBounds(node);
            return;
        }

        float childX = x + levelSpacing;
        float nextSpacing = levelSpacing * 0.94f;
        float gap = 20 * density;

        for (Node child : node.children) {
            layoutSubtree(child, childX, nextSpacing, false);
        }

        float top = 0;
        for (int i = 0; i < node.children.size(); i++) {
            Node child = node.children.get(i);
            if (i > 0) top += gap;
            float dy = top - child.subtreeMinY;
            child.y += dy;
            shiftDescendants(child, dy);
            top += (child.subtreeMaxY - child.subtreeMinY);
        }

        float center = -top / 2f;
        for (Node child : node.children) {
            child.y += center;
            shiftDescendants(child, center);
        }

        if (absoluteY) {
            for (Node child : node.children) {
                child.y += node.y;
                shiftDescendants(child, node.y);
            }
        } else {
            node.y = 0;
        }
        computeSubtreeBounds(node);
    }

    private void computeSubtreeBounds(Node node) {
        float h = getNodeHeight(node);
        if (node.children.isEmpty()) {
            node.subtreeMinY = -h / 2f;
            node.subtreeMaxY = h / 2f;
        } else {
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (Node child : node.children) {
                float childRelY = child.y - node.y;
                minY = Math.min(minY, childRelY + child.subtreeMinY);
                maxY = Math.max(maxY, childRelY + child.subtreeMaxY);
            }
            node.subtreeMinY = Math.min(-h / 2f, minY);
            node.subtreeMaxY = Math.max(h / 2f, maxY);
        }
    }

    private void shiftDescendants(Node node, float dy) {
        for (Node child : node.children) {
            child.y += dy;
            shiftDescendants(child, dy);
        }
    }

    private float getNodeWidth(Node node) {
        switch (node.depth) {
            case 0: return 190 * density;
            case 1: return 155 * density;
            case 2: return 135 * density;
            default: return 115 * density;
        }
    }

    private float getNodeHeight(Node node) {
        switch (node.depth) {
            case 0: return 58 * density;
            case 1: return 50 * density;
            case 2: return 44 * density;
            default: return 40 * density;
        }
    }

    private float getNodeRadius(Node node) {
        switch (node.depth) {
            case 0: return 22 * density;
            case 1: return 18 * density;
            case 2: return 15 * density;
            default: return 12 * density;
        }
    }

    private float getNodeFontSize(Node node) {
        switch (node.depth) {
            case 0: return 15 * density;
            case 1: return 13 * density;
            case 2: return 12 * density;
            default: return 11 * density;
        }
    }

    private void autoFit() {
        if (totalWidth <= 0 || totalHeight <= 0) return;

        float pad = 40 * density;
        float availW = getWidth() > 0 ? getWidth() - 2 * pad : 1000 * density;
        float availH = getHeight() > 0 ? getHeight() - 2 * pad : 1400 * density;

        scale = Math.min(availW / totalWidth, availH / totalHeight);
        scale = Math.max(0.25f, Math.min(1.0f, scale));

        offsetX = pad;
        offsetY = (getHeight() > 0 ? getHeight() : availH) / 2f - (totalHeight * scale) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (root == null) return;

        canvas.drawColor(0xFFF8F7FC);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        drawGridBackground(canvas);
        drawConnections(canvas);
        drawNodes(canvas);

        canvas.restore();

        drawMinimap(canvas);
    }

    private void drawGridBackground(Canvas canvas) {
        float dotSpacing = 32 * density;
        float dotRadius = 1.8f * density;
        float parallaxX = offsetX * 0.25f;
        float parallaxY = offsetY * 0.25f;

        float worldLeft = -parallaxX / scale - dotSpacing;
        float worldTop = -parallaxY / scale - dotSpacing;
        float worldRight = (getWidth() - parallaxX) / scale + dotSpacing;
        float worldBottom = (getHeight() - parallaxY) / scale + dotSpacing;

        int startCol = (int) (worldLeft / dotSpacing);
        int endCol = (int) (worldRight / dotSpacing) + 1;
        int startRow = (int) (worldTop / dotSpacing);
        int endRow = (int) (worldBottom / dotSpacing) + 1;

        float alpha = 0.35f + scale * 0.15f;

        for (int col = startCol; col < endCol; col++) {
            for (int row = startRow; row < endRow; row++) {
                float x = col * dotSpacing;
                float y = row * dotSpacing;
                float distFromCenter = Math.abs(y - 800 * density) / (800 * density);
                int alphaVal = (int) (Math.max(15, alpha * (1f - distFromCenter * 0.5f) * 255));
                dotPaint.setColor(Color.argb(alphaVal, 104, 0, 255));
                dotPaint.setAlpha(alphaVal);
                canvas.drawCircle(x, y, dotRadius, dotPaint);
            }
        }
    }

    private void drawConnections(Canvas canvas) {
        for (Node node : allNodes) {
            if (!node.expanded || node.children.isEmpty()) continue;
            for (Node child : node.children) {
                drawConnection(canvas, node, child);
            }
        }
    }

    private void drawConnection(Canvas canvas, Node parent, Node child) {
        float pw = getNodeWidth(parent);
        float cw = getNodeWidth(child);

        float startX = parent.x + pw / 2f;
        float startY = parent.y;
        float endX = child.x - cw / 2f;
        float endY = child.y;

        float midX = (startX + endX) / 2f;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.cubicTo(midX, startY, midX, endY, endX, endY);

        linePaint.setColor(parent.branchColor);
        linePaint.setAlpha(160);
        linePaint.setStrokeWidth(2.2f * density * (child.depth == 1 ? 1f : 0.75f));
        linePaint.setPathEffect(new DashPathEffect(new float[]{10 * density, 6 * density}, dashPhase));
        canvas.drawPath(path, linePaint);
    }

    private void drawNodes(Canvas canvas) {
        for (Node node : allNodes) {
            if (!isNodeVisible(node)) continue;
            drawNode(canvas, node);
        }
    }

    private boolean isNodeVisible(Node node) {
        if (node.parent == null) return true;
        return node.parent.expanded;
    }

    private void drawNode(Canvas canvas, Node node) {
        float w = getNodeWidth(node);
        float h = getNodeHeight(node);
        float radius = getNodeRadius(node);
        float rx = node.x - w / 2f;
        float ry = node.y - h / 2f;

        RectF rect = new RectF(rx, ry, rx + w, ry + h);

        shadowPaint.setShadowLayer(10 * density, 0, 5 * density, 0x1A000000);
        canvas.drawRoundRect(rect, radius, radius, shadowPaint);

        if (node.isRoot) {
            glowPaint.setColor(0x336800FF);
            glowPaint.setStrokeWidth(6 * density);
            RectF glowRect = new RectF(rx - 3 * density, ry - 3 * density, rx + w + 3 * density, ry + h + 3 * density);
            canvas.drawRoundRect(glowRect, radius + 3 * density, radius + 3 * density, glowPaint);
        }

        nodeFillPaint.setColor(node.isRoot ? 0xFF6800FF : 0xF8FAFFFF);
        nodeFillPaint.setAlpha(node.isRoot ? 255 : 240);
        canvas.drawRoundRect(rect, radius, radius, nodeFillPaint);

        if (focusedNode == node) {
            nodeStrokePaint.setColor(0xFF6800FF);
            nodeStrokePaint.setStrokeWidth(3f * density);
            nodeStrokePaint.setAlpha(255);
        } else if (node.isRoot) {
            nodeStrokePaint.setColor(0xFF8C33FF);
            nodeStrokePaint.setStrokeWidth(2.5f * density);
            nodeStrokePaint.setAlpha(200);
        } else {
            nodeStrokePaint.setColor(node.branchColor);
            nodeStrokePaint.setStrokeWidth(1.8f * density);
            nodeStrokePaint.setAlpha(node.depth == 1 ? 180 : 120);
        }
        nodeStrokePaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(rect, radius, radius, nodeStrokePaint);

        float fontSize = getNodeFontSize(node);
        textPaint.setTextSize(fontSize);
        textPaint.setColor(node.isRoot ? 0xFFFFFFFF : 0xFF1D1D1F);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", node.isRoot ? Typeface.BOLD : Typeface.NORMAL));

        String displayText = node.name;
        float maxTextWidth = w - 24 * density;
        float textWidth = textPaint.measureText(displayText);
        if (textWidth > maxTextWidth && displayText.length() > 4) {
            int chars = (int) (maxTextWidth / textWidth * displayText.length()) - 2;
            if (chars > 3) {
                displayText = displayText.substring(0, Math.min(chars, displayText.length())) + "..";
            }
        }

        float textY = node.y + 2 * density - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(displayText, node.x, textY, textPaint);

        if (!node.isRoot && node.depth <= 1) {
            float dotX = node.x + w / 2f - 5 * density;
            float dotY = node.y - h / 2f + 5 * density;
            Paint dotP = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotP.setColor(node.branchColor);
            dotP.setAlpha(200);
            canvas.drawCircle(dotX, dotY, 3.5f * density, dotP);
        }
    }

    private void drawMinimap(Canvas canvas) {
        if (totalWidth <= 0 || totalHeight <= 0) return;

        float mmW = 140 * density;
        float mmH = 90 * density;
        float mmX = getWidth() - mmW - 16 * density;
        float mmY = getHeight() - mmH - 80 * density;

        minimapBounds.set(mmX, mmY, mmX + mmW, mmY + mmH);

        float mmRadius = 14 * density;
        minimapBgPaint.setColor(0xEEFFFFFF);
        minimapBgPaint.setShadowLayer(8 * density, 0, 3 * density, 0x20000000);
        canvas.drawRoundRect(mmX, mmY, mmX + mmW, mmY + mmH, mmRadius, mmRadius, minimapBgPaint);

        int mmSave = canvas.save();
        canvas.clipRect(mmX + 2 * density, mmY + 2 * density, mmX + mmW - 2 * density, mmY + mmH - 2 * density);

        float mmScaleX = (mmW - 8 * density) / totalWidth;
        float mmScaleY = (mmH - 8 * density) / totalHeight;
        float mmScale = Math.min(mmScaleX, mmScaleY);

        float mmOX = mmX + 4 * density;
        float mmOY = mmY + 4 * density;

        for (Node node : allNodes) {
            if (!isNodeVisible(node)) continue;
            minimapDotPaint.setColor(node.isRoot ? 0xFF6800FF : node.branchColor);
            minimapDotPaint.setAlpha(180);
            float nx = mmOX + node.x * mmScale;
            float ny = mmOY + node.y * mmScale;
            canvas.drawCircle(nx, ny, Math.max(1.5f * density, node.depth == 0 ? 3f * density : 2f * density), minimapDotPaint);
        }

        float vpL = mmOX - offsetX / scale * mmScale;
        float vpT = mmOY - offsetY / scale * mmScale;
        float vpR = mmOX + (getWidth() - offsetX) / scale * mmScale;
        float vpB = mmOY + (getHeight() - offsetY) / scale * mmScale;

        minimapVpPaint.setStyle(Paint.Style.STROKE);
        minimapVpPaint.setColor(0xFF6800FF);
        minimapVpPaint.setStrokeWidth(1.5f * density);
        minimapVpPaint.setAlpha(200);
        canvas.drawRect(vpL, vpT, vpR, vpB, minimapVpPaint);

        canvas.restoreToCount(mmSave);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                lastX = x;
                lastY = y;
                isDragging = false;
                touchedNode = hitTestNode(x, y);
                if (touchedNode != null) {
                    dragStartWorldX = touchedNode.x;
                    dragStartWorldY = touchedNode.y;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (scaleDetector.isInProgress()) return true;

                float dx = x - lastX;
                float dy = y - lastY;
                float totalDx = x - downX;
                float totalDy = y - downY;

                if (!isDragging && Math.abs(totalDx) > 8 * density || Math.abs(totalDy) > 8 * density) {
                    isDragging = true;
                }

                if (isDragging && touchedNode != null) {
                    touchedNode.x += dx / scale;
                    touchedNode.y += dy / scale;
                    invalidate();
                } else if (isDragging) {
                    offsetX += dx;
                    offsetY += dy;
                    clampOffset();
                    invalidate();
                }

                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_UP:
                float upDx = x - downX;
                float upDy = y - downY;
                float dist = (float) Math.sqrt(upDx * upDx + upDy * upDy);

                if (!isDragging && dist < 12 * density) {
                    if (isMinimapTap(x, y)) {
                        navigateMinimap(x, y);
                    } else if (touchedNode != null) {
                        handleSingleTap(touchedNode);
                    }
                }

                if (touchedNode != null && interactionListener != null) {
                    interactionListener.onNodeChanged();
                }

                touchedNode = null;
                isDragging = false;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                touchedNode = null;
                isDragging = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    private Node hitTestNode(float screenX, float screenY) {
        float worldX = (screenX - offsetX) / scale;
        float worldY = (screenY - offsetY) / scale;

        for (int i = allNodes.size() - 1; i >= 0; i--) {
            Node node = allNodes.get(i);
            if (!isNodeVisible(node)) continue;

            float w = getNodeWidth(node);
            float h = getNodeHeight(node);
            float left = node.x - w / 2f;
            float top = node.y - h / 2f;
            float right = left + w;
            float bottom = top + h;

            if (worldX >= left && worldX <= right && worldY >= top && worldY <= bottom) {
                return node;
            }
        }
        return null;
    }

    private void handleSingleTap(Node node) {
        if (node.children.isEmpty()) {
            focusNode(node);
            return;
        }

        toggleNode(node);
    }

    private void toggleNode(Node node) {
        node.expanded = !node.expanded;
        invalidate();
    }

    private void focusNode(Node node) {
        focusedNode = node;

        float targetScale = 1.5f;
        float targetX = getWidth() / 2f - node.x * targetScale;
        float targetY = getHeight() / 2f - node.y * targetScale;

        ValueAnimator zoomAnim = ValueAnimator.ofFloat(0f, 1f);
        zoomAnim.setDuration(350);
        final float startScale = scale;
        final float startOX = offsetX;
        final float startOY = offsetY;
        zoomAnim.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            scale = startScale + (targetScale - startScale) * t;
            offsetX = startOX + (targetX - startOX) * t;
            offsetY = startOY + (targetY - startOY) * t;
            invalidate();
        });
        zoomAnim.start();

        postDelayed(() -> {
            focusedNode = null;
            invalidate();
        }, 2000);
    }

    public void deleteNode(Node nodeToDelete) {
        if (nodeToDelete == null || nodeToDelete.isRoot || root == null) return;

        Node parent = nodeToDelete.parent;
        if (parent != null) {
            parent.children.remove(nodeToDelete);
        }

        removeFromAllNodes(nodeToDelete);

        layoutTree();
        autoFit();
        invalidate();

        if (interactionListener != null) {
            interactionListener.onNodeChanged();
        }
    }

    private void removeFromAllNodes(Node node) {
        allNodes.remove(node);
        for (Node child : node.children) {
            removeFromAllNodes(child);
        }
    }

    public void updateFromJson(JSONObject treeJson) {
        allNodes.clear();
        root = null;
        searchResults.clear();
        searchIndex = -1;
        searchQuery = "";

        try {
            JSONObject rootJson = treeJson.optJSONObject("root");
            if (rootJson == null) return;

            root = new Node(rootJson.optString("name", "Topic"),
                    rootJson.optString("detail", ""), true, 0);
            root.branchColor = 0xFF6800FF;

            JSONArray childrenJson = treeJson.optJSONArray("children");
            if (childrenJson != null) {
                for (int i = 0; i < childrenJson.length(); i++) {
                    JSONObject child = childrenJson.optJSONObject(i);
                    if (child != null) {
                        parseChild(root, child, i % BRANCH_COLORS.length);
                    }
                }
            }

            layoutTree();
            autoFit();
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject getTreeJson() {
        if (root == null) return null;
        try {
            JSONObject json = new JSONObject();
            json.put("root", nodeToJson(root));
            JSONArray childrenArr = new JSONArray();
            for (Node child : root.children) {
                childrenArr.put(nodeToJson(child));
            }
            json.put("children", childrenArr);
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject nodeToJson(Node node) throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", node.name);
        json.put("detail", node.detail != null ? node.detail : "");
        JSONArray childrenArr = new JSONArray();
        for (Node child : node.children) {
            childrenArr.put(nodeToJson(child));
        }
        json.put("children", childrenArr);
        return json;
    }
    public void zoomToFit() {
        if (root == null) return;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(400);

        float startScale = scale;
        float startOX = offsetX;
        float startOY = offsetY;

        float availW = getWidth() > 0 ? getWidth() - 80 * density : 1000 * density;
        float availH = getHeight() > 0 ? getHeight() - 80 * density : 1400 * density;
        float rawScale = Math.min(availW / totalWidth, availH / totalHeight);
        final float endScale = Math.max(0.25f, Math.min(1.0f, rawScale));
        final float endOX = 40 * density;
        final float endOY = (getHeight() > 0 ? getHeight() : availH) / 2f - (totalHeight * endScale) / 2f;

        anim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            scale = startScale + (endScale - startScale) * t;
            offsetX = startOX + (endOX - startOX) * t;
            offsetY = startOY + (endOY - startOY) * t;
            invalidate();
        });
        anim.start();
    }

    private boolean isMinimapTap(float x, float y) {
        return minimapBounds.contains(x, y);
    }

    private void navigateMinimap(float tapX, float tapY) {
        float mmW = 140 * density;
        float mmH = 90 * density;
        float mmX = getWidth() - mmW - 16 * density;
        float mmY = getHeight() - mmH - 80 * density;

        float mmScale = Math.min((mmW - 8 * density) / totalWidth, (mmH - 8 * density) / totalHeight);

        float worldTapX = (tapX - mmX - 4 * density) / mmScale;
        float worldTapY = (tapY - mmY - 4 * density) / mmScale;

        offsetX = getWidth() / 2f - worldTapX * scale;
        offsetY = getHeight() / 2f - worldTapY * scale;
        clampOffset();
        invalidate();
    }

    private void clampOffset() {
        float viewW = getWidth();
        float viewH = getHeight();
        float worldW = totalWidth * scale;
        float worldH = totalHeight * scale;

        float pad = 100 * density;
        float minimapPadX = 170 * density;
        float minimapPadY = 190 * density;

        offsetX = Math.max(viewW - worldW - minimapPadX, Math.min(pad, offsetX));
        offsetY = Math.max(viewH - worldH - minimapPadY, Math.min(pad, offsetY));
    }

    public void searchNode(String query) {
        searchQuery = query.toLowerCase().trim();
        searchResults.clear();
        searchIndex = -1;

        if (searchQuery.isEmpty()) {
            focusedNode = null;
            invalidate();
            return;
        }

        for (Node node : allNodes) {
            if (node.name.toLowerCase().contains(searchQuery)) {
                searchResults.add(node);
            }
        }

        if (!searchResults.isEmpty()) {
            searchIndex = 0;
            navigateToSearchResult();
        }
    }

    public void nextSearchResult() {
        if (searchResults.isEmpty()) return;
        searchIndex = (searchIndex + 1) % searchResults.size();
        navigateToSearchResult();
    }

    private void navigateToSearchResult() {
        if (searchIndex < 0 || searchIndex >= searchResults.size()) return;

        Node node = searchResults.get(searchIndex);

        if (node.parent != null && !node.parent.expanded) {
            expandAncestors(node);
        }

        focusNode(node);
    }

    private void expandAncestors(Node node) {
        Node current = node.parent;
        while (current != null) {
            current.expanded = true;
            current = current.parent;
        }
        invalidate();
    }

    public void clearSearch() {
        searchQuery = "";
        searchResults.clear();
        searchIndex = -1;
        focusedNode = null;
        invalidate();
    }

    public boolean hasSearchResults() {
        return !searchResults.isEmpty();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scale * detector.getScaleFactor();
            newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            offsetX = focusX - (focusX - offsetX) * (newScale / scale);
            offsetY = focusY - (focusY - offsetY) * (newScale / scale);
            scale = newScale;
            clampOffset();
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Node node = hitTestNode(e.getX(), e.getY());
            if (node != null && interactionListener != null) {
                interactionListener.onNodeDoubleTap(node);
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Node node = hitTestNode(e.getX(), e.getY());
            if (node != null && interactionListener != null) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                interactionListener.onNodeLongPress(node.name, node.detail);
                interactionListener.onNodeDeleteRequest(node);
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (scaleDetector.isInProgress()) return false;
            if (touchedNode != null) return false;
            offsetX -= distanceX;
            offsetY -= distanceY;
            clampOffset();
            invalidate();
            return true;
        }
    }

    private void zoomToFitBranch(Node node) {
        float targetScale = Math.max(scale, 1.8f);
        float targetX = getWidth() / 2f - node.x * targetScale;
        float targetY = getHeight() / 2f - node.y * targetScale;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(350);
        float startScale = scale;
        float startOX = offsetX;
        float startOY = offsetY;
        anim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            scale = startScale + (targetScale - startScale) * t;
            offsetX = startOX + (targetX - startOX) * t;
            offsetY = startOY + (targetY - startOY) * t;
            invalidate();
        });
        anim.start();
    }

    public static class Node {
        String name, detail;
        boolean isRoot, expanded = true;
        int depth;
        int branchColor;
        Node parent;
        List<Node> children = new ArrayList<>();
        float x, y;
        float subtreeMinY, subtreeMaxY;

        Node(String name, String detail, boolean isRoot, int depth) {
            this.name = name;
            this.detail = detail;
            this.isRoot = isRoot;
            this.depth = depth;
        }
    }
}
