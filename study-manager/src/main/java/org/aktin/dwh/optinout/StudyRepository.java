package org.aktin.dwh.optinout;

import lombok.val;
import org.aktin.dwh.optinout.sic.CodeGeneratorFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class StudyRepository {
    private final static Logger log = Logger.getLogger(StudyRepository.class.getName());

    private DataSourceProvider dsp;

    @Inject
    StudyRepository(DataSourceProvider dsp) {
        this.dsp = dsp;
    }

    /**
     * Generates a new SIC (Study Identification Code) for the specified study.
     * @param studyId the unique identifier of the study for which the SIC is to be generated
     * @return the generated SIC as a String
     * @throws IllegalArgumentException if the study with the specified ID is not found
     * @throws UnsupportedOperationException if SIC generation is not supported for the study
     * @throws SQLException if an error occurs while updating the SIC generator state in the database
     * @throws IOException if an error occurs during SIC generation
     */
    public String generateSIC(String studyId) throws SQLException, IOException {
        StudyImpl study = getStudy(studyId);
        if (study == null) {
            throw new IllegalArgumentException("Unknown study: " + studyId);
        }

        val codeGenerator = study.getSicGeneration() == SICGeneration.AutoAndManual
                ? CodeGeneratorFactory.createInstance(study.getSicGenerator(), study.getSicGeneratorState())
                : null;

        if (codeGenerator == null) {
            throw new UnsupportedOperationException("Generation of SICs not supported by this study: " + study.getTitle());
        }

        val code = codeGenerator.generateCode();
        // write state
        try (val ps = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_UPDATE_SIC_STATE)) {
            ps.setString(1, codeGenerator.getState());
            ps.setString(2, study.getId());
            ps.executeUpdate();
        }

        return code;
    }

    /**
     * Retrieves a list of all studies from the database.
     *
     * @return a list of StudyImpl objects representing the studies retrieved from the database
     * @throws SQLException if a database access error occurs
     */
    public List<StudyImpl> getStudies() throws SQLException {
        val list = new ArrayList<StudyImpl>();
        try (val rs = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_ALL_STUDIES).executeQuery()) {

            while (rs.next()) {
                val study = toStudy(rs);
                list.add(study);
            }
        }

        return list;
    }

    /**
     * Retrieves a study from the database based on its unique identifier.
     *
     * @param id the unique identifier of the study to be retrieved
     * @return a StudyImpl object representing the study with the specified ID, or null if no study is found
     * @throws SQLException if a database access error occurs
     */
    public StudyImpl getStudy(String id) throws SQLException {
        try (val ps = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_STUDY_BY_ID)) {
            ps.setString(1, id);
            val rs = ps.executeQuery();
            if (!rs.isBeforeFirst()) {
                return null;
            }
            rs.next();
            return toStudy(rs);
        }
    }

    /**
     * Adds a new study to the database.
     *
     * @param id the unique identifier of the study
     * @param title the title of the study
     * @param description a brief description of the study
     * @param options serialized representation of study-specific options
     * @param sicGenerate the study identification code (SIC) generation strategy
     * @throws SQLException if a database access error occurs
     */
    public void addStudy(String id, String title, String description, String options, String sicGenerate) throws SQLException {
        try (PreparedStatement ps = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_INSERT_STUDY)) {
            ps.setString(1, id);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, options);
            ps.setString(5, sicGenerate);
            ps.execute();
        }
    }

    /**
     * Converts a ResultSet row into a StudyImpl object by extracting the relevant fields
     * and applying options to the study.
     *
     * @param rs the ResultSet containing the study data to be converted
     * @return a StudyImpl object populated with data from the ResultSet row
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    private StudyImpl toStudy(ResultSet rs) throws SQLException {
        Instant closedTimestamp = rs.getTimestamp(5) != null ? rs.getTimestamp(5).toInstant() : null;
        val sicGenerator = rs.getString(7);
        val sicGeneration = sicGenerator == null || sicGenerator.isEmpty() || sicGenerator.equals("MANUAL")
                ? SICGeneration.ManualOnly : SICGeneration.AutoAndManual;


        val study = new StudyImpl(rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getTimestamp(4).toInstant(),
                closedTimestamp,
                null,
                sicGenerator,
                rs.getString(8),
                sicGeneration);

        // load options
        parseAndApplyOptions(study, rs.getString(6));

        return study;
    }

    /**
     * Parses a comma-separated list of database options and applies them to the specified study.
     * Each option is expected to be in the format "key=value". Only valid key-value pairs
     * will be processed and applied.
     *
     * @param study the {@code StudyImpl} instance to which the options will be applied
     * @param dbOptions the comma-separated string containing database options in "key=value" format
     * @throws IllegalArgumentException if {@code dbOptions} is null or empty
     */
    private void parseAndApplyOptions(StudyImpl study, String dbOptions) {
        if (dbOptions == null || dbOptions.isEmpty()) {
            throw new IllegalArgumentException("Database options are empty");
        }

        val options = Arrays.stream(dbOptions.split(","))
                .map(s -> s.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> arr[0].trim().toUpperCase(),
                        arr -> arr[1].trim()
                ));

        options.forEach((key, value) -> applySingleOption(study, key, value));
    }

    private void applySingleOption(StudyImpl study, String key, String value) {
        // switch-case for extensibility
        switch (key) {
            case "OPT":
                handleParticipationOption(study, value);
                break;
            default:
                break;
        }
    }

    private void handleParticipationOption(StudyImpl study, String value) {
        boolean optIn = value.contains("I");
        boolean optOut = value.contains("O");

        if (optIn && optOut) {
            throw new IllegalArgumentException("Participation option cannot be both opt-in and opt-out");
        }
        study.setParticipation(optIn ? Participation.OptIn : Participation.OptOut);
    }

}
