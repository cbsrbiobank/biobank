package edu.ualberta.med.biobank.common.reports;

import edu.ualberta.med.biobank.model.Aliquot;
import edu.ualberta.med.biobank.model.AliquotPosition;

@Deprecated
public class AliquotSCount extends QueryObject {

    protected static final String NAME = "Sample Types by Study";

    public AliquotSCount(String op, Integer siteId) {
        super(
            "Lists the total number of each aliquot sample type by study.",
            "Select Alias.patientVisit.patient.study.nameShort, Alias.sampleType.name, count(*) from "
                + Aliquot.class.getName()
                + " as Alias where Alias.aliquotPosition not in (from "
                + AliquotPosition.class.getName()
                + " a where a.container.label like 'SS%') and Alias.patientVisit.patient.study.site "
                + op
                + siteId
                + " GROUP BY Alias.patientVisit.patient.study.nameShort, Alias.sampleType.name",
            new String[] { "Study", "Sample Type", "Total" });
    }

    @Override
    public String getName() {
        return NAME;
    }
}
