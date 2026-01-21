package org.aktin.dwh.optinout.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class StudyImpl implements Study {
    public StudyImpl(String id,
              String title,
              String description,
              Instant createdTimestamp,
              Instant closedTimestamp,
              Participation participation,
              String sicGenerator,
              String sicGeneratorState) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdTimestamp = createdTimestamp;
        this.closedTimestamp = closedTimestamp;
        this.participation = participation;
        this.sicGenerator = sicGenerator;
        this.sicGeneratorState = sicGeneratorState;

        determineSicGenerationStrategy();
    }

    private String id;
    private String title;
    private String description;
    private Instant createdTimestamp;
    private Instant closedTimestamp;
    private Participation participation;
    private String sicGenerator;
    private String sicGeneratorState;
    /**
     * Represents the specific mode of SIC (Standard Industrial Classification) generation
     * for the study. Determines whether SIC codes are generated manually, automatically,
     * or both, based on the state of the `sicGenerator` configuration during the
     * instantiation of the study. Defaults to {@link SICGeneration#ManualOnly}.
     */
    private SICGeneration sicGeneration;

    private void determineSicGenerationStrategy() {
        sicGeneration = sicGenerator != null
                && !sicGenerator.equals("MANUAL") ? SICGeneration.AutoAndManual : SICGeneration.ManualOnly;
    }
}
