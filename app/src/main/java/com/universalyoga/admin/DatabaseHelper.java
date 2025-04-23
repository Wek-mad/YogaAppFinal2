package com.universalyoga.admin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Database helper class to handle SQLite operations for the Universal Yoga app.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "UniversalYoga.db";
    private static final int DATABASE_VERSION = 2; // Increased from 1 to 2 to trigger database upgrade

    // Table Names
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_CLASS_INSTANCES = "class_instances";

    // Common Column Names
    private static final String KEY_ID = "id";

    // Courses Table Columns
    private static final String KEY_NAME = "name";
    private static final String KEY_DAY_OF_WEEK = "day_of_week";
    private static final String KEY_TIME = "time";
    private static final String KEY_CAPACITY = "capacity";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_PRICE = "price";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_EQUIPMENT_NEEDED = "equipment_needed";
    private static final String KEY_EQUIPMENT_DESCRIPTION = "equipment_description";

    // Class Instances Table Columns
    private static final String KEY_COURSE_ID = "course_id";
    private static final String KEY_DATE = "date";
    private static final String KEY_TEACHER = "teacher";
    private static final String KEY_ADDITIONAL_COMMENTS = "additional_comments";
    private static final String KEY_AVAILABLE_SPOTS = "available_spots";
    private static final String KEY_IS_CANCELLED = "is_cancelled";

    // Table Create Statements
    // Courses table create statement
    private static final String CREATE_TABLE_COURSES = "CREATE TABLE " + TABLE_COURSES +
            "(" +
            KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_NAME + " TEXT," +
            KEY_DAY_OF_WEEK + " TEXT NOT NULL," +
            KEY_TIME + " TEXT NOT NULL," +
            KEY_CAPACITY + " INTEGER NOT NULL," +
            KEY_DURATION + " INTEGER NOT NULL," +
            KEY_PRICE + " REAL NOT NULL," +
            KEY_TYPE + " TEXT NOT NULL," +
            KEY_DESCRIPTION + " TEXT," +
            KEY_DIFFICULTY + " TEXT," +
            KEY_EQUIPMENT_NEEDED + " INTEGER DEFAULT 0," +
            KEY_EQUIPMENT_DESCRIPTION + " TEXT" +
            ")";

    // Class Instances table create statement
    private static final String CREATE_TABLE_CLASS_INSTANCES = "CREATE TABLE " + TABLE_CLASS_INSTANCES +
            "(" +
            KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_COURSE_ID + " INTEGER NOT NULL," +
            KEY_DATE + " INTEGER NOT NULL," +  // Store as long (milliseconds since epoch)
            KEY_TEACHER + " TEXT NOT NULL," +
            KEY_ADDITIONAL_COMMENTS + " TEXT," +
            KEY_AVAILABLE_SPOTS + " INTEGER," +
            KEY_IS_CANCELLED + " INTEGER DEFAULT 0," +
            "FOREIGN KEY (" + KEY_COURSE_ID + ") REFERENCES " + TABLE_COURSES + "(" + KEY_ID + ")" +
            ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL(CREATE_TABLE_COURSES);
        db.execSQL(CREATE_TABLE_CLASS_INSTANCES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            // Add the missing name column to existing databases
            try {
                db.execSQL("ALTER TABLE " + TABLE_COURSES + " ADD COLUMN " + KEY_NAME + " TEXT");

                // Update existing rows to set name = type
                db.execSQL("UPDATE " + TABLE_COURSES + " SET " + KEY_NAME + " = " + KEY_TYPE);
            } catch (Exception e) {
                // Column might already exist in some cases, or other error occurred
                // Fallback to complete rebuild
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
                onCreate(db);
            }
        } else {
            // Fallback to complete rebuild for more complex upgrades
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
            onCreate(db);
        }
    }

    /**
     * Reset the database by dropping and recreating all tables
     */
    public void resetDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
        onCreate(db);
    }

    // Course CRUD Operations

    /**
     * Add a new course to the database
     * @param course The course to add
     * @return The ID of the newly inserted course, or -1 if failed
     */
    public long addCourse(Course course) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        // Check if name is null or empty and set a default if needed
        if (course.getName() == null || course.getName().isEmpty()) {
            values.put(KEY_NAME, course.getType()); // Use type as name if name is not provided
        } else {
            values.put(KEY_NAME, course.getName());
        }
        values.put(KEY_DAY_OF_WEEK, course.getDayOfWeek());
        values.put(KEY_TIME, course.getTime());
        values.put(KEY_CAPACITY, course.getCapacity());
        values.put(KEY_DURATION, course.getDuration());
        values.put(KEY_PRICE, course.getPrice());
        values.put(KEY_TYPE, course.getType());
        values.put(KEY_DESCRIPTION, course.getDescription());
        values.put(KEY_DIFFICULTY, course.getDifficulty());
        values.put(KEY_EQUIPMENT_NEEDED, course.isEquipmentNeeded() ? 1 : 0);
        values.put(KEY_EQUIPMENT_DESCRIPTION, course.getEquipmentDescription());

        // Insert the row
        long id = db.insert(TABLE_COURSES, null, values);

        if (id > 0) {
            course.setId((int) id);
            new FirestoreSyncManager().uploadCourse(course);
        }

        return id;
    }

    /**
     * Get a course from the database by its ID
     * @param id The ID of the course to retrieve
     * @return The course, or null if not found
     */
    public Course getCourse(int id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_COURSES,
                null,
                KEY_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null);

        Course course = null;
        if (cursor != null && cursor.moveToFirst()) {
            course = cursorToCourse(cursor);
            cursor.close();
        }

        return course;
    }

    /**
     * Get all courses from the database
     * @return A list of all courses
     */
    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_COURSES;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Course course = cursorToCourse(cursor);
                courses.add(course);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return courses;
    }

    /**
     * Update a course in the database
     * @param course The course to update
     * @return The number of rows affected
     */
    public int updateCourse(Course course) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        // Check if name is null or empty and set a default if needed
        if (course.getName() == null || course.getName().isEmpty()) {
            values.put(KEY_NAME, course.getType()); // Use type as name if name is not provided
        } else {
            values.put(KEY_NAME, course.getName());
        }
        values.put(KEY_DAY_OF_WEEK, course.getDayOfWeek());
        values.put(KEY_TIME, course.getTime());
        values.put(KEY_CAPACITY, course.getCapacity());
        values.put(KEY_DURATION, course.getDuration());
        values.put(KEY_PRICE, course.getPrice());
        values.put(KEY_TYPE, course.getType());
        values.put(KEY_DESCRIPTION, course.getDescription());
        values.put(KEY_DIFFICULTY, course.getDifficulty());
        values.put(KEY_EQUIPMENT_NEEDED, course.isEquipmentNeeded() ? 1 : 0);
        values.put(KEY_EQUIPMENT_DESCRIPTION, course.getEquipmentDescription());

        // Update the row
        int rows = db.update(TABLE_COURSES, values, KEY_ID + "=?", new String[]{String.valueOf(course.getId())});
        if (rows > 0) {
            new FirestoreSyncManager().uploadCourse(course);
        }
        return rows;
    }

    /**
     * Delete a course from the database
     * @param courseId The ID of the course to delete
     * @return The number of rows affected
     */
    public int deleteCourse(int courseId) {
        SQLiteDatabase db = getWritableDatabase();
        FirestoreSyncManager syncManager = new FirestoreSyncManager();

        // Fetch class instances tied to this course
        List<ClassInstance> instances = getClassInstancesForCourse(courseId);

        for (ClassInstance instance : instances) {
            // Delete from Firestore
            syncManager.deleteClassInstance(instance.getId());

            // Delete from local DB
            db.delete(
                    TABLE_CLASS_INSTANCES,
                    KEY_ID + "=?",
                    new String[]{String.valueOf(instance.getId())}
            );
        }

        // Delete the course from Firestore
        syncManager.deleteCourse(courseId);

        // Delete from local DB
        return db.delete(
                TABLE_COURSES,
                KEY_ID + "=?",
                new String[]{String.valueOf(courseId)}
        );
    }

    // Class Instance CRUD Operations

    /**
     * Add a new class instance to the database
     * @param instance The class instance to add
     * @return The ID of the newly inserted class instance, or -1 if failed
     */
    public long addClassInstance(ClassInstance instance) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_COURSE_ID, instance.getCourseId());
        values.put(KEY_DATE, instance.getDate().getTime());  // Store as milliseconds
        values.put(KEY_TEACHER, instance.getTeacher());
        values.put(KEY_ADDITIONAL_COMMENTS, instance.getAdditionalComments());
        values.put(KEY_AVAILABLE_SPOTS, instance.getAvailableSpots());
        values.put(KEY_IS_CANCELLED, instance.isCancelled() ? 1 : 0);

        // Insert the row
        long id = db.insert(TABLE_CLASS_INSTANCES, null, values);

        if (id > 0) {
            instance.setId((int) id);
            new FirestoreSyncManager().uploadClassInstance(instance);
        }

        return id;
    }

    /**
     * Get a class instance from the database by its ID
     * @param id The ID of the class instance to retrieve
     * @return The class instance, or null if not found
     */
    public ClassInstance getClassInstance(int id) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_CLASS_INSTANCES,
                null,
                KEY_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null);

        ClassInstance instance = null;
        if (cursor != null && cursor.moveToFirst()) {
            instance = cursorToClassInstance(cursor);
            cursor.close();
        }

        return instance;
    }

    /**
     * Get all class instances for a specific course
     * @param courseId The ID of the course
     * @return A list of class instances for the course
     */
    public List<ClassInstance> getClassInstancesForCourse(int courseId) {
        List<ClassInstance> instances = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CLASS_INSTANCES,
                null,
                KEY_COURSE_ID + "=?",
                new String[]{String.valueOf(courseId)},
                null, null, KEY_DATE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                ClassInstance instance = cursorToClassInstance(cursor);
                instances.add(instance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return instances;
    }

    /**
     * Update a class instance in the database
     * @param instance The class instance to update
     * @return The number of rows affected
     */
    public int updateClassInstance(ClassInstance instance) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_COURSE_ID, instance.getCourseId());
        values.put(KEY_DATE, instance.getDate().getTime());
        values.put(KEY_TEACHER, instance.getTeacher());
        values.put(KEY_ADDITIONAL_COMMENTS, instance.getAdditionalComments());
        values.put(KEY_AVAILABLE_SPOTS, instance.getAvailableSpots());
        values.put(KEY_IS_CANCELLED, instance.isCancelled() ? 1 : 0);

        // Update the row
        int rows = db.update(TABLE_CLASS_INSTANCES, values, KEY_ID + "=?", new String[]{String.valueOf(instance.getId())});
        if (rows > 0) {
            new FirestoreSyncManager().uploadClassInstance(instance);
        }
        return rows;
    }

    /**
     * Delete a class instance from the database
     * @param instanceId The ID of the class instance to delete
     * @return The number of rows affected
     */
    public int deleteClassInstance(int instanceId) {
        int rows = getWritableDatabase().delete(TABLE_CLASS_INSTANCES, KEY_ID + "=?", new String[]{String.valueOf(instanceId)});
        if (rows > 0) {
            new FirestoreSyncManager().deleteClassInstance(instanceId);
        }
        return rows;
    }

    /**
     * Search for class instances by teacher name
     * @param teacherName The teacher name to search for (partial match)
     * @return A list of class instances with matching teacher name
     */
    public List<ClassInstance> searchClassInstancesByTeacher(String keyword) {
        List<ClassInstance> list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM class_instances WHERE teacher LIKE ?",
                new String[]{"%" + keyword + "%"}
        );

        if (cursor.moveToFirst()) {
            do {
                ClassInstance instance = new ClassInstance(
                        cursor.getInt(cursor.getColumnIndexOrThrow("course_id")),
                        new Date(cursor.getLong(cursor.getColumnIndexOrThrow("date"))),
                        cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                        cursor.getString(cursor.getColumnIndexOrThrow("additional_comments")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("available_spots")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("is_cancelled")) == 1
                );
                instance.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                list.add(instance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * Search for class instances by date
     * @param date The date to search for
     * @return A list of class instances on the specified date
     */
    public List<ClassInstance> searchClassInstancesByDate(Date date) {
        List<ClassInstance> instances = new ArrayList<>();

        // Get start and end of the day in milliseconds
        long startOfDay = date.getTime();
        startOfDay = startOfDay - (startOfDay % (24 * 60 * 60 * 1000));
        long endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CLASS_INSTANCES,
                null,
                KEY_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(startOfDay), String.valueOf(endOfDay)},
                null, null, KEY_DATE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                ClassInstance instance = cursorToClassInstance(cursor);
                instances.add(instance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return instances;
    }

    /**
     * Search for courses by day of the week
     * @param dayOfWeek The day of the week to search for
     * @return A list of courses on the specified day
     */
    public List<Course> searchCoursesByDayOfWeek(String dayOfWeek) {
        List<Course> courses = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_COURSES,
                null,
                KEY_DAY_OF_WEEK + "=?",
                new String[]{dayOfWeek},
                null, null, KEY_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Course course = cursorToCourse(cursor);
                courses.add(course);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return courses;
    }

    // Helper methods to convert Cursor to objects

    private Course cursorToCourse(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));

        // Try to get name, but fall back to type if name doesn't exist
        String name;
        try {
            name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME));
            if (name == null || name.isEmpty()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE));
            }
        } catch (IllegalArgumentException e) {
            // If the name column doesn't exist, use type instead
            name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE));
        }

        String dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DAY_OF_WEEK));
        String time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME));
        int capacity = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CAPACITY));
        int duration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DURATION));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE));
        String type = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE));

        String description = null;
        try {
            description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION));
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        String difficulty = null;
        try {
            difficulty = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DIFFICULTY));
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        boolean equipmentNeeded = false;
        try {
            equipmentNeeded = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EQUIPMENT_NEEDED)) == 1;
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        String equipmentDescription = null;
        try {
            equipmentDescription = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EQUIPMENT_DESCRIPTION));
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        Course course = new Course(
                id, name, dayOfWeek, time, capacity, duration, price, type,
                description, difficulty, equipmentNeeded, equipmentDescription
        );

        return course;
    }

    private ClassInstance cursorToClassInstance(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
        int courseId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COURSE_ID));
        long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DATE));
        String teacher = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEACHER));

        String additionalComments = null;
        try {
            additionalComments = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ADDITIONAL_COMMENTS));
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        int availableSpots = 0;
        try {
            availableSpots = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AVAILABLE_SPOTS));
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        boolean isCancelled = false;
        try {
            isCancelled = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_CANCELLED)) == 1;
        } catch (IllegalArgumentException e) {
            // Handle missing column gracefully
        }

        Date date = new Date(dateMillis);

        return new ClassInstance(
                id, courseId, date, teacher, additionalComments, availableSpots, isCancelled
        );
    }

    public void insertOrUpdateCourse(Course course) {
        if (getCourseById(course.getId()) == null) {
            insertCourse(course);
        } else {
            updateCourse(course);
        }
    }

    public void insertOrUpdateClassInstance(ClassInstance instance) {
        if (getClassInstanceById(instance.getId()) == null) {
            insertClassInstance(instance);
        } else {
            updateClassInstance(instance);
        }
    }
    // Get course by ID (support for insertOrUpdate)
    public Course getCourseById(int id) {
        return getCourse(id); // đã có sẵn hàm getCourse
    }

    // Get class instance by ID
    public ClassInstance getClassInstanceById(int id) {
        return getClassInstance(id); // đã có sẵn hàm getClassInstance
    }

    // Insert course without Firestore sync (used by insertOrUpdate)
    public long insertCourse(Course course) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, course.getId()); // important: set ID manually from Firestore
        values.put(KEY_NAME, course.getName());
        values.put(KEY_DAY_OF_WEEK, course.getDayOfWeek());
        values.put(KEY_TIME, course.getTime());
        values.put(KEY_CAPACITY, course.getCapacity());
        values.put(KEY_DURATION, course.getDuration());
        values.put(KEY_PRICE, course.getPrice());
        values.put(KEY_TYPE, course.getType());
        values.put(KEY_DESCRIPTION, course.getDescription());
        values.put(KEY_DIFFICULTY, course.getDifficulty());
        values.put(KEY_EQUIPMENT_NEEDED, course.isEquipmentNeeded() ? 1 : 0);
        values.put(KEY_EQUIPMENT_DESCRIPTION, course.getEquipmentDescription());

        return db.insert(TABLE_COURSES, null, values);
    }

    // Insert class instance without Firestore sync (used by insertOrUpdate)
    public long insertClassInstance(ClassInstance instance) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, instance.getId()); // set ID from Firestore
        values.put(KEY_COURSE_ID, instance.getCourseId());
        values.put(KEY_DATE, instance.getDate().getTime());
        values.put(KEY_TEACHER, instance.getTeacher());
        values.put(KEY_ADDITIONAL_COMMENTS, instance.getAdditionalComments());
        values.put(KEY_AVAILABLE_SPOTS, instance.getAvailableSpots());
        values.put(KEY_IS_CANCELLED, instance.isCancelled() ? 1 : 0);

        return db.insert(TABLE_CLASS_INSTANCES, null, values);
    }

    public void deleteAllCoursesAndClasses() {
        SQLiteDatabase db = getWritableDatabase();

        // Step 1: Get all class instances and delete from Firestore
        Cursor classCursor = db.query(TABLE_CLASS_INSTANCES, new String[]{KEY_ID}, null, null, null, null, null);
        if (classCursor.moveToFirst()) {
            do {
                int instanceId = classCursor.getInt(classCursor.getColumnIndexOrThrow(KEY_ID));
            } while (classCursor.moveToNext());
        }
        classCursor.close();

        // Step 2: Get all courses and delete from Firestore
        Cursor courseCursor = db.query(TABLE_COURSES, new String[]{KEY_ID}, null, null, null, null, null);
        if (courseCursor.moveToFirst()) {
            do {
                int courseId = courseCursor.getInt(courseCursor.getColumnIndexOrThrow(KEY_ID));
            } while (courseCursor.moveToNext());
        }
        courseCursor.close();

        // Step 3: Delete all records from both tables locally
        db.delete(TABLE_CLASS_INSTANCES, null, null);
        db.delete(TABLE_COURSES, null, null);
    }

}