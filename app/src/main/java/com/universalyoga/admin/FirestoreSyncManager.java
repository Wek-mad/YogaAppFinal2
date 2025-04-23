package com.universalyoga.admin;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreSyncManager {

    private static final String TAG = "FirestoreSync";
    private final FirebaseFirestore firestore;

    public FirestoreSyncManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Upload a single course to Firestore
    public void uploadCourse(@NonNull Course course) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", course.getName());
        data.put("dayOfWeek", course.getDayOfWeek());
        data.put("time", course.getTime());
        data.put("capacity", course.getCapacity());
        data.put("duration", course.getDuration());
        data.put("price", course.getPrice());
        data.put("type", course.getType());
        data.put("description", course.getDescription());
        data.put("difficulty", course.getDifficulty());
        data.put("equipmentNeeded", course.isEquipmentNeeded());
        data.put("equipmentDescription", course.getEquipmentDescription());

        firestore.collection("courses")
                .document(String.valueOf(course.getId()))
                .set(data)
                .addOnSuccessListener(unused -> Log.d(TAG, "Course synced: " + course.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync course", e));
    }

    // Delete a course from Firestore
    public void deleteCourse(int courseId) {
        firestore.collection("courses")
                .document(String.valueOf(courseId))
                .delete()
                .addOnSuccessListener(unused -> Log.d(TAG, "Course deleted in Firestore: " + courseId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete course", e));
    }

    // Upload a class instance to Firestore
    public void uploadClassInstance(@NonNull ClassInstance instance) {
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", instance.getCourseId());
        data.put("teacher", instance.getTeacher());
        data.put("date", instance.getDate().getTime());
        data.put("comments", instance.getAdditionalComments());
        data.put("availableSpots", instance.getAvailableSpots());
        data.put("isCancelled", instance.isCancelled());

        firestore.collection("class_instances")
                .document(String.valueOf(instance.getId()))
                .set(data)
                .addOnSuccessListener(unused -> Log.d(TAG, "Class instance synced: " + instance.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync class instance", e));
    }

    // Delete a class instance from Firestore
    public void deleteClassInstance(int instanceId) {
        firestore.collection("class_instances")
                .document(String.valueOf(instanceId))
                .delete()
                .addOnSuccessListener(unused -> Log.d(TAG, "Class instance deleted: " + instanceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete class instance", e));
    }

    // Fetch all courses from Firestore
    public void fetchCourses(FirestoreFetchCallback<Course> callback) {
        Log.d(TAG, "[fetchCourses] Fetching courses from Firestore...");

        firestore.collection("courses")
                .get()
                .addOnSuccessListener(snapshots -> {
                    Log.d(TAG, "[fetchCourses] Fetch success: " + snapshots.size() + " documents");

                    if (snapshots.isEmpty()) {
                        Log.w(TAG, "[fetchCourses] No documents found in 'courses' collection.");
                    }

                    List<Course> courses = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Log.d(TAG, "[fetchCourses] Document ID: " + doc.getId() + ", Data: " + doc.getData());

                        Course course = doc.toObject(Course.class);
                        if (course != null) {
                            try {
                                course.setId(Integer.parseInt(doc.getId()));
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "[fetchCourses] Invalid course ID format: " + doc.getId(), e);
                            }
                            courses.add(course);
                        } else {
                            Log.w(TAG, "[fetchCourses] Failed to map document to Course: " + doc.getId());
                        }
                    }

                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "[fetchCourses] Firestore fetch failed", e);
                    callback.onFailure(e);
                });
    }

    // Fetch all class instances from Firestore
    public void fetchClassInstances(FirestoreFetchCallback<ClassInstance> callback) {
        firestore.collection("class_instances")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<ClassInstance> instances = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        ClassInstance instance = doc.toObject(ClassInstance.class);
                        if (instance != null) {
                            instance.setId(Integer.parseInt(doc.getId()));
                            instances.add(instance);
                            Map<String, Object> data = new HashMap<>();
                            data.put("date", instance.getDate()); // <-- giữ nguyên Date object

                        }
                    }
                    callback.onSuccess(instances);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
