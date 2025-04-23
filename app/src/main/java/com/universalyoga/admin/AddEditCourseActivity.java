package com.universalyoga.admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditCourseActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Course currentCourse;
    private boolean isEditMode = false;

    // UI elements
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private EditText etCourseName, etTime, etCapacity, etDuration, etPrice, etDescription;
    private Spinner spinnerType, spinnerDifficulty;
    private CheckBox cbEquipmentNeeded;
    private EditText etEquipmentDescription;
    private Button btnSave, btnCancel, btnSelectTime;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_course);

        // Initialize DB helper first!
        dbHelper = new DatabaseHelper(this);

        initializeUI();

        int courseId = getIntent().getIntExtra("course_id", -1);
        if (courseId != -1) {
            isEditMode = true;
            currentCourse = dbHelper.getCourse(courseId);
            if (currentCourse == null) {
                Toast.makeText(this, "Error loading course", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            tvTitle.setText("Edit Course");
            loadCourseData();
        } else {
            isEditMode = false;
            currentCourse = new Course("", "", 0, 0, 0.0, "");
            tvTitle.setText("Add New Course");
        }

        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                showConfirmationDialog();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void initializeUI() {
        tvTitle = findViewById(R.id.tvTitle);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        etCourseName = findViewById(R.id.etCourseName);
        etTime = findViewById(R.id.etTime);
        etCapacity = findViewById(R.id.etCapacity);
        etDuration = findViewById(R.id.etDuration);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);

        spinnerType = findViewById(R.id.spinnerType);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        cbEquipmentNeeded = findViewById(R.id.cbEquipmentNeeded);
        etEquipmentDescription = findViewById(R.id.etEquipmentDescription);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnSelectTime = findViewById(R.id.btnSelectTime);

        ArrayAdapter<CharSequence> typesAdapter = ArrayAdapter.createFromResource(
                this, R.array.yoga_types, android.R.layout.simple_spinner_item);
        typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typesAdapter);

        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
                this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        cbEquipmentNeeded.setOnCheckedChangeListener((btn, isChecked) -> {
            etEquipmentDescription.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) etEquipmentDescription.setText("");
        });

        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());
        etTime.setOnClickListener(v -> showTimePickerDialog());
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedTime.set(Calendar.MINUTE, minute1);
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            etTime.setText(sdf.format(selectedTime.getTime()));
        }, hour, minute, false);

        dialog.show();
    }

    private void loadCourseData() {
        etCourseName.setText(currentCourse.getName());
        etTime.setText(currentCourse.getTime());
        etCapacity.setText(String.valueOf(currentCourse.getCapacity()));
        etDuration.setText(String.valueOf(currentCourse.getDuration()));
        etPrice.setText(String.valueOf(currentCourse.getPrice()));
        etDescription.setText(currentCourse.getDescription());

        // Checkbox days
        setDayCheckbox(currentCourse.getDayOfWeek());

        // Spinners
        setSpinnerValue(spinnerType, currentCourse.getType(), R.array.yoga_types);
        setSpinnerValue(spinnerDifficulty, currentCourse.getDifficulty(), R.array.difficulty_levels);

        cbEquipmentNeeded.setChecked(currentCourse.isEquipmentNeeded());
        etEquipmentDescription.setText(currentCourse.getEquipmentDescription());
        etEquipmentDescription.setVisibility(currentCourse.isEquipmentNeeded() ? View.VISIBLE : View.GONE);
    }

    private void setDayCheckbox(String dayString) {
        String[] days = dayString.split(",");
        for (String day : days) {
            switch (day.trim()) {
                case "Monday": cbMonday.setChecked(true); break;
                case "Tuesday": cbTuesday.setChecked(true); break;
                case "Wednesday": cbWednesday.setChecked(true); break;
                case "Thursday": cbThursday.setChecked(true); break;
                case "Friday": cbFriday.setChecked(true); break;
                case "Saturday": cbSaturday.setChecked(true); break;
                case "Sunday": cbSunday.setChecked(true); break;
            }
        }
    }

    private void setSpinnerValue(Spinner spinner, String value, int arrayResId) {
        String[] array = getResources().getStringArray(arrayResId);
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private boolean validateInput() {
        if (etCourseName.getText().toString().trim().isEmpty()) {
            etCourseName.setError("Course name is required");
            return false;
        }
        if (!cbMonday.isChecked() && !cbTuesday.isChecked() && !cbWednesday.isChecked() &&
                !cbThursday.isChecked() && !cbFriday.isChecked() && !cbSaturday.isChecked() && !cbSunday.isChecked()) {
            Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etTime.getText().toString().trim().isEmpty()) {
            etTime.setError("Time is required");
            return false;
        }
        if (etCapacity.getText().toString().trim().isEmpty()) {
            etCapacity.setError("Capacity is required");
            return false;
        }
        if (etDuration.getText().toString().trim().isEmpty()) {
            etDuration.setError("Duration is required");
            return false;
        }
        if (etPrice.getText().toString().trim().isEmpty()) {
            etPrice.setError("Price is required");
            return false;
        }
        return true;
    }

    private String getSelectedDays() {
        StringBuilder builder = new StringBuilder();
        if (cbMonday.isChecked()) builder.append("Monday,");
        if (cbTuesday.isChecked()) builder.append("Tuesday,");
        if (cbWednesday.isChecked()) builder.append("Wednesday,");
        if (cbThursday.isChecked()) builder.append("Thursday,");
        if (cbFriday.isChecked()) builder.append("Friday,");
        if (cbSaturday.isChecked()) builder.append("Saturday,");
        if (cbSunday.isChecked()) builder.append("Sunday,");
        if (builder.length() > 0) builder.setLength(builder.length() - 1); // remove last comma
        return builder.toString();
    }

    private void showConfirmationDialog() {
        Log.d("CourseConfirm", "Showing confirmation dialog");

        try {
            // Get all course data from input fields
            String courseName = etCourseName.getText().toString().trim();
            String selectedDays = getSelectedDays();
            String time = etTime.getText().toString().trim();
            String capacity = etCapacity.getText().toString().trim();
            String duration = etDuration.getText().toString().trim();
            String price = etPrice.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();
            String difficulty = spinnerDifficulty.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();
            boolean equipmentNeeded = cbEquipmentNeeded.isChecked();
            String equipmentDescription = equipmentNeeded ?
                    etEquipmentDescription.getText().toString().trim() : "N/A";

            if (description.isEmpty()) {
                description = "None";
            }

            // Build confirmation message
            StringBuilder message = new StringBuilder();
            message.append("Please confirm the course details:\n\n");
            message.append("Name: ").append(courseName).append("\n");
            message.append("Type: ").append(type).append("\n");
            message.append("Day(s): ").append(selectedDays).append("\n");
            message.append("Time: ").append(time).append("\n");
            message.append("Duration: ").append(duration).append(" minutes\n");
            message.append("Capacity: ").append(capacity).append(" students\n");
            message.append("Price: £").append(price).append("\n");
            message.append("Difficulty: ").append(difficulty).append("\n");

            if (equipmentNeeded) {
                message.append("Equipment Needed: Yes\n");
                message.append("Equipment Description: ").append(equipmentDescription).append("\n");
            } else {
                message.append("Equipment Needed: No\n");
            }

            message.append("Description: ").append(description).append("\n");

            // Show confirmation dialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle(isEditMode ? "Update Course" : "Add New Course")
                    .setMessage(message.toString())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        Log.d("CourseConfirm", "Confirmation accepted");
                        saveCourse();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d("CourseConfirm", "Confirmation cancelled");
                    })
                    .show();

        } catch (Exception e) {
            Log.e("CourseConfirm", "Error showing confirmation dialog", e);
            Toast.makeText(this, "Error showing confirmation. Saving directly.", Toast.LENGTH_SHORT).show();
            saveCourse();
        }
    }

    private void saveCourse() {
        currentCourse.setName(etCourseName.getText().toString().trim());
        currentCourse.setDayOfWeek(getSelectedDays());
        currentCourse.setTime(etTime.getText().toString().trim());
        currentCourse.setCapacity(Integer.parseInt(etCapacity.getText().toString().trim()));
        currentCourse.setDuration(Integer.parseInt(etDuration.getText().toString().trim()));
        currentCourse.setPrice(Double.parseDouble(etPrice.getText().toString().trim()));
        currentCourse.setType(spinnerType.getSelectedItem().toString());
        currentCourse.setDescription(etDescription.getText().toString().trim());
        currentCourse.setDifficulty(spinnerDifficulty.getSelectedItem().toString());
        currentCourse.setEquipmentNeeded(cbEquipmentNeeded.isChecked());
        currentCourse.setEquipmentDescription(etEquipmentDescription.getText().toString().trim());

        if (isEditMode) {
            dbHelper.updateCourse(currentCourse);
        } else {
            dbHelper.addCourse(currentCourse);
        }

        Toast.makeText(this, isEditMode ? "Course updated successfully" : "Course added successfully", Toast.LENGTH_SHORT).show();
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