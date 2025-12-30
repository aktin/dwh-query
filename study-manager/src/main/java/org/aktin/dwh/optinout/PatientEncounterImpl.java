package org.aktin.dwh.optinout;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class PatientEncounterImpl implements PatientEncounter {
    private String pseudonym;
    private String encounterId;
    private Instant startDate;
    private Instant endDate;
}
