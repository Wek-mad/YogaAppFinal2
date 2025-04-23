package com.universalyoga.admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Custom adapter for displaying course items in a ListView
 */
public class CourseAdapter extends ArrayAdapter<Course> {

    private Context context;
    private List<Course> courses;
    private CourseActionListener actionListener;

    public interface CourseActionListener {
        void onEditCourse(Course course);
        void onManageClasses(Course course);
        void onDeleteCourse(Course course); // ✅ Add this line
    }


    public CourseAdapter(Context context, List<Course> courses, CourseActionListener listener) {
        super(context, R.layout.item_course, courses);
        this.context = context;
        this.courses = courses;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        }

        final Course currentCourse = courses.get(position);

        TextView tvCourseType = listItem.findViewById(R.id.tvCourseType);
        TextView tvCourseDay = listItem.findViewById(R.id.tvCourseDay);
        TextView tvCourseTime = listItem.findViewById(R.id.tvCourseTime);
        TextView tvCoursePrice = listItem.findViewById(R.id.tvCoursePrice);
        Button btnEditCourse = listItem.findViewById(R.id.btnEditCourse);
        Button btnManageClasses = listItem.findViewById(R.id.btnManageClasses);

        tvCourseType.setText(currentCourse.getType());
        tvCourseDay.setText(currentCourse.getDayOfWeek());
        tvCourseTime.setText(currentCourse.getTime() + " (" + currentCourse.getDuration() + " mins)");
        tvCoursePrice.setText("£" + String.format("%.2f", currentCourse.getPrice()));

        // Set button click listeners
        btnEditCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onEditCourse(currentCourse);
                }
            }
        });

        btnManageClasses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onManageClasses(currentCourse);
                }
            }
        });

        Button btnDeleteCourse = listItem.findViewById(R.id.btnDeleteCourse);
        btnDeleteCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onDeleteCourse(currentCourse);
                }
            }
        });

        return listItem;
    }
}