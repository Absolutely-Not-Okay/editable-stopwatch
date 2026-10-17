package uk.co.editablestopwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView timeDisplay;
    private TextView statusDisplay;
    private Button startPauseButton;
    private long savedElapsed = 0L;
    private long startTime = 0L;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private android.content.SharedPreferences prefs;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateDisplay();
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(20, 24, 32));
        getWindow().setNavigationBarColor(Color.rgb(20, 24, 32));
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences("stopwatch", MODE_PRIVATE);
        savedElapsed = prefs.getLong("elapsed", 0L);
        running = prefs.getBoolean("running", false);
        startTime = prefs.getLong("startTime", 0L);

        if (running && startTime > 0L) {
            savedElapsed += Math.max(0L, System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();
        } else {
            running = false;
        }

        buildInterface();
        saveState();
        updateDisplay();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(20, 24, 32));

        TextView heading = new TextView(this);
        heading.setText("EDITABLE STOPWATCH");
        heading.setTextColor(Color.rgb(160, 190, 230));
        heading.setTextSize(16);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Elapsed time");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.topMargin = dp(10);
        root.addView(subtitle, subParams);

        timeDisplay = new TextView(this);
        timeDisplay.setText("00:00:00");
        timeDisplay.setTextColor(Color.WHITE);
        timeDisplay.setTextSize(42);
        timeDisplay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        timeDisplay.setGravity(Gravity.CENTER);
        timeDisplay.setSingleLine(true);
        LinearLayout.LayoutParams timeParams = matchWrap();
        timeParams.topMargin = dp(36);
        root.addView(timeDisplay, timeParams);

        statusDisplay = new TextView(this);
        statusDisplay.setText("PAUSED");
        statusDisplay.setTextColor(Color.rgb(170, 180, 195));
        statusDisplay.setTextSize(14);
        statusDisplay.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(10);
        root.addView(statusDisplay, statusParams);

        startPauseButton = makeButton("Start");
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = dp(42);
        root.addView(startPauseButton, startParams);
        startPauseButton.setOnClickListener(v -> toggleRunning());

        Button editButton = makeButton("Set elapsed time");
        LinearLayout.LayoutParams editParams = matchWrap();
        editParams.topMargin = dp(16);
        root.addView(editButton, editParams);
        editButton.setOnClickListener(v -> showEditDialog());

        Button resetButton = makeButton("Reset to zero");
        LinearLayout.LayoutParams resetParams = matchWrap();
        resetParams.topMargin = dp(16);
        root.addView(resetButton, resetParams);
        resetButton.setOnClickListener(v -> confirmReset());

        TextView footer = new TextView(this);
        footer.setText("Your time is saved on this device.");
        footer.setTextColor(Color.rgb(145, 155, 170));
        footer.setTextSize(13);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(34);
        root.addView(footer, footerParams);

        setContentView(root);
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.rgb(48, 91, 145))
        );
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private long elapsedNow() {
        if (running) {
            return savedElapsed + Math.max(0L, System.currentTimeMillis() - startTime);
        }
        return savedElapsed;
    }

    private void updateDisplay() {
        if (timeDisplay == null) return;

        long totalSeconds = elapsedNow() / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds / 60L) % 60L;
        long seconds = totalSeconds % 60L;

        timeDisplay.setText(String.format(
            Locale.UK, "%02d:%02d:%02d", hours, minutes, seconds
        ));

        statusDisplay.setText(running ? "RUNNING" : "PAUSED");
        statusDisplay.setTextColor(
            running ? Color.rgb(100, 220, 150) : Color.rgb(170, 180, 195)
        );
        startPauseButton.setText(running ? "Pause" : (savedElapsed == 0L ? "Start" : "Resume"));
    }

    private void toggleRunning() {
        if (running) {
            savedElapsed = elapsedNow();
            running = false;
            startTime = 0L;
        } else {
            startTime = System.currentTimeMillis();
            running = true;
        }
        saveState();
        updateDisplay();
    }

    private void showEditDialog() {
        if (running) {
            savedElapsed = elapsedNow();
            running = false;
            startTime = 0L;
            saveState();
            updateDisplay();
        }

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setPadding(dp(20), dp(12), dp(20), dp(4));

        EditText hoursField = numberField("Hours");
        EditText minutesField = numberField("Min");
        EditText secondsField = numberField("Sec");

        long seconds = savedElapsed / 1000L;
        hoursField.setText(String.valueOf(seconds / 3600L));
        minutesField.setText(String.valueOf((seconds / 60L) % 60L));
        secondsField.setText(String.valueOf(seconds % 60L));

        fields.addView(hoursField, fieldParams());
        fields.addView(minutesField, fieldParams());
        fields.addView(secondsField, fieldParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Set elapsed time")
            .setMessage("Enter the time to display.")
            .setView(fields)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set time", null)
            .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(v -> {
                try {
                    long h = parseField(hoursField, 0L);
                    long m = parseField(minutesField, 0L);
                    long s = parseField(secondsField, 0L);

                    if (m > 59L || s > 59L) {
                        Toast.makeText(this,
                            "Minutes and seconds must be between 0 and 59.",
                            Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (h > (Long.MAX_VALUE / 1000L - 3599L) / 3600L) {
                        Toast.makeText(this, "That time is too large.",
                            Toast.LENGTH_LONG).show();
                        return;
                    }

                    savedElapsed = ((h * 3600L) + (m * 60L) + s) * 1000L;
                    running = false;
                    startTime = 0L;
                    saveState();
                    updateDisplay();
                    dialog.dismiss();
                } catch (NumberFormatException ex) {
                    Toast.makeText(this, "Please enter valid whole numbers.",
                        Toast.LENGTH_LONG).show();
                }
            }));

        dialog.show();
    }

    private EditText numberField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setSelectAllOnFocus(true);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setTextSize(18);
        field.setGravity(Gravity.CENTER);
        return field;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, dp(58), 1f
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private long parseField(EditText field, long fallback) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return fallback;
        return Long.parseLong(value);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
            .setTitle("Reset stopwatch?")
            .setMessage("This will set the elapsed time to zero.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> {
                savedElapsed = 0L;
                startTime = 0L;
                running = false;
                saveState();
                updateDisplay();
            })
            .show();
    }

    private void saveState() {
        prefs.edit()
            .putLong("elapsed", savedElapsed)
            .putBoolean("running", running)
            .putLong("startTime", startTime)
            .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        saveState();
    }
          }
