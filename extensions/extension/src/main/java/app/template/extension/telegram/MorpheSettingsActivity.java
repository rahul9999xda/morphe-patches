package app.template.extension.telegram;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import app.template.extension.forwarding.ForwardingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Dedicated in-app Morphe settings screen, opened from Telegram's Settings list. */
public final class MorpheSettingsActivity extends Activity {
    private SharedPreferencesForwardingRuleStore store;
    private LinearLayout content;
    private LinearLayout rulesContainer;
    private Switch enabledSwitch;
    private EditText nameInput;
    private EditText sourceInput;
    private EditText destinationInput;
    private EditText includeInput;
    private EditText excludeInput;
    private Spinner matchSpinner;
    private Spinner deliverySpinner;
    private Switch caseSensitiveSwitch;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new SharedPreferencesForwardingRuleStore(this);
        buildScreen();
        refreshRules();
    }

    private void buildScreen() {
        final boolean dark = (getResources().getConfiguration().uiMode & 0x30) == 0x20;
        final int background = dark ? 0xff171717 : 0xfff7f7fb;
        final int surface = dark ? 0xff242424 : Color.WHITE;
        final int primary = dark ? 0xfff3f3f3 : 0xff202124;
        final int secondary = dark ? 0xffb8b8b8 : 0xff666a73;
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(28));
        content.setBackgroundColor(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Morphe Settings");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(primary);
        content.addView(title, params(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Automatic message forwarding using your existing Telegram account.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondary);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(6);
        content.addView(subtitle, subtitleParams);

        LinearLayout masterCard = card(surface);
        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Enable automatic forwarding");
        enabledSwitch.setTextSize(16);
        enabledSwitch.setTextColor(primary);
        enabledSwitch.setChecked(store.isEnabled());
        masterCard.addView(enabledSwitch, params(-1, -2));
        enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
            store.setEnabled(checked);
            Toast.makeText(this, checked ? "Automatic forwarding enabled" : "Automatic forwarding paused", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams masterParams = params(-1, -2);
        masterParams.topMargin = dp(18);
        content.addView(masterCard, masterParams);

        addSectionTitle("Create forwarding rule", primary);
        LinearLayout form = card(surface);
        nameInput = addInput(form, "Rule name", "e.g. News forwarding");
        sourceInput = addInput(form, "Source chat IDs", "Comma-separated IDs, e.g. -1001234567890, 123456789");
        destinationInput = addInput(form, "Destination chat IDs", "Comma-separated IDs");
        includeInput = addInput(form, "Include keywords (optional)", "Comma-separated words or phrases");
        excludeInput = addInput(form, "Exclude keywords (optional)", "Comma-separated words or phrases");

        TextView matchLabel = label("Keyword matching", secondary);
        form.addView(matchLabel, params(-1, -2));
        matchSpinner = spinner(new String[]{"Match any included keyword", "Match all included keywords"});
        form.addView(matchSpinner, params(-1, dp(48)));

        TextView deliveryLabel = label("Delivery mode", secondary);
        LinearLayout.LayoutParams deliveryLabelParams = params(-1, -2);
        deliveryLabelParams.topMargin = dp(12);
        form.addView(deliveryLabel, deliveryLabelParams);
        deliverySpinner = spinner(new String[]{"Forward (preserve attribution)", "Copy (send as a new message)"});
        form.addView(deliverySpinner, params(-1, dp(48)));

        caseSensitiveSwitch = new Switch(this);
        caseSensitiveSwitch.setText("Case-sensitive keyword matching");
        caseSensitiveSwitch.setTextColor(primary);
        caseSensitiveSwitch.setTextSize(14);
        LinearLayout.LayoutParams caseParams = params(-1, -2);
        caseParams.topMargin = dp(10);
        form.addView(caseSensitiveSwitch, caseParams);

        Button save = button("Save forwarding rule");
        LinearLayout.LayoutParams saveParams = params(-1, dp(48));
        saveParams.topMargin = dp(14);
        form.addView(save, saveParams);
        save.setOnClickListener(v -> saveRule());

        LinearLayout.LayoutParams formParams = params(-1, -2);
        formParams.topMargin = dp(10);
        content.addView(form, formParams);

        addSectionTitle("Your rules", primary);
        rulesContainer = new LinearLayout(this);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rulesParams = params(-1, -2);
        rulesParams.topMargin = dp(8);
        content.addView(rulesContainer, rulesParams);

        TextView note = new TextView(this);
        note.setText("Chat IDs are Telegram dialog IDs. User chats are usually positive; groups and channels are usually negative. Secret chats and protected/no-forward messages are excluded.");
        note.setTextSize(12);
        note.setTextColor(secondary);
        LinearLayout.LayoutParams noteParams = params(-1, -2);
        noteParams.topMargin = dp(18);
        content.addView(note, noteParams);
    }

    private void saveRule() {
        try {
            String name = text(nameInput);
            if (name.isEmpty()) name = "Forwarding rule";
            Set<Long> sources = parseIds(text(sourceInput));
            Set<Long> destinations = parseIds(text(destinationInput));
            if (sources.isEmpty() || destinations.isEmpty()) {
                Toast.makeText(this, "Enter at least one source and one destination chat ID", Toast.LENGTH_LONG).show();
                return;
            }
            List<ForwardingRule> rules = new ArrayList<>(store.loadAllRules());
            ForwardingRule rule = new ForwardingRule(UUID.randomUUID().toString(), name, true,
                    sources, destinations, parseWords(text(includeInput)), parseWords(text(excludeInput)),
                    matchSpinner.getSelectedItemPosition() == 1 ? ForwardingRule.MatchMode.ALL : ForwardingRule.MatchMode.ANY,
                    caseSensitiveSwitch.isChecked(), deliverySpinner.getSelectedItemPosition() == 1
                            ? ForwardingRule.DeliveryMode.COPY : ForwardingRule.DeliveryMode.FORWARD);
            rules.add(rule);
            store.saveRules(rules);
            nameInput.setText(""); sourceInput.setText(""); destinationInput.setText("");
            includeInput.setText(""); excludeInput.setText("");
            refreshRules();
            Toast.makeText(this, "Forwarding rule saved", Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, "Could not save rule: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshRules() {
        if (rulesContainer == null) return;
        rulesContainer.removeAllViews();
        List<ForwardingRule> rules = store.loadAllRules();
        if (rules.isEmpty()) {
            TextView empty = label("No forwarding rules yet.", 0xff777777);
            rulesContainer.addView(empty, params(-1, -2));
            return;
        }
        for (ForwardingRule rule : rules) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackground(roundBackground(isDark() ? 0xff242424 : Color.WHITE, dp(14)));
            TextView name = label(rule.getName(), isDark() ? 0xfff2f2f2 : 0xff202124);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.addView(name, params(-1, -2));
            TextView details = label("Sources: " + rule.getSourceChatIds() + "\nDestinations: " + rule.getDestinationChatIds(), isDark() ? 0xffbdbdbd : 0xff666a73);
            details.setTextSize(12);
            LinearLayout.LayoutParams detailsParams = params(-1, -2);
            detailsParams.topMargin = dp(5);
            item.addView(details, detailsParams);
            Button remove = button("Remove rule");
            LinearLayout.LayoutParams removeParams = params(-1, dp(40));
            removeParams.topMargin = dp(8);
            item.addView(remove, removeParams);
            remove.setOnClickListener(v -> {
                List<ForwardingRule> current = new ArrayList<>(store.loadAllRules());
                ArrayList<ForwardingRule> kept = new ArrayList<>();
                for (ForwardingRule candidate : current) if (!candidate.getId().equals(rule.getId())) kept.add(candidate);
                store.saveRules(kept);
                refreshRules();
            });
            LinearLayout.LayoutParams itemParams = params(-1, -2);
            itemParams.bottomMargin = dp(8);
            rulesContainer.addView(item, itemParams);
        }
    }

    private void addSectionTitle(String text, int color) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(17);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(color);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(22);
        p.bottomMargin = dp(4);
        content.addView(heading, p);
    }

    private EditText addInput(LinearLayout parent, String label, String hint) {
        TextView heading = label(label, isDark() ? 0xffbdbdbd : 0xff666a73);
        parent.addView(heading, params(-1, -2));
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setTextSize(14);
        input.setHint(hint);
        input.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(3);
        p.bottomMargin = dp(9);
        parent.addView(input, p);
        return input;
    }

    private Spinner spinner(String[] options) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList(options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(roundBackground(color, dp(16)));
        return card;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private TextView label(String text, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(color);
        return view;
    }

    private static LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private GradientDrawable roundBackground(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private Set<Long> parseIds(String input) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) ids.add(Long.parseLong(value));
        }
        return ids;
    }

    private List<String> parseWords(String input) {
        ArrayList<String> words = new ArrayList<>();
        for (String part : input.split(",")) if (!part.trim().isEmpty()) words.add(part.trim());
        return words;
    }

    private static String text(EditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private boolean isDark() { return (getResources().getConfiguration().uiMode & 0x30) == 0x20; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
