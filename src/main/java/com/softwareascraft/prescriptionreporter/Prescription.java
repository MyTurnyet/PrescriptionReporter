package com.softwareascraft.prescriptionreporter;

public class Prescription {
    private final String userName;
    private final String drugName;
    private final int numberOfTimesFilled;
    private final int numberOfTimesReturned;

    private Prescription(String userName, String drugName, int fillCount, int numberOfTimesReturned) {
        this.userName = userName;
        this.drugName = drugName;
        this.numberOfTimesFilled = fillCount;
        this.numberOfTimesReturned = numberOfTimesReturned;
    }

    public static Prescription create(String userName, String drugName) {
        return new Prescription(userName, drugName, 0, 0);
    }

    public int fillCount() {
        return this.numberOfTimesFilled;
    }

    public Prescription fill() {
        return new Prescription(this.userName, this.drugName, this.fillCount() + 1, this.numberOfTimesReturned);
    }

    public Prescription returnIt() {
        return new Prescription(this.userName, this.drugName, this.fillCount() - 1, this.numberOfTimesReturned + 1);
    }

    public int returnCount() {
        return this.numberOfTimesReturned;
    }
}
