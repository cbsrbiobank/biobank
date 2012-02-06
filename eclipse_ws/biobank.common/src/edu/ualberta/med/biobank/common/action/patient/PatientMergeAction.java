package edu.ualberta.med.biobank.common.action.patient;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.Session;

import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.BooleanResult;
import edu.ualberta.med.biobank.common.action.CollectionUtils;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.peer.CollectionEventPeer;
import edu.ualberta.med.biobank.common.permission.patient.PatientMergePermission;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Log;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Specimen;

/**
 * Merge patient2 into patient1.
 * 
 */
public class PatientMergeAction implements Action<BooleanResult> {

    private static final long serialVersionUID = 1L;

    private Integer patient1Id;
    private Integer patient2Id;

    public PatientMergeAction(Integer patient1Id, Integer patient2Id) {
        this.patient1Id = patient1Id;
        this.patient2Id = patient2Id;
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return new PatientMergePermission(patient1Id, patient2Id).isAllowed(
            null);
    }

    @Override
    public BooleanResult run(ActionContext context) throws ActionException {
        // FIXME add checks
        // FIXME logging?

        Patient patient1 = context.load(Patient.class, patient1Id);
        Patient patient2 = context.load(Patient.class, patient2Id);
        if (!patient1.getStudy().equals(patient2.getStudy())) {
            throw new PatientMergeException(
                PatientMergeException.ExceptionTypeEnum.STUDY);
        }

        Collection<CollectionEvent> c1events =
            patient1.getCollectionEventCollection();
        Collection<CollectionEvent> c2events =
            patient2.getCollectionEventCollection();
        if (!c2events.isEmpty()) {
            boolean merged = false;
            // for loop on a different list.
            for (CollectionEvent c2event : new ArrayList<CollectionEvent>(
                c2events)) {
                for (CollectionEvent c1event : c1events) {
                    merged = merge(context.getSession(), c1event, c2event);
                    if (merged)
                        break;
                }
                if (!merged) {
                    // the collection has not been merged, so we can add it
                    // as a new collection event in patient1
                    c1events.add(c2event);
                    c2events.remove(c2event);
                    c2event.setPatient(patient1);
                }
                merged = false;
            }

            context.getSession().saveOrUpdate(patient1);
            
            // flush so deleting the patient realizes its collection events have been removed.
            context.getSession().flush();
            
            context.getSession().delete(patient2);

            // FIXME see how logs should be done properly...
            Log logP2 = new Log();
            logP2.setAction("merge"); //$NON-NLS-1$
            logP2.setPatientNumber(patient2.getPnumber());
            logP2.setDetails(patient2.getPnumber() + " --> " //$NON-NLS-1$
                + patient1.getPnumber());
            logP2.setType("Patient"); //$NON-NLS-1$
            context.getSession().save(logP2);

            Log logP1 = new Log();
            logP1.setAction("merge"); //$NON-NLS-1$
            logP1.setPatientNumber(patient1.getPnumber());
            logP1.setDetails(patient1.getPnumber() + " <-- " //$NON-NLS-1$
                + patient2.getPnumber());
            logP1.setType("Patient"); //$NON-NLS-1$
            context.getSession().save(logP1);
        }

        return new BooleanResult(true);
    }

    /**
     * merge 2 collection event if their visitNumber are identical.
     * 
     * @return true if a merge has been made, otherwise false
     */
    private boolean merge(Session session, CollectionEvent c1event,
        CollectionEvent c2event) {
        if (c1event.getVisitNumber().equals(c2event.getVisitNumber())) {
            Collection<Specimen> c2Origspecs = CollectionUtils.getCollection(
                c2event, CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION);
            Collection<Specimen> c2AllSpecs = CollectionUtils.getCollection(
                c2event, CollectionEventPeer.ALL_SPECIMEN_COLLECTION);

            Collection<Specimen> c1OrigSpecs = CollectionUtils.getCollection(
                c1event, CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION);
            Collection<Specimen> c1AllSpecs = CollectionUtils.getCollection(
                c1event, CollectionEventPeer.ALL_SPECIMEN_COLLECTION);
            for (Specimen spec : c2AllSpecs) {
                if (c2Origspecs.contains(spec)) {
                    spec.setOriginalCollectionEvent(c1event);
                    c1OrigSpecs.add(spec);
                }
                spec.setCollectionEvent(c1event);
                c1AllSpecs.add(spec);
            }
            // because of this and the move of specimens into another cevent, we
            // can't use delete-orphan
            session.delete(c2event);
            return true;
        }
        return false;
    }
}
