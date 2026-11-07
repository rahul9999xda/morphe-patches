package app.template.extension.telegram;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Builds the Morphe row inserted into Telegram's existing Settings list. */
public final class MorpheSettingsEntry {
    private MorpheSettingsEntry() { }

    public static View createSettingsRow(Context context) {
        if (context == null) throw new IllegalStateException("Telegram application context is not ready");
        final Context appContext = context;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 18), dp(context, 9), dp(context, 18), dp(context, 9));
        row.setMinimumHeight(dp(context, 72));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 72)));
        row.setBackground(selectableBackground(context));
        row.setContentDescription("Morphe Settings");

        TextView logo = new TextView(context);
        logo.setText("🧩");
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBackground = new GradientDrawable();
        logoBackground.setColor(Color.rgb(105, 79, 210));
        logoBackground.setCornerRadius(dp(context, 12));
        logo.setBackground(logoBackground);
        row.addView(logo, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.leftMargin = dp(context, 14);
        row.addView(labels, labelsParams);

        TextView title = new TextView(context);
        title.setText("Morphe Settings");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setTextColor(themeColor(context, android.R.attr.textColorPrimary, Color.BLACK));
        labels.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText("Automatic forwarding and custom options");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTextColor(themeColor(context, android.R.attr.textColorSecondary, 0xff777777));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(context, 2);
        labels.addView(subtitle, subtitleParams);

        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        chevron.setTextColor(themeColor(context, android.R.attr.textColorSecondary, 0xff777777));
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 44)));

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    Intent intent = new Intent(appContext, MorpheSettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(intent);
                } catch (Throwable ignored) { }
            }
        });
        return row;
    }

    public static int getRowHeightPx(Context context) { return dp(context, 72); }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static int themeColor(Context context, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) {
                try { return context.getResources().getColor(value.resourceId); }
                catch (Throwable ignored) { }
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        }
        return fallback;
    }

    private static android.graphics.drawable.Drawable selectableBackground(Context context) {
        TypedArray array = context.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        try {
            int resource = array.getResourceId(0, 0);
            return resource != 0 ? context.getDrawable(resource) : new GradientDrawable();
        } catch (Throwable ignored) {
            return new GradientDrawable();
        } finally {
            array.recycle();
        }
    }
}
