package com.superior.mindforgeai;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;

import com.google.android.material.button.MaterialButton;

/**
 * Creates consistently styled "Add" buttons across all content fragments.
 * Uses brand purple outline pill style — matching the app's design language.
 */
public class AddButtonFactory {

    public static MaterialButton create(Context context, String label, Runnable onClick) {
        MaterialButton btn = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(label);
        btn.setTextColor(Color.parseColor("#6800FF"));
        btn.setStrokeColorResource(R.color.brand_purple);
        btn.setStrokeWidth(4);                 // 4px visible border
        btn.setCornerRadius(60);               // full pill
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setIconTintResource(R.color.brand_purple);
        btn.setRippleColorResource(R.color.brand_purple_alpha15);

        // Spacing
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 16;
        lp.bottomMargin = 8;
        lp.leftMargin = 20;
        lp.rightMargin = 20;
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }
}
