// --- MainActivity.java (UPDATED) ---
package com.universalyoga.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CourseAdapter.CourseActionListener {

    private static final int REQUEST_ADD_COURSE = 1;
    private static final int REQUEST_EDIT_COURSE = 2;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

    private DatabaseHelper dbHelper;
    private ListView courseListView;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Universal Yoga Courses");
        }

        dbHelper = new DatabaseHelper(this);
        courseListView = findViewById(R.id.courseListView);
        courseList = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courseList, this);
        courseListView.setAdapter(courseAdapter);

        FloatingActionButton fab = findViewById(R.id.fabAddCourse);
        fab.setOnClickListener(view -> openAddCourseActivity());

        syncFromCloud();
    }

    private void syncFromCloud() {
        dbHelper.deleteAllCoursesAndClasses();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Log.d("MainActivity", "Fetching data from Firestore...");

        firestore.collection("courses")
                .get()
                .addOnSuccessListener(courseSnapshots -> {
                    for (DocumentSnapshot doc : courseSnapshots) {
                        Course course = doc.toObject(Course.class);
                        if (course != null) {
                            try {
                                course.setId(Integer.parseInt(doc.getId()));
                                dbHelper.insertOrUpdateCourse(course);
                            } catch (Exception e) {
                                Log.w("MainActivity", "Invalid course ID: " + doc.getId(), e);
                            }
                        }
                    }

                    firestore.collection("class_instances")
                            .get()
                            .addOnSuccessListener(instanceSnapshots -> {
                                Log.d("MainActivity", "Fetched " + instanceSnapshots.size() + " class_instances documents");
                                for (DocumentSnapshot doc : instanceSnapshots) {
                                    try {
                                        ClassInstance instance = doc.toObject(ClassInstance.class);
                                        if (instance != null) {
                                            instance.setId(Integer.parseInt(doc.getId()));
                                            dbHelper.insertOrUpdateClassInstance(instance);
                                            Log.d("MainActivity", "Synced instance: " + doc.getId());
                                        }
                                    } catch (Exception ex) {
                                        // Fallback for String date
                                        try {
                                            int courseId = ((Long) doc.get("courseId")).intValue();
                                            String teacher = doc.getString("teacher");
                                            String dateStr = doc.getString("date");
                                            Date parsedDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(dateStr);
                                            String comments = doc.getString("comments");
                                            long spots = doc.getLong("availableSpots") != null ? doc.getLong("availableSpots") : 0;
                                            boolean isCancelled = Boolean.TRUE.equals(doc.getBoolean("isCancelled"));

                                            ClassInstance fallback = new ClassInstance(courseId, parsedDate, teacher, comments, (int) spots, isCancelled);
                                            fallback.setId(Integer.parseInt(doc.getId()));
                                            dbHelper.insertOrUpdateClassInstance(fallback);
                                            Log.d("MainActivity", "Fallback inserted instance: " + doc.getId());
                                        } catch (ParseException pe) {
                                            Log.e("MainActivity", "Date parse failed for doc: " + doc.getId(), pe);
                                        } catch (Exception e) {
                                            Log.e("MainActivity", "Failed to process instance: " + doc.getId(), e);
                                        }
                                    }
                                }
                                loadCourses();
                                Toast.makeText(MainActivity.this, "Synced from cloud", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MainActivity", "Failed to fetch class instances", e);
                                Toast.makeText(MainActivity.this, "Failed to sync class instances", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Failed to fetch courses", e);
                    Toast.makeText(MainActivity.this, "Failed to sync courses", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        courseList.clear();
        courseList.addAll(dbHelper.getAllCourses());
        courseAdapter.notifyDataSetChanged();

        if (courseList.isEmpty()) {
            Toast.makeText(this, "No courses found. Add a course to get started.", Toast.LENGTH_LONG).show();
        }
    }

    private void openAddCourseActivity() {
        Intent intent = new Intent(this, AddEditCourseActivity.class);
        startActivityForResult(intent, REQUEST_ADD_COURSE);
    }

    private void openEditCourseActivity(Course course) {
        Intent intent = new Intent(this, AddEditCourseActivity.class);
        intent.putExtra("course_id", course.getId());
        startActivityForResult(intent, REQUEST_EDIT_COURSE);
    }

    private void openClassInstancesActivity(Course course) {
        Intent intent = new Intent(this, ClassInstancesActivity.class);
        intent.putExtra("course_id", course.getId());
        startActivity(intent);
    }

    private void deleteCourse(final Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete the course \"" + course.getType() + " - " + course.getDayOfWeek() + "\"? This will also delete all class instances.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteCourse(course.getId());
                    FirestoreSyncManager syncManager = new FirestoreSyncManager();
                    syncManager.deleteCourse(course.getId());
                    List<ClassInstance> instances = dbHelper.getClassInstancesForCourse(course.getId());
                    for (ClassInstance instance : instances) {
                        syncManager.deleteClassInstance(instance.getId());
                    }
                    loadCourses();
                    Toast.makeText(MainActivity.this, "Course deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetDatabase() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Database")
                .setMessage("Are you sure you want to reset the database? This will delete all courses and class instances.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    dbHelper.resetDatabase();
                    loadCourses();
                    Toast.makeText(MainActivity.this, "Database reset", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSearchActivity() {
        Intent intent = new Intent(this, SearchClassActivity.class);
        startActivity(intent);
    }

    private void uploadToCloud() {
        Toast.makeText(this, "Uploading data to cloud...", Toast.LENGTH_SHORT).show();
        FirestoreSyncManager syncManager = new FirestoreSyncManager();
        List<Course> courses = dbHelper.getAllCourses();
        for (Course course : courses) {
            syncManager.uploadCourse(course);
            List<ClassInstance> instances = dbHelper.getClassInstancesForCourse(course.getId());
            for (ClassInstance instance : instances) {
                syncManager.uploadClassInstance(instance);
            }
        }
    }

    @Override
    public void onEditCourse(Course course) {
        openEditCourseActivity(course);
    }

    @Override
    public void onManageClasses(Course course) {
        openClassInstancesActivity(course);
    }

    @Override
    public void onDeleteCourse(Course course) {
        deleteCourse(course);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadCourses();
            return true;
        } else if (id == R.id.action_reset_db) {
            resetDatabase();
            return true;
        } else if (id == R.id.action_search) {
            openSearchActivity();
            return true;
        } else if (id == R.id.action_upload) {
            uploadToCloud();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_COURSE || requestCode == REQUEST_EDIT_COURSE) {
                loadCourses();
                Toast.makeText(this, "Course saved successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
