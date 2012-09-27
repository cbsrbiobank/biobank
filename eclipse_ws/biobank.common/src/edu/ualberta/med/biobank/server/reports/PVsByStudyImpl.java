package edu.ualberta.med.biobank.server.reports;

import edu.ualberta.med.biobank.common.reports.BiobankReport;
import edu.ualberta.med.biobank.common.util.AbstractRowPostProcess;
import edu.ualberta.med.biobank.common.util.DateRangeRowPostProcess;
import edu.ualberta.med.biobank.model.ProcessingEvent;

public class PVsByStudyImpl extends AbstractReport {

    private static final String QUERY = "Select Alias.shipmentPatient.patient.study.nameShort, "
        + " Year(Alias.dateProcessed), "
        + GROUPBY_DATE
        + "(Alias.dateProcessed), count(*) from "
        + ProcessingEvent.class.getName()
        + " as Alias where Alias.dateProcessed between ? and ?"
        + " GROUP BY Alias.shipmentPatient.patient.study.nameShort, "
        + "Year(Alias.dateProcessed), "
        + GROUPBY_DATE
        + "(Alias.dateProcessed)";

    private DateRangeRowPostProcess dateRangePostProcess;

    public PVsByStudyImpl(BiobankReport report) {
        super(QUERY, report);
        dateRangePostProcess = new DateRangeRowPostProcess(report.getGroupBy()
            .equals("Year"), 1);
    }

    @Override
    public AbstractRowPostProcess getRowPostProcess() {
        return dateRangePostProcess;
    }

}