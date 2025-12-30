package org.aktin.dwh.optinout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class StudyImpl implements Study {
    private String id;
    private String title;
    private String description;
    private Instant createdTimestamp;
    private Instant closedTimestamp;
    private Participation participation;
    private String sicGenerator;
    private String sicGeneratorState;
    private SICGeneration sicGeneration;
}
