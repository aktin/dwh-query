package org.aktin.dwh.optinout;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;


@ApplicationScoped
public class StudyServiceImpl implements StudyService {
	private static final Logger log = Logger.getLogger(StudyServiceImpl.class.getName());

	private StudyRepository repository;

    @Inject
    public StudyServiceImpl(StudyRepository repository) {
        this.repository = repository;
    }

	@Override
	public List<StudyImpl> getStudies() throws IOException {
        try {
            return repository.getStudies();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
