package edu.ualberta.med.biobank.treeview.clinicShipment;

import edu.ualberta.med.biobank.common.wrappers.ClinicShipmentWrapper;
import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.admin.ClinicAdapter;

public class ClinicWithShipmentAdapter extends ClinicAdapter {

    public ClinicWithShipmentAdapter(AdapterBase parent,
        ClinicWrapper clinicWrapper) {
        super(parent, clinicWrapper);
    }

    @Override
    public AdapterBase search(Object searchedObject) {
        if (searchedObject instanceof ClinicShipmentWrapper) {
            return getChild((ModelWrapper<?>) searchedObject, true);
        }
        return searchChildren(searchedObject);
    }
}
