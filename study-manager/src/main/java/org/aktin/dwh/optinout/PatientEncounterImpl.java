package org.aktin.dwh.optinout;

import java.time.Instant;

public class PatientEncounterImpl implements PatientEncounter {
    private int encounterId;
    private int patientId;
    private Instant startDate;
    private Instant endDate;

    PatientEncounterImpl(int encounterId, int patientId, Instant startDate, Instant endDate) {
         this.encounterId = encounterId;
         this.patientId = patientId;
         this.startDate = startDate;
         this.endDate = endDate;
    }

    @Override
    public int getEncounterId() {
        return encounterId;
    }

    @Override
    public int getPatientId() {
        return patientId;
    }

    @Override
    public Instant getStartDate() {
        return startDate;
    }

    @Override
    public Instant getEndDate() {
        return endDate;
    }
}
