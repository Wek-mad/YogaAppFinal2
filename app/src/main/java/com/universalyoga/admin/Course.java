package com.universalyoga.admin;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a yoga course in the Universal Yoga app.
 * A course is a recurring class that happens on a specific day of the week.
 */
public class Course {
    // Required fields
    private int id; // Database primary key
    private String name; // Course name
    private String dayOfWeek; // e.g., "Monday", "Tuesday"
    private String time; // e.g., "10:00", "11:00"
    private int capacity; // How many persons can attend
    private int duration; // In minutes, e.g., 60
    private double price; // Price per class, e.g., 10.0 (£10)
    private String type; // e.g., "Flow Yoga", "Aerial Yoga", "Family Yoga"

    // Optional fields
    private String description;
    private String difficulty; // e.g., "Beginner", "Intermediate", "Advanced"
    private boolean equipmentNeeded; // Whether special equipment is needed
    private String equipmentDescription; // Description of equipment if needed

    // List to store class instances
    private List<ClassInstance> classInstances;

    /**
     * Constructor with required fields
     */
    public Course(String dayOfWeek, String time, int capacity, int duration, double price, String type) {
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.capacity = capacity;
        this.duration = duration;
        this.price = price;
        this.type = type;
        this.classInstances = new ArrayList<>();
    }
    public Course() {
        // Required by Firebase
    }
    /**
     * Full constructor with all fields
     */
    public Course(String name, String dayOfWeek, String time, int capacity, int duration, double price,
                  String type, String description, String difficulty,
                  boolean equipmentNeeded, String equipmentDescription) {
        this(dayOfWeek, time, capacity, duration, price, type);
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.equipmentNeeded = equipmentNeeded;
        this.equipmentDescription = equipmentDescription;
    }

    /**
     * Constructor with id for database retrieval
     */
    public Course(int id, String name, String dayOfWeek, String time, int capacity, int duration, double price,
                  String type, String description, String difficulty,
                  boolean equipmentNeeded, String equipmentDescription) {
        this(name, dayOfWeek, time, capacity, duration, price, type, description,
                difficulty, equipmentNeeded, equipmentDescription);
        this.id = id;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isEquipmentNeeded() {
        return equipmentNeeded;
    }

    public void setEquipmentNeeded(boolean equipmentNeeded) {
        this.equipmentNeeded = equipmentNeeded;
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public void setEquipmentDescription(String equipmentDescription) {
        this.equipmentDescription = equipmentDescription;
    }

    public List<ClassInstance> getClassInstances() {
        return classInstances;
    }

    /**
     * Add a class instance to this course
     */
    public void addClassInstance(ClassInstance instance) {
        if (classInstances == null) {
            classInstances = new ArrayList<>();
        }
        classInstances.add(instance);
    }

    /**
     * Remove a class instance from this course
     */
    public boolean removeClassInstance(ClassInstance instance) {
        return classInstances.remove(instance);
    }

    /**
     * Get a string representation of the course
     */
    @Override
    public String toString() {
        return (name != null && !name.isEmpty() ? name : type) + " - " + dayOfWeek + " at " + time + ", £" + price;
    }

    /**
     * Validate that all required fields are present
     * @return true if all required fields are valid, false otherwise
     */
    public boolean isValid() {
        return dayOfWeek != null && !dayOfWeek.isEmpty() &&
                time != null && !time.isEmpty() &&
                capacity > 0 &&
                duration > 0 &&
                price > 0 &&
                type != null && !type.isEmpty();
    }
}
