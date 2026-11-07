package app.template.extension.telegram;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Builds a Telegram-style Morphe card in the Settings screen. */
public final class MorpheSettingsEntry {
    private static final int ACCENT = 0xff079bc9;
    private MorpheSettingsEntry() { }

    /** Kept for compatibility with the existing Telegram Settings injection hook. */
    public static View createSettingsRow(Context context) {
        if (context == null) throw new IllegalStateException("Telegram application context is not ready");
        final Context appContext = context;

        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        outer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 22), dp(context, 18), dp(context, 18), dp(context, 18));
        card.setBackground(cardBackground(context));
        card.setElevation(dp(context, 1));
        outer.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("Morphe");
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        sectionTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        sectionTitle.setTextColor(ACCENT);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headingParams.bottomMargin = dp(context, 14);
        card.addView(sectionTitle, headingParams);

        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, dp(context, 4), 0, dp(context, 4));
        item.setBackground(selectableBackground(context));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription("Morphe Settings");
        card.addView(item, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        MorpheLogoView logo = new MorpheLogoView(context);
        item.addView(logo, new LinearLayout.LayoutParams(dp(context, 56), dp(context, 56)));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.leftMargin = dp(context, 18);
        item.addView(labels, labelsParams);

        TextView title = new TextView(context);
        title.setText("Morphe Settings");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setTextColor(themeColor(context, android.R.attr.textColorPrimary, 0xff202124));
        labels.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText("Automatic forwarding and custom options");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setTextColor(themeColor(context, android.R.attr.textColorSecondary, 0xff777777));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(context, 3);
        labels.addView(subtitle, subtitleParams);

        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        chevron.setTextColor(themeColor(context, android.R.attr.textColorSecondary, 0xff777777));
        chevron.setGravity(Gravity.CENTER);
        item.addView(chevron, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 48)));

        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    Intent intent = new Intent(appContext, MorpheSettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(intent);
                } catch (android.content.ActivityNotFoundException missingActivity) {
                    Toast.makeText(appContext,
                            "Morphe Settings activity is not registered in this build",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        return outer;
    }

    /** Filled Telegram-style app tile with a white, custom-drawn puzzle mark. */
    private static final class MorpheLogoView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF tile = new RectF();
        private final Path mark = new Path();
        MorpheLogoView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float d = getResources().getDisplayMetrics().density;
            float side = Math.min(getWidth(), getHeight());
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            tile.set(left, top, left + side, top + side);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ACCENT);
            canvas.drawRoundRect(tile, 14f * d, 14f * d, paint);

            // A clean white puzzle-piece silhouette, matching Telegram's filled help icons.
            float s = side / 56f;
            canvas.save();
            canvas.translate(left + 10f * s, top + 9f * s);
            canvas.scale(s, s);
            mark.reset();
            mark.moveTo(7, 3); mark.lineTo(17, 3);
            mark.cubicTo(16, 10, 27, 11, 27, 4);
            mark.lineTo(37, 4); mark.lineTo(37, 14);
            mark.cubicTo(30, 13, 29, 24, 37, 24);
            mark.lineTo(37, 35); mark.lineTo(26, 35);
            mark.cubicTo(27, 28, 16, 27, 16, 35);
            mark.lineTo(6, 35); mark.lineTo(6, 24);
            mark.cubicTo(13, 25, 14, 14, 6, 14);
            mark.close();
            paint.setColor(Color.WHITE);
            canvas.drawPath(mark, paint);
            canvas.restore();
        }
    }

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
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
                return value.data;
        }
        return fallback;
    }
    private static android.graphics.drawable.Drawable cardBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(themeColor(context, android.R.attr.colorBackground, Color.WHITE));
        shape.setCornerRadius(dp(context, 24));
        shape.setStroke(dp(context, 1), themeColor(context, android.R.attr.textColorHint, 0x14000000));
        return shape;
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
