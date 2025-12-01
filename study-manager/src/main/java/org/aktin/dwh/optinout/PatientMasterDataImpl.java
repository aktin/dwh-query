package org.aktin.dwh.optinout;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PatientMasterDataImpl implements PatientMasterData {
    private Instant birthDate;
    private String sex;
    private String zip;
    private int patientId;
}
