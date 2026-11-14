package app.template.extension.telegram;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.widget.EditText;

/**
 * Plain-text clipboard insertion used by the Disable Rich HTML Paste patch.
 * It deliberately converts each clipboard item with coerceToText() and then
 * inserts a String, so HTML spans/styles cannot enter Telegram's composer.
 */
public final class PlainTextPaste {
    private static final int MENU_PASTE = 0x01020022;
    private static final int MENU_PASTE_AS_PLAIN_TEXT = 0x01020031;

    private PlainTextPaste() { }

    public static boolean handle(EditText view, int actionId) {
        if (view == null || (actionId != MENU_PASTE && actionId != MENU_PASTE_AS_PLAIN_TEXT)) {
            return false;
        }
        try {
            Context context = view.getContext();
            Object service = context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (!(service instanceof ClipboardManager)) return false;
            ClipData clip = ((ClipboardManager) service).getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return false;

            StringBuilder plain = new StringBuilder();
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence itemText = clip.getItemAt(i).coerceToText(context);
                if (itemText == null) continue;
                if (plain.length() > 0) plain.append('\n');
                // toString() intentionally strips any Spanned/HTML formatting.
                plain.append(itemText.toString());
            }
            if (plain.length() == 0) return false;

            Editable editable = view.getText();
            if (editable == null) return false;
            int length = editable.length();
            int start = Selection.getSelectionStart(editable);
            int end = Selection.getSelectionEnd(editable);
            if (start < 0 || end < 0) {
                start = end = length;
            }
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
            // If clipboard access is unavailable, let the host EditText handler
            // retain its normal fallback behavior.
            return false;
        }
    }
}
