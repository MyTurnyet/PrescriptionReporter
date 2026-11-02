package com.softwareascraft.prescriptionreporter;

public class Prescription {
    private final String userName;
    private final String drugName;
    private final int numberOfTimesFilled;

    public Prescription(String userName, String drugName, int fillCount) {
        this.userName = userName;
        this.drugName = drugName;
        this.numberOfTimesFilled = fillCount;
    }

    public int fillCount() {
        return this.numberOfTimesFilled;
    }

    public Prescription fill() {
        return new Prescription(this.userName, this.drugName, this.fillCount()+1);
    }
}
