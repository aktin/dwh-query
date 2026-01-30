package org.aktin.dwh.optinout.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class StudyImpl implements Study {
    private String id;
    private String title;
    private String description;
    private Instant createdTimestamp;
    private Instant closedTimestamp;
    @Setter
    private Participation participation;
    private String sicGenerator;
    private String sicGeneratorState;
    /**
     * Represents the specific mode of SIC (Standard Industrial Classification) generation
     * for the study. Determines whether SIC codes are generated manually, automatically,
     * or both, based on the state of the `sicGenerator` configuration during the
     * instantiation of the study. Defaults to {@link SICGeneration#ManualOnly}.
     */
    public SICGeneration getSicGeneration() {
        return sicGenerator != null
                && !sicGenerator.equals("MANUAL") ? SICGeneration.AutoAndManual : SICGeneration.ManualOnly;
    }
}
