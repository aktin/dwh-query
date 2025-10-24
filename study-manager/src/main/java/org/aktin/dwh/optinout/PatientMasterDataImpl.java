package org.aktin.dwh.optinout;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
