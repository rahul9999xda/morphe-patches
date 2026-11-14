package app.template.extension.telegram;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.widget.EditText;

/**
 * Plain-text clipboard insertion for Telegram's composer. Recognized paste
 * actions are always consumed so AndroidX cannot fall through to rich HTML
 * receive-content handling when clipboard conversion fails.
 */
public final class PlainTextPaste {
    private static final int MENU_PASTE = 0x01020022;
    private static final int MENU_PASTE_AS_PLAIN_TEXT = 0x01020031;

    private PlainTextPaste() { }

    public static boolean handle(EditText view, int actionId) {
        if (actionId != MENU_PASTE && actionId != MENU_PASTE_AS_PLAIN_TEXT) {
            return false;
        }
        // A recognized paste action is consumed even on failure. Returning false
        // would invoke Telegram/AndroidX's original rich-content paste path.
        if (view == null) return true;
        try {
            Context context = view.getContext();
            Object service = context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (!(service instanceof ClipboardManager)) return true;
            ClipData clip = ((ClipboardManager) service).getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return true;

            StringBuilder plain = new StringBuilder();
            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                CharSequence text = item.getText();
                if (text == null && item.getHtmlText() != null) {
                    text = Html.fromHtml(item.getHtmlText(), Html.FROM_HTML_MODE_LEGACY);
                }
                if (text == null) {
                    try {
                        text = item.coerceToText(context);
                    } catch (Throwable ignored) {
                        // Skip an item that cannot be represented as text.
                    }
                }
                if (text == null) continue;
                if (plain.length() > 0) plain.append('\n');
                plain.append(text.toString());
            }

            Editable editable = view.getText();
            if (editable == null || plain.length() == 0) return true;
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
            editable.replace(start, end, plain.toString());
            return true;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
