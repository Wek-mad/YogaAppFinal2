package com.universalyoga.admin;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Calendar;

/**
 * This class represents a specific instance of a yoga class.
 * It contains details about a specific date when a course will take place.
 */
public class ClassInstance {
    // Required fields
    private int id; // Database primary key
    private int courseId; // Foreign key to the course
    private Date date; // e.g., "17/10/2023"
    private String teacher; // Who is teaching this class

    // Optional fields
    private String additionalComments;
    private int availableSpots; // Number of spots still available
    private boolean isCancelled; // Whether this specific class has been canceled

    /**
     * Constructor with required fields
     */
    public ClassInstance(int courseId, Date date, String teacher) {
        this.courseId = courseId;
        this.date = date;
        this.teacher = teacher;
    }
    public ClassInstance() {
        // Required by Firestore for deserialization
    }
    /**
     * Full constructor with all fields
     */
    public ClassInstance(int courseId, Date date, String teacher,
                         String additionalComments, int availableSpots, boolean isCancelled) {
        this(courseId, date, teacher);
        this.additionalComments = additionalComments;
        this.availableSpots = availableSpots;
        this.isCancelled = isCancelled;
    }

    /**
     * Constructor with id for database retrieval
     */
    public ClassInstance(int id, int courseId, Date date, String teacher,
                         String additionalComments, int availableSpots, boolean isCancelled) {
        this(courseId, date, teacher, additionalComments, availableSpots, isCancelled);
        this.id = id;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getAdditionalComments() {
        return additionalComments;
    }

    public void setAdditionalComments(String additionalComments) {
        this.additionalComments = additionalComments;
    }

    public int getAvailableSpots() {
        return availableSpots;
    }

    public void setAvailableSpots(int availableSpots) {
        this.availableSpots = availableSpots;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     * Get a formatted date string (dd/MM/yyyy)
     */
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        return sdf.format(date);
    }

    /**
     * Parse a date string in the format dd/MM/yyyy
     * @param dateString The date string to parse
     * @return The parsed Date object, or null if the string couldn't be parsed
     */
    public static Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the day of the week for this class instance
     * @return The day of the week (e.g., "Monday", "Tuesday")
     */
    public String getDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String[] days = new String[] {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return days[dayIndex];
    }

    /**
     * Check if the day of the week of this instance matches the expected day for its course
     * @param expectedDay The expected day of the week for the course
     * @return true if the day matches, false otherwise
     */
    public boolean isDayMatching(String expectedDay) {
        return getDayOfWeek().equalsIgnoreCase(expectedDay);
    }

    /**
     * Get a string representation of the class instance
     */
    @Override
    public String toString() {
        return getFormattedDate() + " - Teacher: " + teacher +
                (isCancelled ? " (CANCELLED)" : "");
    }

    /**
     * Validate that all required fields are present
     * @return true if all required fields are valid, false otherwise
     */
    public boolean isValid() {
        return courseId > 0 &&
                date != null &&
                teacher != null && !teacher.isEmpty();
    }
}