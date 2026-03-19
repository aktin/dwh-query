package org.aktin.dwh.optinout.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class PatientEncounterPeriodImpl implements PatientEncounterPeriod {
    private String ide;
    private Instant startDate;
    private Instant endDate;
}
