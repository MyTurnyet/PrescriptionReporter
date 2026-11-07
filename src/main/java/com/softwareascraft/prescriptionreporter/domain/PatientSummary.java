package com.softwareascraft.prescriptionreporter.domain;

/**
 * One patient's line in the report.
 *
 * @param patientName  the patient
 * @param netFillCount fills net of returns, summed across all of that patient's prescriptions
 * @param income       whole dollars; negative when returns outweigh fills
 */
public record PatientSummary(String patientName, int netFillCount, long income) {
}
