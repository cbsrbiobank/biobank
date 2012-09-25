package edu.ualberta.med.biobank.batchoperation.specimen;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrNotNullOrEmpty;
import org.supercsv.cellprocessor.constraint.Unique;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.exception.SuperCSVReflectionException;
import org.supercsv.io.ICsvBeanReader;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.batchoperation.ClientBatchOpErrorsException;
import edu.ualberta.med.biobank.batchoperation.ClientBatchOpInputErrorList;
import edu.ualberta.med.biobank.batchoperation.IBatchOpPojoReader;
import edu.ualberta.med.biobank.batchoperation.specimen.CbsrTecanSpecimenPojoReader.TecanCsvRowPojo;
import edu.ualberta.med.biobank.common.action.batchoperation.IBatchOpInputPojo;
import edu.ualberta.med.biobank.common.action.batchoperation.specimen.SpecimenBatchOpInputPojo;
import edu.ualberta.med.biobank.forms.DecodeImageForm;

/**
 * Reads an OHS TECAN CSV file containing specimen information and returns the file
 * as a list of SpecimenBatchOpInputPojo.
 * 
 * @author Brian Allen
 * 
 */
public class OhsTecanSpecimenPojoReader implements
    IBatchOpPojoReader<SpecimenBatchOpInputPojo> {
    
    private static final I18n i18n = I18nFactory
        .getI18n(DecodeImageForm.class);

    public static final String CSV_TECAN_RACK_ID_MISSING_ERROR =
        i18n.tr("TECAN rack ID missing");

    public static final String CSV_INVENTORY_ID_MISSING_ERROR =
        i18n.tr("inventory ID missing");

    public static final String CSV_TIME_STAMP_MISSING_ERROR =
        i18n.tr("timestamp missing");

    public static final String CSV_TECHNICIAN_ID_MISSING_ERROR =
        i18n.tr("technician ID missing");

    public static final String CSV_BC_SCAN_TYPE_ERROR =
        i18n.tr("specimen type is BC-Scan but " +
            "parent inventory ID does not end in 1 or 2");

    public static final String CSV_SPECIMEN_TYPE_ERROR =
        i18n.tr("specimen type is unknown");

    public static final String CSV_TIME_STAMP_PARSE_ERROR =
        i18n.tr("timestamp format incorrect");

    public static final String CSV_MULTIPLE_TECHNICIANS_ERROR =
        i18n.tr("multiple technicians in same csv file");

    public static final String CSV_MULTIPLE_SOURCE_VOLUMES_ERROR =
        i18n.tr("multiple source volumes for same specimen");

    private static final String CSV_FIRST_HEADER = "TECAN_Rack_ID";

    private static final String TYPE_URINE_PRIMARY = "Urine-Primary";
    private static final String TYPE_URINE_SECONDARY = "Urine-Secondary";
    private static final String TYPE_BUFFY = "Buffy";
    private static final String TYPE_RBC_PRIMARY = "RBC-Primary";
    private static final String TYPE_RBC_SECONDARY = "RBC-Secondary";
    private static final String TYPE_SERUM_PRIMARY = "Serum-Primary";
    private static final String TYPE_SERUM_SECONDARY = "Serum-Secondary";
    private static final String TYPE_PLASMA_PRIMARY = "Plasma-Primary";
    private static final String TYPE_PLASMA_SECONDARY = "Plasma-Secondary";
    private static final String TYPE_BC_SCAN = "BC-Scan";
    private static final String TYPE_PRIMARY_MATRIX = "Primary Matrix";
    private static final String TYPE_SECONDARY_MATRIX = "Secondary Matrix";
    private static final String[] ALIQUOT_TYPES = new String[] {
        TYPE_URINE_PRIMARY,
        TYPE_URINE_SECONDARY,
        TYPE_BUFFY,
        TYPE_RBC_PRIMARY,
        TYPE_RBC_SECONDARY,
        TYPE_SERUM_PRIMARY,
        TYPE_SERUM_SECONDARY,
        TYPE_PLASMA_PRIMARY,
        TYPE_PLASMA_SECONDARY,
        TYPE_BC_SCAN,
        TYPE_PRIMARY_MATRIX,
        TYPE_SECONDARY_MATRIX
    };
    
    private static final SimpleDateFormat TIME_STAMP_FORMAT =
        new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    private final List<SpecimenBatchOpInputPojo> sourceSpecimens =
        new ArrayList<SpecimenBatchOpInputPojo>(0);

    @SuppressWarnings("unused")
    public static class TecanCsvRowPojo implements IBatchOpInputPojo {
        private static final long serialVersionUID = 1L;

        int lineNumber;
        String tecanRackId;
        String inventoryId;
        String dontCare2;
        String sourceId;
        String dontCare4;
        String dontCare5;
        String aliquotVolume;
        String sourceVolume;
        String timeStamp;
        String technicianId;
        String dontCare10;
        String dontCare11;
        String plateErrors;
        String sampleErrors;
        String dontCare14;
        String dontCare15;

        @Override
        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getTecanRackId() {
            return tecanRackId;
        }

        public void setTecanRackId(String tecanRackId) {
            this.tecanRackId = tecanRackId;
        }

        public String getInventoryId() {
            return inventoryId;
        }

        public void setInventoryId(String inventoryId) {
            this.inventoryId = inventoryId;
        }

        public String getDontCare2() {
            return dontCare2;
        }

        public void setDontCare2(String dontCare2) {
            this.dontCare2 = dontCare2;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getDontCare4() {
            return dontCare4;
        }

        public void setDontCare4(String dontCare4) {
            this.dontCare4 = dontCare4;
        }

        public String getDontCare5() {
            return dontCare5;
        }

        public void setDontCare5(String dontCare5) {
            this.dontCare5 = dontCare5;
        }

        public String getAliquotVolume() {
            return aliquotVolume;
        }

        public void setAliquotVolume(String aliquotVolume) {
            this.aliquotVolume = aliquotVolume;
        }

        public String getSourceVolume() {
            return sourceVolume;
        }

        public void setSourceVolume(String sourceVolume) {
            this.sourceVolume = sourceVolume;
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getTechnicianId() {
            return technicianId;
        }

        public void setTechnicianId(String technicianId) {
            this.technicianId = technicianId;
        }

        public String getDontCare10() {
            return dontCare10;
        }

        public void setDontCare10(String dontCare10) {
            this.dontCare10 = dontCare10;
        }

        public String getDontCare11() {
            return dontCare11;
        }

        public void setDontCare11(String dontCare11) {
            this.dontCare11 = dontCare11;
        }

        public String getPlateErrors() {
            return plateErrors;
        }

        public void setPlateErrors(String plateErrors) {
            this.plateErrors = plateErrors;
        }

        public String getSampleErrors() {
            return sampleErrors;
        }

        public void setSampleErrors(String sampleErrors) {
            this.sampleErrors = sampleErrors;
        }

        public String getDontCare14() {
            return dontCare14;
        }

        public void setDontCare14(String dontCare14) {
            this.dontCare14 = dontCare14;
        }

        public String getDontCare15() {
            return dontCare15;
        }

        public void setDontCare15(String dontCare15) {
            this.dontCare15 = dontCare15;
        }
    }

    private static final String[] NAME_MAPPINGS = new String[] {
        "tecanRackId",
        "inventoryId",
        "dontCare2",
        "sourceId",
        "dontCare4",
        "dontCare5",
        "aliquotVolume",
        "sourceVolume",
        "timeStamp",
        "technicianId",
        "dontCare10",
        "dontCare11",
        "plateErrors",
        "sampleErrors",
        "dontCare14",
        "dontCare15"
    };
    
    private ICsvBeanReader reader;

    private final ClientBatchOpInputErrorList errorList =
        new ClientBatchOpInputErrorList();

    public OhsTecanSpecimenPojoReader() {

    }

    @Override
    public void setReader(ICsvBeanReader reader) {
        this.reader = reader;
    }

    // cell processors have to be recreated every time the file is read
    public CellProcessor[] getCellProcessors() {

        Map<String, CellProcessor> aMap =
            new LinkedHashMap<String, CellProcessor>();
        
        aMap.put("tecanRackId", new StrNotNullOrEmpty());
        aMap.put("inventoryId", null);
        aMap.put("dontCare2", null);
        aMap.put("sourceId", null);
        aMap.put("dontCare4", null);
        aMap.put("dontCare5", null);
        aMap.put("aliquotVolume", null);
        aMap.put("sourceVolume", null);
        aMap.put("timeStamp", null);
        aMap.put("technicianId", null);
        aMap.put("dontCare10", null);
        aMap.put("dontCare11", null);
        aMap.put("plateErrors", null);
        aMap.put("sampleErrors", null);
        aMap.put("dontCare14", null);
        aMap.put("dontCare15", null);

        if (aMap.size() != NAME_MAPPINGS.length) {
            throw new IllegalStateException(
                "the number of name mappings do not match the cell processors");
        }

        return aMap.values().toArray(new CellProcessor[0]);
    }

    @Override
    public ClientBatchOpInputErrorList getErrorList() {
        return errorList;
    }

    public static boolean isHeaderValid(String[] csvHeaders) {
        if (csvHeaders == null) {
            throw new NullPointerException("csvHeaders is null");
        }
        return csvHeaders[0].equals(CSV_FIRST_HEADER)
            // last 2 columns have no column name
            && (csvHeaders.length + 2 == NAME_MAPPINGS.length);
    }

    @Override
    public List<SpecimenBatchOpInputPojo> getPojos()
        throws ClientBatchOpErrorsException, IOException {
        if (reader == null) {
            throw new IllegalStateException("CSV reader is null");
        }

        CellProcessor[] cellProcessors = getCellProcessors();

        List<SpecimenBatchOpInputPojo> result =
            new ArrayList<SpecimenBatchOpInputPojo>();

        TecanCsvRowPojo csvPojo;

        try {
            boolean hasBuffy = false;
            String technicianId = null;
            while ((csvPojo =
                reader.read(TecanCsvRowPojo.class,
                    NAME_MAPPINGS, cellProcessors)) != null) {
                
                // skip certain rows
                if (csvPojo.getSourceId().isEmpty()) continue;
                if (csvPojo.getAliquotVolume().isEmpty()) continue;
                if (Double.parseDouble(csvPojo.aliquotVolume)/1000.0 <= 0.0) continue;

                csvPojo.setLineNumber(reader.getLineNumber());

                SpecimenBatchOpInputPojo batchOpPojo =
                    convertToSpecimenBatchOpInputPojo(csvPojo);

                if (batchOpPojo == null) {
                    // pojo could not be converted, ignore this row
                    continue;
                }
                
                if (batchOpPojo.getSpecimenType().equals(TYPE_BUFFY)) {
                    hasBuffy = true;
                }
                
                // technician ID for processing event
                if (technicianId == null) {
                    technicianId = csvPojo.technicianId;
                }
                else if (!technicianId.equals(csvPojo.technicianId)) {
                    getErrorList().addError(reader.getLineNumber(),
                        CSV_MULTIPLE_TECHNICIANS_ERROR);
                }
                
                // deal with source specimens
                // urine source volume missing by design
                BigDecimal sourceVolume = null;
                if (!csvPojo.sourceVolume.isEmpty()) {
                    sourceVolume = new BigDecimal(Double.parseDouble(csvPojo.sourceVolume)/1000.0);
                }

                boolean sourceSpecimenAlreadyEncountered = false;
                for (SpecimenBatchOpInputPojo sPojo : sourceSpecimens) {
                    if (sPojo.getInventoryId().equals(csvPojo.sourceId)) {
                        if ((sPojo.getVolume() == null && sourceVolume != null)
                            || (sPojo.getVolume() != null && sourceVolume == null)
                            || ((sPojo.getVolume() != null && sourceVolume != null)
                                && (sPojo.getVolume().compareTo(sourceVolume) != 0))) {
                            getErrorList().addError(reader.getLineNumber(),
                                CSV_MULTIPLE_SOURCE_VOLUMES_ERROR);
                        }
                        sourceSpecimenAlreadyEncountered = true;
                        break;
                    }
                }
                if (!sourceSpecimenAlreadyEncountered) {
                    SpecimenBatchOpInputPojo sourcePojo = new SpecimenBatchOpInputPojo();
                    sourcePojo.setInventoryId(csvPojo.sourceId);
                    sourcePojo.setVolume(sourceVolume);
                    sourceSpecimens.add(sourcePojo);
                }

                result.add(batchOpPojo);
            }
            
            // fix specimen type for certain csv files
            if (hasBuffy) {
                for (SpecimenBatchOpInputPojo pojo : result) {
                    if (pojo.getSpecimenType() == TYPE_SERUM_PRIMARY) {
                        pojo.setSpecimenType(TYPE_PLASMA_PRIMARY);
                    }
                    else if (pojo.getSpecimenType() == TYPE_SERUM_SECONDARY) {
                        pojo.setSpecimenType(TYPE_PLASMA_SECONDARY);
                    }
                }
            }
                
            return result;
        } catch (SuperCSVReflectionException e) {
            throw new ClientBatchOpErrorsException(e);
        } catch (SuperCSVException e) {
            throw new ClientBatchOpErrorsException(e);
        }
    }

    private SpecimenBatchOpInputPojo convertToSpecimenBatchOpInputPojo(
        TecanCsvRowPojo csvPojo) {
        SpecimenBatchOpInputPojo batchOpPojo = new SpecimenBatchOpInputPojo();
        batchOpPojo.setLineNumber(csvPojo.getLineNumber());
        
        // check for missing column values
        if (csvPojo.tecanRackId == null) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_TECAN_RACK_ID_MISSING_ERROR);
            return null;
        }
        if (csvPojo.inventoryId == null) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_INVENTORY_ID_MISSING_ERROR);
            return null;
        }
        if (csvPojo.timeStamp == null) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_TIME_STAMP_MISSING_ERROR);
            return null;
        }
        if (csvPojo.technicianId == null) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_TECHNICIAN_ID_MISSING_ERROR);
            return null;
        }
        
        // process other columns
        
        // deal with specimen type
        String specimenType = null;
        for (String sType : ALIQUOT_TYPES) {
            if (csvPojo.tecanRackId.startsWith(sType)) {
                specimenType = sType;
                if (specimenType == TYPE_PRIMARY_MATRIX) {
                    specimenType = TYPE_SERUM_PRIMARY;
                }
                else if (specimenType == TYPE_SECONDARY_MATRIX) {
                    specimenType = TYPE_SERUM_SECONDARY;
                }
                else if (specimenType == TYPE_BC_SCAN) {
                    if (csvPojo.sourceId.endsWith("1")) {
                        specimenType = TYPE_SERUM_PRIMARY;
                    }
                    else if (csvPojo.sourceId.endsWith("2")) {
                        specimenType = TYPE_SERUM_SECONDARY;
                    }
                    else {
                        getErrorList().addError(reader.getLineNumber(),
                            CSV_BC_SCAN_TYPE_ERROR);
                        return null;
                    }
                }
                break;
            }
        }
        if (specimenType == null) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_SPECIMEN_TYPE_ERROR);
            return null;
        }
        batchOpPojo.setSpecimenType(specimenType);
        
        // deal with inventory ID
        batchOpPojo.setInventoryId(csvPojo.inventoryId);
        
        // deal with source ID
        batchOpPojo.setParentInventoryId(csvPojo.sourceId);
        
        // deal with aliquot volume
        batchOpPojo.setVolume(new BigDecimal(Double.parseDouble(csvPojo.aliquotVolume)/1000.0));
        
        // deal with timestamp
        String timeStamp = csvPojo.timeStamp;
        // at one point, timestamps were being prefixed with an underscore
        if (timeStamp.startsWith("_")) {
            timeStamp = timeStamp.substring(1);
        }
        try {
            batchOpPojo.setCreatedAt(TIME_STAMP_FORMAT.parse(timeStamp));
        }
        catch (ParseException pe) {
            getErrorList().addError(reader.getLineNumber(),
                CSV_TIME_STAMP_PARSE_ERROR);
            return null;
        }
        
        // deal with plate errors
        batchOpPojo.setPlateErrors(csvPojo.plateErrors);
        
        // deal with sample errors
        batchOpPojo.setSamplEerrors(csvPojo.sampleErrors);
        
        return batchOpPojo;
    }     

    @Override
    public void preExecution() {
        // TODO Auto-generated method stub

    }

    @Override
    public void postExecution() {
        // TODO Auto-generated method stub

    }

}