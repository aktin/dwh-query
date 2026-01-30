package org.aktin.dwh.optinout.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class PatientMasterDataImpl implements PatientMasterData {
    private String idEnc;
    private Instant birthDate;
    private String sex;
    private String zip;
}
