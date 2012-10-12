package edu.ualberta.med.biobank.model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestAbstractPosition.class, TestAddress.class,
    TestAliquotedSpecimen.class, TestCenter.class, TestClinic.class,
    TestCollectionEvent.class, TestComment.class, TestContact.class,
    TestContainer.class, TestContainerPosition.class,
    TestContainerType.class, TestDispatch.class, TestDispatchSpecimen.class,
    TestGroup.class, TestPrincipal.class, TestProcessingEvent.class,
    TestRequestSpecimen.class, TestSite.class, TestSpecimen.class,
    TestSpecimenType.class, TestStudy.class })
public class ModelSuite {
}