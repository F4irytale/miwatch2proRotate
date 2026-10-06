package com.czsmart.rotate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int[] DEGREES = {0, 90, 180, 270};
    private static final int COLOR_IDLE = 0xFF333333;
    private static final int COLOR_SELECTED = 0xFF2B7DE9;

    private final Button[] buttons = new Button[DEGREES.length];
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        // 圆形屏幕，上下左右留足边距
        int padH = dp(36), padV = dp(28);
        root.setPadding(padH, padV, padH, dp(48));

        TextView title = new TextView(this);
        title.setText("屏幕旋转");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        status.setTextColor(0xFFBBBBBB);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(4), 0, dp(8));
        root.addView(status);

        for (int i = 0; i < DEGREES.length; i++) {
            final int rotation = i;
            Button b = new Button(this);
            b.setText(DEGREES[i] + "°");
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setOnClickListener(v -> onPick(rotation));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            lp.topMargin = dp(6);
            root.addView(b, lp);
            buttons[i] = b;
        }

        CheckBox boot = new CheckBox(this);
        boot.setText("开机自动恢复");
        boot.setTextColor(Color.WHITE);
        boot.setChecked(Rotation.applyOnBoot(this));
        boot.setOnCheckedChangeListener((v, checked) -> Rotation.setApplyOnBoot(this, checked));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(10);
        root.addView(boot, blp);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(root);
        setContentView(scroll);
        scroll.requestFocus(); // 让表冠可以滚动
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void onPick(int rotation) {
        if (!Rotation.canWrite(this)) {
            requestPermission();
            return;
        }
        Rotation.save(this, rotation);
        if (!Rotation.apply(this, rotation)) {
            status.setText("设置失败");
            return;
        }
        refresh();
    }

    private void requestPermission() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (RuntimeException e) {
            // 手表上通常没有这个授权界面，只能用 adb 授权
            status.setText("需要授权，请用 adb 执行：\nappops set " + getPackageName() + " WRITE_SETTINGS allow");
        }
    }

    private void refresh() {
        if (!Rotation.canWrite(this)) {
            status.setText("未授权，点任意按钮查看方法");
        } else {
            status.setText("当前：" + DEGREES[Rotation.current(this) & 3] + "°");
        }
        int cur = Rotation.canWrite(this) ? Rotation.current(this) & 3 : -1;
        for (int i = 0; i < buttons.length; i++) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(22));
            bg.setColor(i == cur ? COLOR_SELECTED : COLOR_IDLE);
            buttons[i].setBackground(bg);
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
