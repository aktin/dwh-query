package org.aktin.dwh.optinout.util;

import lombok.val;
import org.aktin.Preferences;
import org.aktin.dwh.optinout.model.PatientReference;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialization helper for patient references. Retrieves root id from preferences
 */

@Singleton
public class PatientReferenceService {
    @Inject
    private Preferences prefs;

    private Map<PatientReference, String> referenceMap;

    @PostConstruct
    private void init() {
        val map = new HashMap<PatientReference, String>();
        map.put(PatientReference.Patient, prefs.get("cda.patient.root.preset"));
        map.put(PatientReference.Encounter, prefs.get("cda.encounter.root.preset"));
        map.put(PatientReference.Billing, prefs.get("cda.billing.root.preset"));
        referenceMap = Collections.unmodifiableMap(map);
    }

    public String getRoot(PatientReference ref) {
        return referenceMap.get(ref);
    }
}
