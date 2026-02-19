package org.aktin.dwh.optinout.repository;

import lombok.val;
import org.aktin.dwh.optinout.model.Participation;
import org.aktin.dwh.optinout.model.SICGeneration;
import org.aktin.dwh.optinout.model.Study;
import org.aktin.dwh.optinout.model.StudyImpl;
import org.aktin.dwh.optinout.sic.CodeGeneratorFactory;
import org.aktin.dwh.optinout.util.DataSourceProvider;
import org.aktin.dwh.optinout.util.QueryResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
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
    private DataSourceProvider dsp;

    protected StudyRepository() {
        // required for CDI proxy
    }

    @Inject
    public StudyRepository(DataSourceProvider dsp) {
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

        if(study.getSicGeneration() == SICGeneration.ManualOnly) {
            throw new UnsupportedOperationException("Generation of SICs not supported by this study: " + study.getTitle());

        }
        val codeGenerator = CodeGeneratorFactory.createInstance(study.getSicGenerator(), study.getSicGeneratorState());

        val code = codeGenerator.generateCode();
        // write state
        try (val dbc = dsp.getDataSource().getConnection();
            val ps = dbc.prepareStatement(QueryResolver.SQL_UPDATE_SIC_STATE)) {
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
    public List<Study> getStudies() throws SQLException {
        val list = new ArrayList<Study>();
        try (val dsc = dsp.getDataSource().getConnection();
             val ps = dsc.prepareStatement(QueryResolver.SQL_ALL_STUDIES)) {
            val rs = ps.executeQuery();
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
        try (val dbc = dsp.getDataSource().getConnection();
            val ps = dbc.prepareStatement(QueryResolver.SQL_STUDY_BY_ID)) {
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
        try (val dbc = dsp.getDataSource().getConnection();
            val ps = dbc.prepareStatement(QueryResolver.SQL_INSERT_STUDY)) {
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
     * and applying options to the study. It checks the SIC generation strategy
     *
     * @param rs the ResultSet containing the study data to be converted
     * @return a StudyImpl object populated with data from the ResultSet row
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    private StudyImpl toStudy(ResultSet rs) throws SQLException {
        Instant closedTimestamp = rs.getTimestamp(5) != null ? rs.getTimestamp(5).toInstant() : null;
        val sicGenerator = rs.getString(7);

        val study = new StudyImpl(rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getTimestamp(4).toInstant(),
                closedTimestamp,
                null,
                sicGenerator,
                rs.getString(8));

        // load options
        parseAndApplyOptions(study, rs.getString(6));

        return study;
    }

    /**
     * Parses a comma-separated list of database options and applies them to the specified study.
     * Each option is expected to be in the format "key=value". Invalid options will throw an exception.
     * Unknown options will be ignored.
     *
     * @param study the {@code StudyImpl} instance to which the options will be applied
     * @param options the comma-separated string containing study options in "key=value" format
     * @throws IllegalArgumentException if {@code options} is null or empty
     */
    private void parseAndApplyOptions(StudyImpl study, String options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Database options are empty");
        }

        val optionStrings = Arrays.asList(options.split(","));
        val optionsMap = optionStrings.stream()
                .map(s -> s.split("="))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> arr[0].trim().toUpperCase(),
                        arr -> arr[1].trim()
                ));

        if(optionsMap.size() != optionStrings.size()) {
            val invalidOptions = optionStrings.stream()
                    .filter(s -> !optionsMap.containsKey(s.split("=")[0].trim().toUpperCase()))
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid study options: " + invalidOptions);
        }

        optionsMap.forEach((key, value) -> applySingleOption(study, key, value));
    }

    private void applySingleOption(StudyImpl study, String key, String value) {
        // switch-case for extensibility
        switch (key) {
            case "OPT":
                handleParticipationOption(study, value);
                break;
            default:
                Logger.getLogger(getClass().getName()).warning("Unknown study option: " + key);
        }
    }

    private void handleParticipationOption(StudyImpl study, String value) {
        switch (value) {
             case "I":
                 study.setParticipation(Participation.OptIn);
                 break;
             case "O":
                 study.setParticipation(Participation.OptOut);
                 break;
            default:
                throw new IllegalArgumentException("Invalid participation option: " + value);
        }
    }

}
