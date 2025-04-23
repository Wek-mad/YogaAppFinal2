package com.universalyoga.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ClassInstancesActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_INSTANCE = 1;
    private static final int REQUEST_EDIT_INSTANCE = 2;

    private DatabaseHelper dbHelper;
    private Course currentCourse;
    private List<ClassInstance> instanceList;

    private TextView tvCourseInfo;
    private ListView lvInstances;
    private FloatingActionButton fabAddInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_instances);

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Class Instances");
        }
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

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

        // Initialize UI
        initializeUI();

        // Set course info
        tvCourseInfo.setText(String.format("%s - %s at %s\nCapacity: %d, Duration: %d minutes",
                currentCourse.getType(),
                currentCourse.getDayOfWeek(),
                currentCourse.getTime(),
                currentCourse.getCapacity(),
                currentCourse.getDuration()));

        // Load instances
        loadInstances();

        // Set up item click listener for editing
        lvInstances.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClassInstance selectedInstance = instanceList.get(position);
                openEditInstanceActivity(selectedInstance);
            }
        });

        // Set up long click listener for deleting
        lvInstances.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ClassInstance selectedInstance = instanceList.get(position);
                deleteInstance(selectedInstance);
                return true;
            }
        });

        // Set up FAB to add new instance
        fabAddInstance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddInstanceActivity();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and go back
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeUI() {
        tvCourseInfo = findViewById(R.id.tvCourseInfo);
        lvInstances = findViewById(R.id.lvInstances);
        fabAddInstance = findViewById(R.id.fabAddInstance);
    }

    private void loadInstances() {
        instanceList = dbHelper.getClassInstancesForCourse(currentCourse.getId());

        if (instanceList.isEmpty()) {
            Toast.makeText(this, "No class instances found. Add one to get started.", Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<ClassInstance> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                instanceList
        );

        lvInstances.setAdapter(adapter);
    }

    private void openAddInstanceActivity() {
        Intent intent = new Intent(this, AddEditClassInstanceActivity.class);
        intent.putExtra("course_id", currentCourse.getId());
        startActivityForResult(intent, REQUEST_ADD_INSTANCE);
    }

    private void openEditInstanceActivity(ClassInstance instance) {
        Intent intent = new Intent(this, AddEditClassInstanceActivity.class);
        intent.putExtra("course_id", currentCourse.getId());
        intent.putExtra("instance_id", instance.getId());
        startActivityForResult(intent, REQUEST_EDIT_INSTANCE);
    }

    private void deleteInstance(final ClassInstance instance) {
        // Show a confirmation dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Class Instance");
        builder.setMessage("Are you sure you want to delete this class instance?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            dbHelper.deleteClassInstance(instance.getId());
            loadInstances(); // Refresh the list
            Toast.makeText(ClassInstancesActivity.this, "Class instance deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            loadInstances(); // Refresh the list
        }
    }
}