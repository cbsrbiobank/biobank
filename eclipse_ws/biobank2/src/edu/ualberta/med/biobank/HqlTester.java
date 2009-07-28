package edu.ualberta.med.biobank;

import edu.ualberta.med.biobank.model.ContainerType;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.client.ApplicationServiceProvider;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

import java.util.Arrays;
import java.util.List;

public class HqlTester {

    private static WritableApplicationService appService;

    public static void main(String[] args) throws Exception {
        new HqlTester();
    }

    public HqlTester() throws Exception {
        appService = (WritableApplicationService) ApplicationServiceProvider
            .getApplicationServiceFromUrl("http://localhost:8080/biobank2",
                "testuser", "test");

        geTopContainerTypes();
        // getPatientIds();
    }

    @SuppressWarnings("unused")
    private void getPatientIds() throws Exception {
        HQLCriteria c = new HQLCriteria("select patients.id"
            + " from edu.ualberta.med.biobank.model.Patient as patients"
            + " inner join patients.study as study"
            + " where study.nameShort=?");

        c.setParameters(Arrays.asList(new Object[] { "BBP" }));

        List<Integer> results = appService.query(c);
        for (Integer id : results) {
            System.out.println("getPatientIds: id: " + id);
        }
    }

    private void geTopContainerTypes() throws Exception {
        HQLCriteria c = new HQLCriteria(
            "from edu.ualberta.med.biobank.model.ContainerType as cttop"
                + " where cttop.id not in (select child.id"
                + " from edu.ualberta.med.biobank.model.ContainerType as ct"
                + " left join ct.childContainerTypeCollection as child "
                + " where child.id!=null)");

        List<ContainerType> results = appService.query(c);
        for (ContainerType ct : results) {
            System.out.println("geTopContainerTypes: " + ct.getName());
        }
    }

}
