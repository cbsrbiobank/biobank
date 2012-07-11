package edu.ualberta.med.biobank.common.action.csvimport;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.Unique;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.CommonBundle;
import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.BooleanResult;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.action.exception.CsvImportException;
import edu.ualberta.med.biobank.common.action.exception.CsvImportException.ImportError;
import edu.ualberta.med.biobank.i18n.Bundle;
import edu.ualberta.med.biobank.i18n.LString;
import edu.ualberta.med.biobank.i18n.LocalizedException;
import edu.ualberta.med.biobank.i18n.Tr;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.PermissionEnum;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.model.util.RowColPos;
import edu.ualberta.med.biobank.util.CompressedReference;

/**
 * This action takes a CSV file as input and import the specimens contained in
 * the file.
 * 
 * @author loyola
 * 
 */
@SuppressWarnings("nls")
public class SpecimenCsvImportAction implements Action<BooleanResult> {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory
        .getLogger(SpecimenCsvImportAction.class.getName());

    private static final I18n i18n = I18nFactory
        .getI18n(SpecimenCsvImportAction.class);

    private static final Bundle bundle = new CommonBundle();

    public static final int MAX_ERRORS_TO_REPORT = 50;

    public static final String CSV_PARSE_ERROR =
        "Parse error at line {0}\n{1}";

    public static final LString CSV_FILE_ERROR =
        bundle.tr("CVS file not loaded").format();

    public static final LString CSV_UNCOMPRESS_ERROR =
        bundle.tr("CVS file could not be uncompressed").format();

    public static final Tr CSV_PATIENT_ERROR =
        bundle.tr("patient in CSV file with number {0} not exist");

    public static final Tr CSV_PARENT_SPECIMEN_ERROR =
        bundle
            .tr("parent specimen in CSV file with inventory id {0} does not exist");

    public static final Tr CSV_ORIGIN_CENTER_ERROR =
        bundle.tr("origin center in CSV file with name {0} does not exist");

    public static final Tr CSV_CURRENT_CENTER_ERROR =
        bundle.tr("current center in CSV file with name {0} does not exist");

    public static final Tr CSV_SPECIMEN_TYPE_ERROR =
        bundle.tr("specimen type in CSV file with name {0} does not exist");

    public static final Tr CSV_CONTAINER_LABEL_ERROR =
        bundle.tr("container in CSV file with label {0} does not exist");

    public static final Tr CSV_SPECIMEN_LABEL_ERROR =
        bundle.tr("specimen position in CSV file with label {0} is invalid");

    // @formatter:off
    private static final CellProcessor[] PROCESSORS = new CellProcessor[] {
        new Unique(),                       // "inventoryId",
        new Optional(),                     // "parentInventoryID",
        null,                               // "specimenType",
        new ParseDate("yyyy-MM-dd HH:mm"),  // "createdAt",
        null,                               // "patientNumber",
        new ParseInt(),                     // "visitNumber",
        null,                               // "currentCenter",
        null,                               // "originCenter",
        new ParseBool(),                    // "sourceSpecimen",
        new Optional(),                     // "worksheet",
        new Optional(),                     // "palletProductBarcode",
        new Optional(),                     // "rootContainerType",
        new Optional(),                     // "palletLabel",
        new Optional()                      // "palletPosition"
    }; 
    // @formatter:on    

    private CompressedReference<ArrayList<SpecimenCsvInfo>> compressedList =
        null;

    private ActionContext context = null;

    private final Set<SpecimenImportInfo> specimenImportInfos =
        new HashSet<>(0);

    private final Map<String, SpecimenImportInfo> sourceSpcInvIds =
        new HashMap<>(0);

    private final Map<String, Specimen> sourceSpecimens =
        new HashMap<>(0);

    private final Set<ImportError> errors = new HashSet<>(0);

    public SpecimenCsvImportAction(String filename) throws IOException {
        setCsvFile(filename);
    }

    private void setCsvFile(String filename) throws IOException {
        ICsvBeanReader reader = new CsvBeanReader(
            new FileReader(filename), CsvPreference.EXCEL_PREFERENCE);

        final String[] header = new String[] {
            "inventoryId",
            "parentInventoryID",
            "specimenType",
            "createdAt",
            "patientNumber",
            "visitNumber",
            "currentCenter",
            "originCenter",
            "sourceSpecimen",
            "worksheet",
            "palletProductBarcode",
            "rootContainerType",
            "palletLabel",
            "palletPosition"
        };

        try {
            ArrayList<SpecimenCsvInfo> specimenCsvInfos =
                new ArrayList<SpecimenCsvInfo>(0);

            SpecimenCsvInfo specimenCsvInfo;
            reader.getCSVHeader(true);
            while ((specimenCsvInfo =
                reader.read(SpecimenCsvInfo.class, header, PROCESSORS)) != null) {

                if ((specimenCsvInfo.getPalletLabel() != null)
                    && (specimenCsvInfo.getPalletPosition() == null)) {
                    throw new IllegalStateException(
                        i18n.tr(
                            "line {0}: pallet label defined but not position",
                            reader.getLineNumber()));
                }

                if ((specimenCsvInfo.getPalletLabel() == null)
                    && (specimenCsvInfo.getPalletPosition() != null)) {
                    throw new IllegalStateException(
                        i18n.tr(
                            "line {0}: pallet position defined but not label",
                            reader.getLineNumber()));
                }

                specimenCsvInfo.setLineNumber(reader.getLineNumber());
                specimenCsvInfos.add(specimenCsvInfo);
            }

            compressedList =
                new CompressedReference<ArrayList<SpecimenCsvInfo>>(
                    specimenCsvInfos);

        } catch (SuperCSVException e) {
            throw new IllegalStateException(
                i18n.tr(CSV_PARSE_ERROR, e.getMessage(), e.getCsvContext()));
        } finally {
            reader.close();
        }
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return PermissionEnum.LEGACY_IMPORT_CSV.isAllowed(context.getUser());
    }

    @Override
    public BooleanResult run(ActionContext context) throws ActionException {
        if (compressedList == null) {
            throw new LocalizedException(CSV_FILE_ERROR);
        }

        this.context = context;
        boolean result = false;

        ArrayList<SpecimenCsvInfo> specimenCsvInfos = compressedList.get();
        this.context.getSession().getTransaction();

        for (SpecimenCsvInfo csvInfo : specimenCsvInfos) {
            SpecimenImportInfo info = getDbInfo(csvInfo);
            specimenImportInfos.add(info);

            if (!info.isAliquotedSpecimen()) {
                sourceSpcInvIds.put(csvInfo.getParentInventoryID(), info);
            }
        }

        // find aliquoted specimens and ensure the source specimen is listed in
        // the CSV file
        for (SpecimenImportInfo info : specimenImportInfos) {
            if (!info.isAliquotedSpecimen()) continue;

            // if this specimen has a parent specimen that is not in DB,
            // is it in the CSV data?
            SpecimenImportInfo parentInfo =
                sourceSpcInvIds.get(info.getParentInventoryID());
            if ((info.getPatient() == null) && (parentInfo == null)) {
                addError(info.getLineNumber(),
                    CSV_PARENT_SPECIMEN_ERROR.format(info
                        .getParentInventoryID()));
            } else {
                info.setParentInfo(parentInfo);
            }
        }

        if (!errors.isEmpty()) {
            throw new CsvImportException(errors);
        }

        for (SpecimenImportInfo info : specimenImportInfos) {
            // add all source specimens first
            if (info.isAliquotedSpecimen()) continue;
            Specimen spc = addSpecimen(info);
            sourceSpecimens.put(spc.getInventoryId(), spc);
        }

        for (SpecimenImportInfo info : specimenImportInfos) {
            // now add aliquoted specimens
            if (!info.isAliquotedSpecimen()) continue;
            Specimen parentSpc =
                sourceSpecimens.get(info.getParentInventoryID());
            info.setParentSpecimen(parentSpc);
            addSpecimen(info);
        }

        result = true;
        return new BooleanResult(result);
    }

    // get referenced items that exist in the database
    private SpecimenImportInfo getDbInfo(SpecimenCsvInfo csvInfo) {
        SpecimenImportInfo info = new SpecimenImportInfo(csvInfo);

        Patient patient = loadPatient(csvInfo.getPatientNumber());
        info.setPatient(patient);

        log.debug("finding collection event: pt={} numCevents={}",
            csvInfo.getPatientNumber(), patient.getCollectionEvents().size());

        if (!info.isAliquotedSpecimen()) {
            // find the collection event for this specimen
            for (CollectionEvent ce : patient.getCollectionEvents()) {
                if (ce.getVisitNumber().equals(csvInfo.getVisitNumber())) {
                    info.setCevent(ce);
                    break;
                }
            }
        } else {
            Specimen parentSpecimen =
                getSpecimen(csvInfo.getParentInventoryID());
            if (parentSpecimen != null)
                info.setParentSpecimen(parentSpecimen);
        }

        Center originCenter = getCenter(csvInfo.getOriginCenter());
        if (originCenter == null) {
            addError(csvInfo.getLineNumber(),
                CSV_ORIGIN_CENTER_ERROR.format(csvInfo.getOriginCenter()));
        } else {
            info.setOriginCenter(originCenter);
        }

        Center currentCenter = getCenter(csvInfo.getCurrentCenter());
        if (originCenter == null) {
            addError(csvInfo.getLineNumber(),
                CSV_CURRENT_CENTER_ERROR.format(csvInfo.getCurrentCenter()));
        } else {
            info.setCurrentCenter(currentCenter);
        }

        SpecimenType spcType = getSpecimenType(csvInfo.getSpecimenType());
        if (spcType == null) {
            addError(csvInfo.getLineNumber(),
                CSV_SPECIMEN_TYPE_ERROR.format(csvInfo.getSpecimenType()));
        } else {
            info.setSpecimenType(spcType);
        }

        Container container = getContainer(csvInfo.getPalletLabel());
        if (container == null) {
            addError(csvInfo.getLineNumber(),
                CSV_CONTAINER_LABEL_ERROR.format(csvInfo.getPalletLabel()));
        } else {
            info.setContainer(container);
        }

        try {
            RowColPos pos = container.getPositionFromLabelingScheme(csvInfo
                .getPalletPosition());
            info.setSpecimenPos(pos);
        } catch (Exception e) {
            addError(csvInfo.getLineNumber(),
                CSV_SPECIMEN_LABEL_ERROR.format(csvInfo.getPalletLabel()));
        }

        return info;
    }

    private Specimen addSpecimen(SpecimenImportInfo info) {
        if (context == null) {
            throw new IllegalStateException(
                "should only be called once the context is initialized");
        }

        CollectionEvent cevent = info.getCevent();
        boolean ceventCreated = false;

        if (cevent == null) {
            cevent = info.createCollectionEvent();
            ceventCreated = true;
        }

        Specimen spc = info.getSpecimen();
        context.getSession().save(spc);

        if (ceventCreated) {
            context.getSession().saveOrUpdate(cevent);
        }

        log.debug("added collection event: pt={} v#={} invId={}",
            new Object[] {
                info.getCsvInfo().getPatientNumber(),
                info.getCsvInfo().getVisitNumber(),
                info.getCsvInfo().getInventoryId()
            });

        return spc;
    }

    private void addError(int lineNumber, LString message)
        throws CsvImportException {
        ImportError importError = new ImportError(lineNumber, message);
        errors.add(importError);
        if (errors.size() > MAX_ERRORS_TO_REPORT) {
            throw new CsvImportException(errors);
        }

    }

    private Patient getPatient(String pnumber) {
        Criteria c = context.getSession()
            .createCriteria(Patient.class, "p")
            .add(Restrictions.eq("pnumber", pnumber));

        return (Patient) c.uniqueResult();
    }

    /*
     * Generates an action exception if patient does not exist.
     */
    private Patient loadPatient(String pnumber) {
        // make sure patient exists
        Patient p = getPatient(pnumber);
        if (p == null) {
            throw new LocalizedException(CSV_PATIENT_ERROR.format(pnumber));
        }
        return p;
    }

    /*
     * Generates an action exception if specimen with inventory ID does not
     * exist.
     */
    private Specimen getSpecimen(String inventoryId) {
        Criteria c = context.getSession()
            .createCriteria(Specimen.class, "s")
            .add(Restrictions.eq("inventoryId",
                inventoryId));

        return (Specimen) c.uniqueResult();
    }

    /*
     * Generates an action exception if specimen type does not exist.
     */
    private SpecimenType getSpecimenType(String name) {
        Criteria c = context.getSession()
            .createCriteria(SpecimenType.class, "st")
            .add(Restrictions.eq("name", name));

        return (SpecimenType) c.uniqueResult();
    }

    /*
     * Generates an action exception if centre with name does not exist.
     */
    private Center getCenter(String name) {
        Criteria c = context.getSession()
            .createCriteria(Center.class, "c")
            .add(Restrictions.eq("nameShort", name));

        return (Center) c.uniqueResult();
    }

    /*
     * Generates an action exception if container label does not exist.
     */
    private Container getContainer(String label) {
        Criteria c = context.getSession()
            .createCriteria(Container.class, "c")
            .add(Restrictions.eq("label", label));

        return (Container) c.uniqueResult();
    }

}
