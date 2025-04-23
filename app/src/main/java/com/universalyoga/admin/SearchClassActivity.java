package com.universalyoga.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

public class SearchClassActivity extends AppCompatActivity {

    private EditText etSearchTeacher;
    private Button btnSearch;
    private ListView lvSearchResults;
    private ImageView btnBack;

    private DatabaseHelper dbHelper;
    private List<ClassInstance> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_class);

        // Hide default action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Initialize UI elements
        etSearchTeacher = findViewById(R.id.etSearchTeacher);
        btnSearch = findViewById(R.id.btnSearch);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        // Set up back button - this is the key part!
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and return to previous
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = etSearchTeacher.getText().toString().trim();
                if (query.isEmpty()) {
                    Toast.makeText(SearchClassActivity.this, "Please enter a teacher name", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    results = dbHelper.searchClassInstancesByTeacher(query);

                    if (results.isEmpty()) {
                        Toast.makeText(SearchClassActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    }

                    ArrayAdapter<ClassInstance> adapter = new ArrayAdapter<>(
                            SearchClassActivity.this,
                            android.R.layout.simple_list_item_1,
                            results
                    );
                    lvSearchResults.setAdapter(adapter);
                } catch (Exception e) {
                    Toast.makeText(SearchClassActivity.this, "Error searching database", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add item click listener to handle class selection
        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClassInstance selectedClass = results.get(position);

                // Get the course ID for this class instance
                int courseId = selectedClass.getCourseId();

                // Create intent to open ClassInstancesActivity
                Intent intent = new Intent(SearchClassActivity.this, ClassInstancesActivity.class);
                intent.putExtra("course_id", courseId);
                intent.putExtra("selected_instance_id", selectedClass.getId());
                startActivity(intent);

                // Optionally finish this activity
                finish();
            }
        });
    }
}