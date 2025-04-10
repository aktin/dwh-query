package org.aktin.dwh.optinout;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
public class PatientMasterDataImpl implements PatientMasterData {
    @Getter
    private Instant birthDate;
    @Getter
    private String sex;
    @Getter
    private String zip;
    @Getter
    private int patientId;

}
