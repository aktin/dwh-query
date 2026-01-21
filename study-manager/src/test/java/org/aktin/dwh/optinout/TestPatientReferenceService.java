package org.aktin.dwh.optinout;

import lombok.val;
import org.aktin.dwh.optinout.util.PatientReferenceService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestPatientReferenceService extends PatientReferenceService {
        private Map<PatientReference, String> referenceMap;

        TestPatientReferenceService() {
            val map = new HashMap<PatientReference, String>();
            map.put(PatientReference.Patient, "0");
            map.put(PatientReference.Encounter, "1");
            map.put(PatientReference.Billing, "2");
            referenceMap = Collections.unmodifiableMap(map);
        }

        public String getRoot(PatientReference ref) {
            return referenceMap.get(ref);
        }
}
