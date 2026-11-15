package app.template.extension.telegram;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.Html;
import android.text.Spanned;
import android.widget.EditText;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Plain-text clipboard insertion used by the Disable Rich HTML Paste patch.
 * It deliberately converts each clipboard item with coerceToText() and then
 * inserts a String, so HTML spans/styles cannot enter Telegram's composer.
 */
public final class PlainTextPaste {
    private static final int MENU_PASTE = 0x01020022;
    private static final int MENU_PASTE_AS_PLAIN_TEXT = 0x01020031;

    private PlainTextPaste() { }


    /**
     * Handles AndroidX receive-content deliveries that bypass
     * EditText.onTextContextMenuItem(). If the ClipData contains HTML/rich text,
     * insert its plain-text representation and consume the payload. Other
     * content (images, URIs, ordinary text) is left to Telegram's original
     * receive-content pipeline.
     *
     * The target AndroidX ContentInfoCompat wrapper is R8-renamed in Telegram;
     * its public payload field and ClipData accessor are resolved reflectively
     * so this extension does not need a compile-time dependency on that class.
     */
    public static boolean handleReceiveContent(EditText view, Object payload) {
        if (view == null || payload == null) return false;
        try {
            Field payloadField = payload.getClass().getField("a");
            Object content = payloadField.get(payload);
            if (content == null) return false;
            Method clipAccessor = content.getClass().getMethod("a");
            Object rawClip = clipAccessor.invoke(content);
            if (!(rawClip instanceof ClipData)) return false;
            ClipData clip = (ClipData) rawClip;
            if (clip.getItemCount() == 0) return false;

            StringBuilder plain = new StringBuilder();
            boolean hasRichText = false;
            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                CharSequence itemText = item.getText();
                String html = item.getHtmlText();
                if (html != null || (itemText instanceof Spanned && hasSpans((Spanned) itemText))) {
                    hasRichText = true;
                }
                if (itemText == null && html != null) {
                    itemText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
                }
                if (itemText == null) {
                    try { itemText = item.coerceToText(view.getContext()); }
                    catch (Throwable ignored) { }
                }
                if (itemText == null) continue;
                if (plain.length() > 0) plain.append('\n');
                plain.append(itemText.toString());
            }
            if (!hasRichText || plain.length() == 0) return false;

            Editable editable = view.getText();
            if (editable == null) return false;
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
            // Fall back to Telegram's original receive-content implementation.
            return false;
        }
    }

    private static boolean hasSpans(Spanned text) {
        return text.length() > 0 && text.getSpans(0, text.length(), Object.class).length > 0;
    }

    public static boolean handle(EditText view, int actionId) {
        if (actionId != MENU_PASTE && actionId != MENU_PASTE_AS_PLAIN_TEXT) return false;
        if (view == null) return false;
        try {
            Context context = view.getContext();
            Object service = context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (!(service instanceof ClipboardManager)) return false;
            ClipData clip = ((ClipboardManager) service).getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return false;

            StringBuilder plain = new StringBuilder();
            boolean hasRichText = false;
            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                CharSequence itemText = item.getText();
                String html = item.getHtmlText();
                if (html != null || (itemText instanceof Spanned && hasSpans((Spanned) itemText))) {
                    hasRichText = true;
                }
                if (itemText == null && html != null) {
                    itemText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
                }
                if (itemText == null) {
                    try { itemText = item.coerceToText(context); }
                    catch (Throwable ignored) { }
                }
                if (itemText == null) continue;
                if (plain.length() > 0) plain.append('\n');
                // String conversion deliberately strips HTML and all style spans.
                plain.append(itemText.toString());
            }
            // Leave ordinary text, image and URI paste to Telegram's normal path.
            if (!hasRichText) return false;
            // Consume an HTML paste even if conversion yields no text; otherwise the
            // original receive-content path could reinsert the rich HTML payload.
            if (plain.length() == 0) return true;

            Editable editable = view.getText();
            if (editable == null) return true;
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
            // Preserve the host fallback if clipboard access itself fails.
            return false;
        }
    }
}
