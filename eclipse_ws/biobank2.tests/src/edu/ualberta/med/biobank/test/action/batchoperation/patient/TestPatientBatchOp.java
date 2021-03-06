package edu.ualberta.med.biobank.test.action.batchoperation.patient;

import java.io.File;
import java.util.Date;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ualberta.med.biobank.common.action.batchoperation.BatchOpGetResult;
import edu.ualberta.med.biobank.common.action.batchoperation.patient.PatientBatchOpAction;
import edu.ualberta.med.biobank.common.action.batchoperation.patient.PatientBatchOpGetAction;
import edu.ualberta.med.biobank.common.action.batchoperation.patient.PatientBatchOpInputPojo;
import edu.ualberta.med.biobank.common.action.exception.BatchOpErrorsException;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.test.action.TestAction;
import edu.ualberta.med.biobank.test.action.batchoperation.CsvUtil;

/**
 *
 * @author Nelson Loyola
 *
 */
@SuppressWarnings("nls")
public class TestPatientBatchOp extends TestAction {

    private static Logger log = LoggerFactory.getLogger(TestPatientBatchOp.class);

    private static final String CSV_NAME = "import_patients.csv";

    private static Date DEFAULT_ENROLLMENT_DATE = DateFormatter.parseToDate("1970-01-01");

    private PatientBatchOpPojoHelper patientPojoHelper;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        patientPojoHelper = new PatientBatchOpPojoHelper(factory.getNameGenerator());

        // delete the CSV file if it exists
        File file = new File(CSV_NAME);
        file.delete();

        session.beginTransaction();
        factory.createSite();
        factory.createStudy();
    }

    @Test
    public void noErrors() throws Exception {
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            factory.getDefaultStudy().getNameShort(), 100);
        patientPojoHelper.addComments(pojos);
        PatientCsvWriter.write(CSV_NAME, pojos);

        try {
            PatientBatchOpAction importAction = new PatientBatchOpAction(
                factory.getDefaultSite(), pojos, new File(CSV_NAME), false);
            exec(importAction);
        } catch (BatchOpErrorsException e) {
            CsvUtil.showErrorsInLog(log, e);
            Assert.fail("errors in CVS data: " + e.getMessage());
        }

        checkPojosAgainstDb(pojos);
    }

    @Test
    public void badStudyName() throws Exception {
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            "invalid_study_name", 1);
        PatientCsvWriter.write(CSV_NAME, pojos);

        // try with a study that does not exist
        try {
            PatientBatchOpAction importAction = new PatientBatchOpAction(
                factory.getDefaultSite(), pojos, new File(CSV_NAME), false);
            exec(importAction);
            Assert.fail("should not be allowed to import with a study name that does not exist");
        } catch (BatchOpErrorsException e) {
            // do nothing
        }
    }

    @Test
    public void existingPatient() throws Exception {
        Patient patient = factory.createPatient();
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            "invalid_study_name", 1);

        // give the pojo the same patient number as the patient added above
        for (PatientBatchOpInputPojo pojo : pojos) {
            pojo.setPatientNumber(patient.getPnumber());
        }
        PatientCsvWriter.write(CSV_NAME, pojos);

        // try with a study that does not exist
        try {
            PatientBatchOpAction importAction = new PatientBatchOpAction(
                factory.getDefaultSite(), pojos, new File(CSV_NAME), false);
            exec(importAction);
            Assert.fail("should not be allowed to import an existing patient");
        } catch (BatchOpErrorsException e) {
            // do nothing
        }

    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void existingPatientIgnoreDuplicates() throws Exception {
        Patient patient = factory.createPatient();
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            factory.getDefaultStudy().getNameShort(), 1);

        // give the pojo the same patient number as the patient added above
        for (PatientBatchOpInputPojo pojo : pojos) {
            pojo.setPatientNumber(patient.getPnumber());
        }
        PatientCsvWriter.write(CSV_NAME, pojos);

        PatientBatchOpAction importAction = new PatientBatchOpAction(
            factory.getDefaultSite(), pojos, new File(CSV_NAME), true);
        exec(importAction);
    }

    @Test
    public void noEnrollmentDate() throws Exception {
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            factory.getDefaultStudy().getNameShort(), 1);

        for (PatientBatchOpInputPojo pojo : pojos) {
            pojo.setEnrollmentDate(null);
        }
        PatientCsvWriter.write(CSV_NAME, pojos);

        // try with a study that does not exist
        try {
            PatientBatchOpAction importAction = new PatientBatchOpAction(
                factory.getDefaultSite(), pojos, new File(CSV_NAME), false);
            exec(importAction);
        } catch (BatchOpErrorsException e) {
            CsvUtil.showErrorsInLog(log, e);
            Assert.fail("errors in CVS data: " + e.getMessage());
        }

        checkPojosAgainstDb(pojos);

    }

    private void checkPojosAgainstDb(Set<PatientBatchOpInputPojo> pojos) {
        for (PatientBatchOpInputPojo pojo : pojos) {
            Criteria c = session.createCriteria(Patient.class, "p")
                .add(Restrictions.eq("pnumber", pojo.getPatientNumber()));
            Patient patient = (Patient) c.uniqueResult();

            Assert.assertEquals(pojo.getStudyName(), patient.getStudy().getNameShort());

            if (pojo.getEnrollmentDate() == null) {
                Assert.assertEquals(DEFAULT_ENROLLMENT_DATE, patient.getCreatedAt());
            } else {
                Assert.assertEquals(pojo.getEnrollmentDate(), patient.getCreatedAt());
            }

            if ((pojo.getComment() != null) && !pojo.getComment().isEmpty()) {
                Assert.assertEquals(1, patient.getComments().size());
                Assert.assertEquals(pojo.getComment(),
                    patient.getComments().iterator().next().getMessage());

            }
        }
    }

    @Test
    public void patientBatchOpGetAction() throws Exception {
        session.getTransaction().commit();

        Set<PatientBatchOpInputPojo> pojos = patientPojoHelper.createPatients(
            factory.getDefaultStudy().getNameShort(), 100);
        patientPojoHelper.addComments(pojos);
        PatientCsvWriter.write(CSV_NAME, pojos);

        PatientBatchOpAction importAction = new PatientBatchOpAction(
            factory.getDefaultSite(), pojos, new File(CSV_NAME), false);
        Integer batchOpId = exec(importAction).getId();

        checkPojosAgainstDb(pojos);

        BatchOpGetResult<Patient> batchOpResult =
            exec(new PatientBatchOpGetAction(batchOpId));

        Assert.assertEquals(pojos.size(), batchOpResult.getModelObjects().size());
    }
}
