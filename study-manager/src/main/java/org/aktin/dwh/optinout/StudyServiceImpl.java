package org.aktin.dwh.optinout;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


@ApplicationScoped
public class StudyServiceImpl implements StudyService {
	private StudyRepository repository;

    @Inject
    public StudyServiceImpl(StudyRepository repository) {
        this.repository = repository;
    }

	@Override
	public List<Study> getStudies() throws IOException {
        try {
            return repository.getStudies();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
