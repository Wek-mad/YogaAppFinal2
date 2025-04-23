package com.universalyoga.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditClassInstanceActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Course currentCourse;
    private ClassInstance currentInstance;
    private boolean isEditMode = false;

    // UI elements
    private TextView tvTitle;
    private TextView tvCourseInfo;
    private TextView tvDayWarning;
    private EditText etDate;
    private Button btnSelectDate;
    private EditText etTeacher;
    private EditText etAdditionalComments;
    private TextView tvAvailableSpots;
    private Button btnSave;
    private Button btnCancel;

    // The selected date
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_class_instance);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Initialize UI elements
        initializeUI();

        // Get course ID from intent
        int courseId = getIntent().getIntExtra("course_id", -1);
        if (courseId == -1) {
            Toast.makeText(this, "Error: No course selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load course data
        currentCourse = dbHelper.getCourse(courseId);
        if (currentCourse == null) {
            Toast.makeText(this, "Error: Course not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d("check", currentCourse.getDayOfWeek());

        // Display course information
        tvCourseInfo.setText(String.format("%s - %s at %s\nCapacity: %d, Duration: %d minutes",
                currentCourse.getType(),
                currentCourse.getDayOfWeek(),
                currentCourse.getTime(),
                currentCourse.getCapacity(),
                currentCourse.getDuration()));

        // Check if we're in edit mode
        int instanceId = getIntent().getIntExtra("instance_id", -1);
        if (instanceId != -1) {
            isEditMode = true;
            tvTitle.setText("Edit Class Instance");
            loadInstanceData(instanceId);
        } else {
            isEditMode = false;
            tvTitle.setText("Add Class Instance");

            // Set available spots to course capacity automatically
            tvAvailableSpots.setText(String.valueOf(currentCourse.getCapacity()));

            // Find the next date that matches the course day of week
            findNextMatchingDate();
        }

        // Set up button click listeners
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    saveClassInstance();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initializeUI() {
        tvTitle = findViewById(R.id.tvTitle);
        tvCourseInfo = findViewById(R.id.tvCourseInfo);
        tvDayWarning = findViewById(R.id.tvDayWarning);
        etDate = findViewById(R.id.etDate);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        etTeacher = findViewById(R.id.etTeacher);
        etAdditionalComments = findViewById(R.id.etAdditionalComments);
        tvAvailableSpots = findViewById(R.id.tvAvailableSpots);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Set up date picker
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
    }

    private void loadInstanceData(int instanceId) {
        currentInstance = dbHelper.getClassInstance(instanceId);
        if (currentInstance == null) {
            Toast.makeText(this, "Error loading class instance data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set UI elements with instance data
        selectedDate.setTime(currentInstance.getDate());
        updateDateDisplay();

        etTeacher.setText(currentInstance.getTeacher());
        etAdditionalComments.setText(currentInstance.getAdditionalComments());
        tvAvailableSpots.setText(String.valueOf(currentInstance.getAvailableSpots()));

        // Check if day matches course day
        checkDayMatch();
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }


    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));

        // Check if day matches course day
        checkDayMatch();
    }

    private void checkDayMatch() {
        String selectedDay = getDayOfWeek(selectedDate);
        String[] allowedDays = currentCourse.getDayOfWeek().split(",");

        boolean match = false;
        for (String day : allowedDays) {
            if (day.trim().equalsIgnoreCase(selectedDay)) {
                match = true;
                break;
            }
        }

        if (!match) {
            tvDayWarning.setVisibility(View.VISIBLE);
        } else {
            tvDayWarning.setVisibility(View.GONE);
        }
    }

    private String getDayOfWeek(Calendar calendar) {
        String[] days = new String[] {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };
        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return days[dayIndex];
    }

    private void findNextMatchingDate() {
        String[] allowedDays = currentCourse.getDayOfWeek().split(",");
        int today = selectedDate.get(Calendar.DAY_OF_WEEK);
        int minDaysToAdd = Integer.MAX_VALUE;

        for (String day : allowedDays) {
            int targetDay = getDayIndex(day.trim());
            if (targetDay == -1) continue;

            int daysToAdd = (targetDay - today + 7) % 7;
            if (daysToAdd == 0) daysToAdd = 7; // go to next week if today matches

            minDaysToAdd = Math.min(minDaysToAdd, daysToAdd);
        }

        selectedDate.add(Calendar.DAY_OF_MONTH, minDaysToAdd);
        updateDateDisplay();
    }

    private int getDayIndex(String day) {
        String[] days = new String[]{
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };
        for (int i = 0; i < days.length; i++) {
            if (days[i].equalsIgnoreCase(day)) {
                return i + 1; // Calendar.DAY_OF_WEEK is 1-based
            }
        }
        return -1;
    }


    private boolean validateInput() {
        if (etDate.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }

        String teacher = etTeacher.getText().toString().trim();
        if (teacher.isEmpty()) {
            etTeacher.setError("Teacher name is required");
            return false;
        }

        String selectedDay = getDayOfWeek(selectedDate);
        String[] allowedDays = currentCourse.getDayOfWeek().split(",");

        boolean match = false;
        for (String day : allowedDays) {
            if (day.trim().equalsIgnoreCase(selectedDay)) {
                match = true;
                break;
            }
        }

        if (!match) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Day Mismatch")
                    .setMessage("The selected date (" + selectedDay + ") doesn't match any of the allowed days: " +
                            currentCourse.getDayOfWeek() + ". Do you want to continue anyway?")
                    .setPositiveButton("Continue", (dialog, which) -> showConfirmationDialog())
                    .setNegativeButton("Cancel", null)
                    .show();
            return false;
        }

        return true;
    }

    private void saveClassInstance() {
        Log.d("ConfirmDialog", "saveClassInstance called");
        if (validateInput()) {
            Log.d("ConfirmDialog", "validation passed, showing confirmation dialog");
            showConfirmationDialog();
        } else {
            Log.d("ConfirmDialog", "validation failed");
        }
    }

    private void showConfirmationDialog() {
        Log.d("ConfirmDialog", "showConfirmationDialog method started");

        try {
            // Format date for display
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
            String formattedDate = sdf.format(selectedDate.getTime());

            // Get teacher name and comments
            String teacher = etTeacher.getText().toString().trim();
            String additionalComments = etAdditionalComments.getText().toString().trim();
            if (additionalComments.isEmpty()) {
                additionalComments = "None";
            }

            Log.d("ConfirmDialog", "Building confirmation message");
            // Build message with all class details
            StringBuilder message = new StringBuilder();
            message.append("Please confirm the following class details:\n\n");
            message.append("Course: ").append(currentCourse.getType()).append("\n");
            message.append("Date: ").append(formattedDate).append("\n");
            message.append("Time: ").append(currentCourse.getTime()).append("\n");
            message.append("Duration: ").append(currentCourse.getDuration()).append(" minutes\n");
            message.append("Teacher: ").append(teacher).append("\n");
            message.append("Available Spots: ").append(tvAvailableSpots.getText().toString()).append("\n");
            message.append("Additional Comments: ").append(additionalComments).append("\n");

            // Check if day matches course day and add warning if not
            String selectedDay = getDayOfWeek(selectedDate);
            String[] allowedDays = currentCourse.getDayOfWeek().split(",");
            boolean match = false;
            for (String day : allowedDays) {
                if (day.trim().equalsIgnoreCase(selectedDay)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                message.append("\nWARNING: The selected date (").append(selectedDay)
                        .append(") doesn't match the course's regular day(s): ")
                        .append(currentCourse.getDayOfWeek()).append("\n");
            }

            Log.d("ConfirmDialog", "Creating and showing dialog");
            // Create and show confirmation dialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle(isEditMode ? "Update Class Instance" : "Add Class Instance")
                    .setMessage(message.toString())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        Log.d("ConfirmDialog", "Confirm button clicked");
                        actualSaveClassInstance();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d("ConfirmDialog", "Cancel button clicked");
                    })
                    .show();

            Log.d("ConfirmDialog", "Dialog should be displayed now");
        } catch (Exception e) {
            Log.e("ConfirmDialog", "Error showing confirmation dialog", e);
            // Fall back to direct save if there's an exception
            Toast.makeText(this, "Error showing confirmation. Saving directly.", Toast.LENGTH_SHORT).show();
            actualSaveClassInstance();
        }
    }

    private void actualSaveClassInstance() {
        Date date = selectedDate.getTime();
        String teacher = etTeacher.getText().toString().trim();
        String additionalComments = etAdditionalComments.getText().toString().trim();
        int availableSpots = Integer.parseInt(tvAvailableSpots.getText().toString());

        FirestoreSyncManager syncManager = new FirestoreSyncManager();

        if (isEditMode && currentInstance != null) {
            // Update existing instance
            currentInstance.setDate(date);
            currentInstance.setTeacher(teacher);
            currentInstance.setAdditionalComments(additionalComments);
            currentInstance.setAvailableSpots(availableSpots);

            dbHelper.updateClassInstance(currentInstance);
            syncManager.uploadClassInstance(currentInstance);
        } else {
            // Create and save new instance once
            ClassInstance newInstance = new ClassInstance(
                    currentCourse.getId(),
                    date,
                    teacher,
                    additionalComments,
                    availableSpots,
                    false
            );

            dbHelper.addClassInstance(newInstance);
            syncManager.uploadClassInstance(newInstance);
        }

        Toast.makeText(this, isEditMode ? "Class instance updated" : "Class instance added", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}