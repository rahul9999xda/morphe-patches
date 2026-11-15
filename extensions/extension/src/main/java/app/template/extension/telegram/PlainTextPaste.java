package app.template.extension.telegram;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.widget.EditText;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Plain-text-only clipboard insertion for Telegram's message composer.
 * Both the classic context-menu paste path and AndroidX receive-content path
 * are consumed when clipboard text is available. The original receive-content
 * path is retained for non-text payloads such as images and URIs.
 */
public final class PlainTextPaste {
    private static final int MENU_PASTE = 0x01020022;
    private static final int MENU_PASTE_AS_PLAIN_TEXT = 0x01020031;

    private PlainTextPaste() { }

    public static boolean handle(EditText view, int actionId) {
        if (actionId != MENU_PASTE && actionId != MENU_PASTE_AS_PLAIN_TEXT) return false;
        // A paste menu action is always consumed when text clipboard content exists.
        // This prevents AndroidX/AppCompat from subsequently inserting HTML spans.
        if (view == null) return true;
        try {
            Object service = view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (!(service instanceof ClipboardManager)) return false;
            ClipData clip = ((ClipboardManager) service).getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return false;
            String plain = clipToPlainText(view.getContext(), clip);
            if (plain == null) return false; // allow non-text/image clipboard payloads
            replaceSelection(view, plain);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Handles the R8-renamed AndroidX/AppCompat receive-content wrappers used
     * by Telegram 13.2.2 and Plus 13.2.1.0. Android/Web use Lr0/g; -> Lr0/f;
     * -> a():ClipData. Plus uses Ls0/d; -> Ls0/d$f; -> c():ClipData. Calling
     * the wrapper interface avoids relying on concrete implementation visibility.
     */

    public static boolean handleReceiveContent(EditText view, Object payload) {
        if (view == null || payload == null) return false;
        try {
            Field field;
            try {
                field = payload.getClass().getField("a");
            } catch (NoSuchFieldException missingPublicField) {
                field = payload.getClass().getDeclaredField("a");
                field.setAccessible(true);
            }
            Object content = field.get(payload);
            if (content == null) return false;

            Object rawClip;
            try {
                Class<?> payloadInterface = Class.forName("r0.f");
                Method accessor = payloadInterface.getMethod("a");
                accessor.setAccessible(true);
                rawClip = accessor.invoke(content);
            } catch (Throwable androidXCompatLookupFailed) {
                // Plus Messenger's AppCompat receive-content wrapper is Ls0/d;.
                // Its private `a` field implements Ls0/d$f; whose c() returns ClipData.
                Class<?> payloadInterface = Class.forName("s0.d$f");
                Method accessor = payloadInterface.getMethod("c");
                accessor.setAccessible(true);
                rawClip = accessor.invoke(content);
            }
            if (!(rawClip instanceof ClipData)) return false;
            ClipData clip = (ClipData) rawClip;
            if (clip.getItemCount() == 0) return false;
            String plain = clipToPlainText(view.getContext(), clip);
            if (plain == null) return false; // preserve original handling for non-text ClipData
            replaceSelection(view, plain);
            return true;
        } catch (Throwable ignored) {
            // Reflection can fail on a different AndroidX mapping. Do not consume
            // an unknown payload; the host's original receiver must handle it.
            return false;
        }
    }

    private static String clipToPlainText(Context context, ClipData clip) {
        StringBuilder result = new StringBuilder();
        boolean hasText = false;
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            CharSequence text = item.getText();
            if (text == null && item.getHtmlText() != null) {
                text = Html.fromHtml(item.getHtmlText(), Html.FROM_HTML_MODE_LEGACY);
            }
            if (text == null) {
                try { text = item.coerceToText(context); }
                catch (Throwable ignored) { }
            }
            if (text == null) continue;
            if (hasText) result.append('\n');
            // String append strips every Spanned style/formatting span.
            result.append(text.toString());
            hasText = true;
        }
        return hasText ? result.toString() : null;
    }

    private static void replaceSelection(EditText view, String text) {
        Editable editable = view.getText();
        if (editable == null) return;
        int length = editable.length();
        int start = Selection.getSelectionStart(editable);
        int end = Selection.getSelectionEnd(editable);
        if (start < 0 || end < 0) start = end = length;
        start = Math.max(0, Math.min(start, length));
        end = Math.max(0, Math.min(end, length));
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        editable.replace(start, end, text);
    }
}
