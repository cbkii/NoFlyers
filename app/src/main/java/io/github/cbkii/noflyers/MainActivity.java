package io.github.cbkii.noflyers;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public final class MainActivity extends Activity {
    private static final String PICSART_PACKAGE = "com.picsart.studio";

    private ConfigStore store;
    private LinearLayout overridesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ConfigStore(this);
        setTitle(R.string.app_name);
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = verticalLayout();
        int pagePadding = dp(20);
        root.setPadding(pagePadding, pagePadding, pagePadding, pagePadding);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text(getString(R.string.app_name), 26f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView summary = text(getString(R.string.module_description), 16f);
        summary.setPadding(0, dp(8), 0, dp(16));
        root.addView(summary);

        root.addView(sectionTitle("Required Vector / LSPosed setup"));
        root.addView(body(
                "Enable this module in Vector/LSPosed, then select only the apps where "
                        + "AppsFlyer should be guarded. PicsArt is the recommended initial scope. "
                        + "Do not select Android, Google Play services or the Play Store."));

        root.addView(sectionTitle("Default mode"));
        HookMode[] defaults = HookMode.selectableDefaults();
        Spinner defaultSpinner = modeSpinner(defaults);
        defaultSpinner.setSelection(indexOf(defaults, store.getDefaultMode()));
        defaultSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            HookMode selected = defaults[position];
            if (selected != store.getDefaultMode()) {
                store.setDefaultMode(selected);
                showRestartNotice();
            }
        }));
        root.addView(defaultSpinner, fullWidthWrap());

        TextView modeHelp = body(
                "Compatibility is safest. ‘Fail App Set calls’ also forces the Google App Set API "
                        + "onto its failure path. Full block prevents AppsFlyer startup and events, "
                        + "which may disable marketing attribution or deferred deep links.");
        modeHelp.setPadding(0, dp(6), 0, dp(12));
        root.addView(modeHelp);

        Switch diagnostics = new Switch(this);
        diagnostics.setText("Diagnostic Xposed logs");
        diagnostics.setChecked(store.diagnosticsEnabled());
        diagnostics.setOnCheckedChangeListener((buttonView, enabled) -> {
            store.setDiagnosticsEnabled(enabled);
            showRestartNotice();
        });
        root.addView(diagnostics, fullWidthWrap());

        Button picsArtPreset = new Button(this);
        picsArtPreset.setText("Add PicsArt preset");
        picsArtPreset.setOnClickListener(view -> {
            store.setOverride(PICSART_PACKAGE, HookMode.FAIL_APPSET);
            rebuildOverrides();
            Toast.makeText(
                    this,
                    "PicsArt set to Compatibility + fail App Set calls",
                    Toast.LENGTH_LONG).show();
        });
        root.addView(picsArtPreset, fullWidthWrap());

        root.addView(sectionTitle("Per-app overrides"));
        root.addView(body(
                "Overrides are matched by exact Android package name. Vector/LSPosed scope remains "
                        + "the primary safety boundary; an override does not add an app to scope."));

        Button addOverride = new Button(this);
        addOverride.setText("Add app override");
        addOverride.setOnClickListener(view -> showOverrideDialog(null, HookMode.COMPAT));
        root.addView(addOverride, fullWidthWrap());

        overridesContainer = verticalLayout();
        overridesContainer.setPadding(0, dp(8), 0, dp(20));
        root.addView(overridesContainer, fullWidthWrap());
        rebuildOverrides();

        root.addView(sectionTitle("Applying changes"));
        root.addView(body(
                "After changing a mode or Vector/LSPosed scope, force-stop and reopen each target "
                        + "app. Reboot only when the module was newly enabled or Vector does not "
                        + "inject it after an app restart."));
        return scrollView;
    }

    private void rebuildOverrides() {
        if (overridesContainer == null) {
            return;
        }
        overridesContainer.removeAllViews();
        Map<String, HookMode> overrides = store.getOverrides();
        if (overrides.isEmpty()) {
            TextView empty = body("No per-app overrides configured.");
            empty.setPadding(0, dp(6), 0, dp(6));
            overridesContainer.addView(empty);
            return;
        }

        for (Map.Entry<String, HookMode> entry : overrides.entrySet()) {
            String packageName = entry.getKey();
            HookMode mode = entry.getValue();

            LinearLayout card = verticalLayout();
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams cardParams = fullWidthWrap();
            cardParams.setMargins(0, dp(5), 0, dp(5));

            TextView packageView = text(packageName, 16f);
            packageView.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(packageView);
            card.addView(body(mode.label() + " — " + mode.description()));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END);

            Button edit = new Button(this);
            edit.setText("Edit");
            edit.setOnClickListener(view -> showOverrideDialog(packageName, mode));
            actions.addView(edit);

            Button remove = new Button(this);
            remove.setText("Remove");
            remove.setOnClickListener(view -> {
                store.removeOverride(packageName);
                rebuildOverrides();
                showRestartNotice();
            });
            actions.addView(remove);

            card.addView(actions, fullWidthWrap());
            card.setBackgroundResource(R.drawable.override_card);
            overridesContainer.addView(card, cardParams);
        }
    }

    private void showOverrideDialog(String existingPackage, HookMode currentMode) {
        LinearLayout content = verticalLayout();
        int padding = dp(18);
        content.setPadding(padding, dp(8), padding, 0);

        EditText packageInput = new EditText(this);
        packageInput.setHint("com.example.app");
        packageInput.setSingleLine(true);
        packageInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (existingPackage != null) {
            packageInput.setText(existingPackage);
            packageInput.setEnabled(false);
        }
        content.addView(packageInput, fullWidthWrap());

        HookMode[] modes = HookMode.selectableOverrides();
        Spinner modeSpinner = modeSpinner(modes);
        modeSpinner.setSelection(indexOf(modes, currentMode));
        content.addView(modeSpinner, fullWidthWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existingPackage == null ? "Add app override" : "Edit app override")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String packageName = packageInput.getText().toString().trim();
                    if (!PackageNameValidator.isValid(packageName)) {
                        packageInput.setError("Enter an exact Android package name");
                        return;
                    }
                    HookMode selected = modes[modeSpinner.getSelectedItemPosition()];
                    store.setOverride(packageName, selected);
                    dialog.dismiss();
                    rebuildOverrides();
                    showRestartNotice();
                }));
        dialog.show();
    }

    private Spinner modeSpinner(HookMode[] modes) {
        String[] labels = new String[modes.length];
        for (int index = 0; index < modes.length; index++) {
            labels[index] = modes[index].label();
        }
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private int indexOf(HookMode[] modes, HookMode target) {
        for (int index = 0; index < modes.length; index++) {
            if (modes[index] == target) {
                return index;
            }
        }
        return 0;
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 19f);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(18), 0, dp(6));
        return view;
    }

    private TextView body(String value) {
        TextView view = text(value, 15f);
        view.setLineSpacing(0f, 1.15f);
        return view;
    }

    private TextView text(String value, float sizeSp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        return view;
    }

    private LinearLayout verticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams fullWidthWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showRestartNotice() {
        Toast.makeText(this, "Force-stop and reopen scoped apps to apply", Toast.LENGTH_SHORT).show();
    }

    private interface PositionConsumer {
        void accept(int position);
    }

    private static final class SimpleItemSelectedListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        private final PositionConsumer consumer;

        SimpleItemSelectedListener(PositionConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onItemSelected(
                android.widget.AdapterView<?> parent,
                View view,
                int position,
                long id) {
            consumer.accept(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
