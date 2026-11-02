package com.softwareascraft.prescriptionreporter;

public class Prescription {
    private final String userName;
    private final String drugName;

    public Prescription(String userName, String drugName) {
        this.userName = userName;
        this.drugName = drugName;
    }

    public int fillCount() {
        return 0;
    }
}
