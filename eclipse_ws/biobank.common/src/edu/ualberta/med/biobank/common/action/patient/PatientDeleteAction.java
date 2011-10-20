package edu.ualberta.med.biobank.common.action.patient;

import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.check.CollectionIsEmptyCheck;
import edu.ualberta.med.biobank.common.action.check.ValueProperty;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.peer.PatientPeer;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.User;

public class PatientDeleteAction implements Action<Integer> {

    private static final long serialVersionUID = 1L;

    private static final String HAS_COLLECTION_EVENTS_MSG = Messages.getString("PatientDeleteAction.has.collectionevents.msg"); //$NON-NLS-1$

    private Integer patientId;

    public PatientDeleteAction(Integer patientId) {
        this.patientId = patientId;
    }

    @Override
    public boolean isAllowed(User user, Session session) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Integer run(User user, Session session) throws ActionException {
        Patient patient = (Patient) session.load(Patient.class, patientId);

        new CollectionIsEmptyCheck<Patient>(new ValueProperty<Patient>(
            PatientPeer.ID, patientId), Patient.class,
            PatientPeer.COLLECTION_EVENT_COLLECTION, patient.getPnumber(),
            HAS_COLLECTION_EVENTS_MSG).run(user, session);

        session.delete(patient);

        return patientId;
    }
}
