package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.security.User;
import edu.ualberta.med.biobank.common.util.DispatchAliquotState;
import edu.ualberta.med.biobank.model.Dispatch;
import edu.ualberta.med.biobank.model.DispatchAliquot;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Study;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

/**
 * State 0 = Creation; State 1 = In Transit; State 2 = Received; State 3 =
 * Ok/Closed;
 */
public class DispatchWrapper extends AbstractShipmentWrapper<Dispatch> {

    private static final String NON_PROCESSED_ALIQUOTS_KEY = "nonProcessedDispatchAliquotCollection";

    private static final String RECEIVED_ALIQUOTS_KEY = "receivedDispatchAliquots";

    private static final String MISSING_ALIQUOTS_KEY = "missingDispatchAliquots";

    private static final String EXTRA_ALIQUOTS_KEY = "extraDispatchAliquots";

    private static final String PENDING_ERRORS_ALIQUOTS_KEY = "pendingErrorsDispatchAliquots";

    private Set<DispatchAliquotWrapper> deletedDispatchedShipmentAliquots = new HashSet<DispatchAliquotWrapper>();

    private boolean stateModified = false;

    public DispatchWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public DispatchWrapper(WritableApplicationService appService, Dispatch ship) {
        super(appService, ship);
    }

    @Override
    public Class<Dispatch> getWrappedClass() {
        return Dispatch.class;
    }

    @Override
    protected String[] getPropertyChangeNames() {
        String[] properties = super.getPropertyChangeNames();
        List<String> list = new ArrayList<String>(Arrays.asList(properties));
        list.addAll(Arrays.asList("sender", "receiver", "aliquotCollection",
            "study"));
        return list.toArray(new String[] {});
    }

    @Override
    protected void persistChecks() throws BiobankCheckException,
        ApplicationException, WrapperException {
        if (getSender() == null) {
            throw new BiobankCheckException("Sender should be set");
        }
        if (getReceiver() == null) {
            throw new BiobankCheckException("Receiver should be set");
        }
        if (getStudy() == null) {
            throw new BiobankCheckException("Study should be set");
        }
        if (!checkWaybillUniqueForSender()) {
            throw new BiobankCheckException("A dispatch shipment with waybill "
                + getWaybill() + " already exists for sending site "
                + getSender().getNameShort());
        }
        if (isInTransitState() && getDateShipped() == null) {
            throw new BiobankCheckException(
                "Date shipped should be set when this shipment is in transit.");
        }
        checkSenderCanSendToReceiver();
    }

    @Override
    protected void persistDependencies(Dispatch origObject) throws Exception {
        for (DispatchAliquotWrapper dsa : deletedDispatchedShipmentAliquots) {
            if (!dsa.isNew()) {
                dsa.delete();
            }
        }
        if (stateModified && isInTransitState()) {
            // when is sent, need to set aliquots positions to null and to
            // remove containers holding them
            for (AliquotWrapper aliquot : getAliquotCollection()) {
                if (aliquot.getPosition() != null) {
                    ContainerWrapper parent = aliquot.getParent();
                    aliquot.setPosition(null);
                    aliquot.persist();
                    parent.reload();
                    if (!parent.hasAliquots()) {
                        parent.delete();
                    }
                }
            }
        }
    }

    private void checkSenderCanSendToReceiver() throws BiobankCheckException,
        WrapperException {
        if (getSender() != null && getReceiver() != null && getStudy() != null) {
            List<SiteWrapper> possibleReceivers = getSender()
                .getStudyDispachSites(getStudy());
            if (possibleReceivers == null
                || !possibleReceivers.contains(getReceiver())) {
                throw new BiobankCheckException("site "
                    + getSender().getNameShort()
                    + " cannot dispatch aliquots to site "
                    + getReceiver().getNameShort() + " for study "
                    + getStudy().getNameShort());
            }
        }
    }

    private boolean checkWaybillUniqueForSender() throws ApplicationException,
        BiobankCheckException {
        String isSameShipment = "";
        List<Object> params = new ArrayList<Object>();
        SiteWrapper sender = getSender();
        if (sender == null) {
            throw new BiobankCheckException("sender site cannot be null");
        }
        params.add(sender.getId());
        params.add(getWaybill());
        if (!isNew()) {
            isSameShipment = " and id <> ?";
            params.add(getId());
        }
        HQLCriteria c = new HQLCriteria("from " + Dispatch.class.getName()
            + " where sender.id=? and waybill = ?" + isSameShipment, params);

        List<Object> results = appService.query(c);
        return results.size() == 0;
    }

    public SiteWrapper getSender() {
        SiteWrapper sender = (SiteWrapper) propertiesMap.get("sender");
        if (sender == null) {
            Site s = wrappedObject.getSender();
            if (s == null)
                return null;
            sender = new SiteWrapper(appService, s);
            propertiesMap.put("sender", sender);
        }
        return sender;
    }

    public void setSender(SiteWrapper sender) {
        propertiesMap.put("sender", sender);
        Site oldSender = wrappedObject.getSender();
        Site newSender = null;
        if (sender != null) {
            newSender = sender.getWrappedObject();
        }
        wrappedObject.setSender(newSender);
        propertyChangeSupport
            .firePropertyChange("sender", oldSender, newSender);
    }

    public SiteWrapper getReceiver() {
        SiteWrapper receiver = (SiteWrapper) propertiesMap.get("receiver");
        if (receiver == null) {
            Site r = wrappedObject.getReceiver();
            if (r == null)
                return null;
            receiver = new SiteWrapper(appService, r);
            propertiesMap.put("receiver", receiver);
        }
        return receiver;
    }

    public void setReceiver(SiteWrapper receiver) {
        propertiesMap.put("receiver", receiver);
        Site oldReceiver = wrappedObject.getReceiver();
        Site newReceiver = null;
        if (receiver != null) {
            newReceiver = receiver.getWrappedObject();
        }
        wrappedObject.setReceiver(newReceiver);
        propertyChangeSupport.firePropertyChange("receiver", oldReceiver,
            newReceiver);
    }

    public StudyWrapper getStudy() {
        StudyWrapper study = (StudyWrapper) propertiesMap.get("study");
        if (study == null) {
            Study s = wrappedObject.getStudy();
            if (s == null)
                return null;
            study = new StudyWrapper(appService, s);
            propertiesMap.put("study", study);
        }
        return study;
    }

    public void setStudy(StudyWrapper study) {
        propertiesMap.put("study", study);
        Study oldStudy = wrappedObject.getStudy();
        Study newStudy = null;
        if (study != null) {
            newStudy = study.getWrappedObject();
        }
        wrappedObject.setStudy(newStudy);
        propertyChangeSupport.firePropertyChange("study", oldStudy, newStudy);
    }

    @SuppressWarnings("unchecked")
    public List<DispatchAliquotWrapper> getDispatchAliquotCollection(
        boolean sort) {
        List<DispatchAliquotWrapper> aliquotCollection = (List<DispatchAliquotWrapper>) propertiesMap
            .get("dispatchAliquotCollection");
        if (aliquotCollection == null) {
            Collection<DispatchAliquot> children = wrappedObject
                .getDispatchAliquotCollection();
            if (children != null) {
                aliquotCollection = new ArrayList<DispatchAliquotWrapper>();
                for (DispatchAliquot dsa : children) {
                    aliquotCollection.add(new DispatchAliquotWrapper(
                        appService, dsa));
                }
                propertiesMap.put("dispatchAliquotCollection",
                    aliquotCollection);
            }
        }
        if ((aliquotCollection != null) && sort)
            Collections.sort(aliquotCollection);
        return aliquotCollection;
    }

    public List<DispatchAliquotWrapper> getDispatchAliquotCollection() {
        return getDispatchAliquotCollection(true);
    }

    @SuppressWarnings("unchecked")
    private List<DispatchAliquotWrapper> getDispatchAliquotCollectionWithState(
        String mapKey, boolean sort, DispatchAliquotState... states) {
        List<DispatchAliquotWrapper> dsaCollection = (List<DispatchAliquotWrapper>) propertiesMap
            .get(mapKey);
        if (dsaCollection == null) {
            Collection<DispatchAliquotWrapper> children = getDispatchAliquotCollection(sort);
            if (children != null) {
                dsaCollection = new ArrayList<DispatchAliquotWrapper>();
                for (DispatchAliquotWrapper dsa : children) {
                    boolean hasState = false;
                    for (DispatchAliquotState state : states) {
                        if (dsa.getState() == state.ordinal()) {
                            hasState = true;
                            break;
                        }
                    }
                    if (hasState)
                        dsaCollection.add(dsa);
                }
                propertiesMap.put(mapKey, dsaCollection);
            }
        }
        if ((dsaCollection != null) && sort)
            Collections.sort(dsaCollection);
        return dsaCollection;
    }

    public List<DispatchAliquotWrapper> getNonProcessedDispatchAliquotCollection() {
        return getDispatchAliquotCollectionWithState(
            NON_PROCESSED_ALIQUOTS_KEY, true, DispatchAliquotState.NONE_STATE);
    }

    public List<DispatchAliquotWrapper> getReceivedDispatchAliquots() {
        return getDispatchAliquotCollectionWithState(RECEIVED_ALIQUOTS_KEY,
            true, DispatchAliquotState.RECEIVED_STATE);
    }

    @SuppressWarnings("unchecked")
    public List<AliquotWrapper> getAliquotCollection(boolean sort) {
        List<AliquotWrapper> aliquotCollection = (List<AliquotWrapper>) propertiesMap
            .get("aliquotCollection");
        if (aliquotCollection == null) {
            Collection<DispatchAliquotWrapper> dsaList = getDispatchAliquotCollection(sort);
            if (dsaList != null) {
                aliquotCollection = new ArrayList<AliquotWrapper>();
                for (DispatchAliquotWrapper dsa : dsaList) {
                    aliquotCollection.add(dsa.getAliquot());
                }
                propertiesMap.put("aliquotCollection", aliquotCollection);
            }
        }
        return aliquotCollection;
    }

    public List<AliquotWrapper> getAliquotCollection() {
        return getAliquotCollection(true);
    }

    private void setDispathcShipmentAliquotCollection(
        Collection<DispatchAliquot> allDsaObjects,
        List<DispatchAliquotWrapper> allDsaWrappers) {
        Collection<DispatchAliquot> oldList = wrappedObject
            .getDispatchAliquotCollection();
        wrappedObject.setDispatchAliquotCollection(allDsaObjects);
        propertyChangeSupport.firePropertyChange("dispatchAliquotCollection",
            oldList, allDsaObjects);
        propertiesMap.put("dispatchAliquotCollection", allDsaWrappers);
    }

    public void addNewAliquots(List<AliquotWrapper> newAliquots)
        throws BiobankCheckException {
        addAliquots(newAliquots, DispatchAliquotState.NONE_STATE);
    }

    public void addExtraAliquots(List<AliquotWrapper> newAliquots)
        throws BiobankCheckException {
        addAliquots(newAliquots, DispatchAliquotState.EXTRA);
    }

    public void addExtraPendingAliquots(List<AliquotWrapper> newAliquots)
        throws BiobankCheckException {
        addAliquots(newAliquots, DispatchAliquotState.EXTRA_PENDING_STATE);
    }

    private void addAliquots(List<AliquotWrapper> newAliquots,
        DispatchAliquotState stateForAliquot) throws BiobankCheckException {
        if ((newAliquots == null) || (newAliquots.size() == 0))
            return;

        Collection<DispatchAliquot> allDsaObjects = new HashSet<DispatchAliquot>();
        List<DispatchAliquotWrapper> allDsaWrappers = new ArrayList<DispatchAliquotWrapper>();
        // already added dsa
        List<DispatchAliquotWrapper> currentList = getDispatchAliquotCollection();
        List<AliquotWrapper> currentAliquots = new ArrayList<AliquotWrapper>();
        if (currentList != null) {
            for (DispatchAliquotWrapper dsa : currentList) {
                allDsaObjects.add(dsa.getWrappedObject());
                allDsaWrappers.add(dsa);
                currentAliquots.add(dsa.getAliquot());
            }
        }
        // new aliquots added
        for (AliquotWrapper aliquot : newAliquots) {
            CheckStatus check = checkCanAddAliquot(currentAliquots, aliquot);
            if (!check.ok)
                throw new BiobankCheckException(check.message);
            DispatchAliquotWrapper dsa = new DispatchAliquotWrapper(appService);
            dsa.setAliquot(aliquot);
            dsa.setState(stateForAliquot.ordinal());
            dsa.setShipment(this);
            allDsaObjects.add(dsa.getWrappedObject());
            allDsaWrappers.add(dsa);
        }
        setDispathcShipmentAliquotCollection(allDsaObjects, allDsaWrappers);
        resetStateLists();
    }

    public class CheckStatus {
        public CheckStatus(boolean b, String string) {
            this.ok = b;
            this.message = string;
        }

        public boolean ok = true;
        public String message;

    }

    public CheckStatus checkCanAddAliquot(List<AliquotWrapper> currentAliquots,
        AliquotWrapper aliquot) {
        if (aliquot.isNew()) {
            return new CheckStatus(false, "Cannot add aliquot "
                + aliquot.getInventoryId() + ": it has not already been saved");
        }
        if (!aliquot.isActive()) {
            return new CheckStatus(false, "Activity status of "
                + aliquot.getInventoryId() + " is not 'Active'."
                + " Check comments on this aliquot for more information.");
        }
        if (aliquot.getPosition() == null) {
            return new CheckStatus(false, "Cannot add aliquot "
                + aliquot.getInventoryId()
                + ": it has no position. A position should be first assigned.");
        }
        if (!aliquot.getParent().getSite().equals(getSender())) {
            return new CheckStatus(false, "Aliquot " + aliquot.getInventoryId()
                + " is currently assigned to site "
                + aliquot.getParent().getSite().getNameShort()
                + ". It should be first assigned to "
                + getSender().getNameShort() + " site.");
        }
        StudyWrapper aliquotStudy = aliquot.getPatientVisit().getPatient()
            .getStudy();
        if (!aliquotStudy.equals(getStudy())) {
            return new CheckStatus(false, "Aliquot " + aliquot.getInventoryId()
                + " is linked to study " + aliquotStudy.getNameShort()
                + ". The study of this shipment is "
                + getStudy().getNameShort() + ".");
        }
        if (currentAliquots != null && currentAliquots.contains(aliquot)) {
            return new CheckStatus(false, aliquot.getInventoryId()
                + " is already in this shipment.");
        }
        if (aliquot.isUsedInDispatch()) {
            return new CheckStatus(false, aliquot.getInventoryId()
                + " is already in a shipment in transit or in creation.");
        }
        return new CheckStatus(true, "");
    }

    public void removeDispatchAliquots(List<DispatchAliquotWrapper> dsasToRemove) {
        if ((dsasToRemove == null) || (dsasToRemove.size() == 0))
            return;

        Collection<DispatchAliquot> allDsaObjects = new HashSet<DispatchAliquot>();
        List<DispatchAliquotWrapper> allDsaWrappers = new ArrayList<DispatchAliquotWrapper>();
        // already added dsa
        List<DispatchAliquotWrapper> currentList = getDispatchAliquotCollection(false);
        if (currentList != null) {
            for (DispatchAliquotWrapper dsa : currentList) {
                if (!dsasToRemove.contains(dsa)) {
                    allDsaObjects.add(dsa.getWrappedObject());
                    allDsaWrappers.add(dsa);
                } else {
                    deletedDispatchedShipmentAliquots.add(dsa);
                }
            }
        }
        setDispathcShipmentAliquotCollection(allDsaObjects, allDsaWrappers);
        resetStateLists();
    }

    public void removeAliquots(List<AliquotWrapper> aliquotsToRemove) {
        if ((aliquotsToRemove == null) || (aliquotsToRemove.size() == 0))
            return;

        Collection<DispatchAliquot> allDsaObjects = new HashSet<DispatchAliquot>();
        List<DispatchAliquotWrapper> allDsaWrappers = new ArrayList<DispatchAliquotWrapper>();
        // already added dsa
        List<DispatchAliquotWrapper> currentList = getDispatchAliquotCollection(false);
        if (currentList != null) {
            for (DispatchAliquotWrapper dsa : currentList) {
                if (!aliquotsToRemove.contains(dsa.getAliquot())) {
                    allDsaObjects.add(dsa.getWrappedObject());
                    allDsaWrappers.add(dsa);
                } else {
                    deletedDispatchedShipmentAliquots.add(dsa);
                }
            }
        }
        setDispathcShipmentAliquotCollection(allDsaObjects, allDsaWrappers);
    }

    @Override
    protected void deleteChecks() throws Exception {

    }

    public void receiveAliquots(List<AliquotWrapper> aliquotsToReceive) {
        List<DispatchAliquotWrapper> nonProcessedAliquots = getNonProcessedDispatchAliquotCollection();
        for (DispatchAliquotWrapper dsa : nonProcessedAliquots) {
            if (aliquotsToReceive.contains(dsa.getAliquot())) {
                dsa.setState(DispatchAliquotState.RECEIVED_STATE.ordinal());
            }
        }
        propertiesMap.put(NON_PROCESSED_ALIQUOTS_KEY, null);
        propertiesMap.put(RECEIVED_ALIQUOTS_KEY, null);
    }

    public boolean isInCreationState() {
        return wrappedObject.getState() == null
            || wrappedObject.getState() == 0;
    }

    public boolean isInTransitState() {
        return wrappedObject.getState() != null
            && wrappedObject.getState() == 1;
    }

    public boolean isInReceivedState() {
        return wrappedObject.getState() != null
            && wrappedObject.getState() == 2;
    }

    public boolean hasBeenReceived() {
        return wrappedObject.getState() != null
            && wrappedObject.getState() >= 2;
    }

    public boolean isInClosedState() {
        return wrappedObject.getState() != null
            && wrappedObject.getState() == 3;
    }

    /**
     * Search for shipments with the given waybill. Site can be the sender or
     * the receiver.
     */
    public static List<DispatchWrapper> getShipmentsInSite(
        WritableApplicationService appService, String waybill, SiteWrapper site)
        throws ApplicationException {
        HQLCriteria criteria = new HQLCriteria("from "
            + Dispatch.class.getName()
            + " where (sender.id = ? or receiver.id = ?) and waybill = ?",
            Arrays.asList(new Object[] { site.getId(), site.getId(), waybill }));
        List<Dispatch> shipments = appService.query(criteria);
        List<DispatchWrapper> wrappers = new ArrayList<DispatchWrapper>();
        for (Dispatch s : shipments) {
            wrappers.add(new DispatchWrapper(appService, s));
        }
        return wrappers;
    }

    /**
     * Search for shipments with the given date sent. Don't use hour and minute.
     * Site can be the sender or the receiver.
     */
    public static List<DispatchWrapper> getShipmentsInSiteByDateSent(
        WritableApplicationService appService, Date dateReceived,
        SiteWrapper site) throws ApplicationException {
        Calendar cal = Calendar.getInstance();
        // date at 0:0am
        cal.setTime(dateReceived);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startDate = cal.getTime();
        // date at 0:0pm
        cal.add(Calendar.DATE, 1);
        Date endDate = cal.getTime();
        HQLCriteria criteria = new HQLCriteria(
            "from "
                + Dispatch.class.getName()
                + " where (sender.id = ? or receiver.id = ?) and dateShipped >= ? and dateShipped <= ?",
            Arrays.asList(new Object[] { site.getId(), site.getId(), startDate,
                endDate }));
        List<Dispatch> shipments = appService.query(criteria);
        List<DispatchWrapper> wrappers = new ArrayList<DispatchWrapper>();
        for (Dispatch s : shipments) {
            wrappers.add(new DispatchWrapper(appService, s));
        }
        return wrappers;
    }

    /**
     * Search for shipments with the given date received. Don't use hour and
     * minute. Site can be the sender or the receiver.
     */
    public static List<DispatchWrapper> getShipmentsInSiteByDateReceived(
        WritableApplicationService appService, Date dateReceived,
        SiteWrapper site) throws ApplicationException {
        Calendar cal = Calendar.getInstance();
        // date at 0:0am
        cal.setTime(dateReceived);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startDate = cal.getTime();
        // date at 0:0pm
        cal.add(Calendar.DATE, 1);
        Date endDate = cal.getTime();
        HQLCriteria criteria = new HQLCriteria(
            "from "
                + Dispatch.class.getName()
                + " where (sender.id = ? or receiver.id = ?) and dateReceived >= ? and dateReceived <= ?",
            Arrays.asList(new Object[] { site.getId(), site.getId(), startDate,
                endDate }));
        List<Dispatch> shipments = appService.query(criteria);
        List<DispatchWrapper> wrappers = new ArrayList<DispatchWrapper>();
        for (Dispatch s : shipments) {
            wrappers.add(new DispatchWrapper(appService, s));
        }
        return wrappers;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSender() == null ? "" : getSender().getNameShort() + "/");
        sb.append(getReceiver() == null ? "" : getReceiver().getNameShort()
            + "/");
        sb.append(getFormattedDateShipped() + "/");
        sb.append(getFormattedDateReceived());
        return sb.toString();
    }

    private void setState(Integer state) {
        Integer oldState = wrappedObject.getState();
        wrappedObject.setState(state);
        stateModified = oldState == null || state == null
            || !oldState.equals(state);
    }

    public void setNextState() {
        Integer state = wrappedObject.getState();
        if (state == null) {
            state = 0;
        }
        state++;
        setState(state);
    }

    public void setInErrorState() {
        setState(4);
    }

    public boolean canBeSentBy(User user, SiteWrapper site) {
        return canUpdate(user) && getSender().equals(site)
            && isInCreationState() && hasAliquots();
    }

    public boolean hasAliquots() {
        return getAliquotCollection() != null
            && getAliquotCollection().size() > 0;
    }

    public boolean canBeReceivedBy(User user, SiteWrapper site) {
        return canUpdate(user) && getReceiver().equals(site)
            && isInTransitState();
    }

    public DispatchAliquotWrapper getDispatchAliquot(String inventoryId) {
        for (DispatchAliquotWrapper dsa : getDispatchAliquotCollection()) {
            if (dsa.getAliquot().getInventoryId().equals(inventoryId))
                return dsa;
        }
        return null;
    }

    // public boolean hasFlaggedAliquots() {
    // List<AliquotWrapper> aliquots = getFlaggedAliquots(false, true);
    // return aliquots != null && aliquots.size() > 0;
    // }

    @Override
    protected void resetInternalFields() {
        deletedDispatchedShipmentAliquots.clear();
        stateModified = false;
    }

    public List<DispatchAliquotWrapper> getExtraDispatchAliquots() {
        return getDispatchAliquotCollectionWithState(EXTRA_ALIQUOTS_KEY, true,
            DispatchAliquotState.EXTRA,
            DispatchAliquotState.EXTRA_PENDING_STATE);
    }

    public List<DispatchAliquotWrapper> getMissingDispatchAliquots() {
        return getDispatchAliquotCollectionWithState(MISSING_ALIQUOTS_KEY,
            true, DispatchAliquotState.MISSING,
            DispatchAliquotState.MISSING_PENDING_STATE);
    }

    public List<DispatchAliquotWrapper> getPendingErrorsDispatchAliquots() {
        return getDispatchAliquotCollectionWithState(
            PENDING_ERRORS_ALIQUOTS_KEY, true,
            DispatchAliquotState.MISSING_PENDING_STATE,
            DispatchAliquotState.EXTRA_PENDING_STATE);
    }

    public void resetStateLists() {
        propertiesMap.put(MISSING_ALIQUOTS_KEY, null);
        propertiesMap.put(EXTRA_ALIQUOTS_KEY, null);
        propertiesMap.put(RECEIVED_ALIQUOTS_KEY, null);
        propertiesMap.put(NON_PROCESSED_ALIQUOTS_KEY, null);
        propertiesMap.put(PENDING_ERRORS_ALIQUOTS_KEY, null);
    }

    public boolean hasPendingErrors() {
        List<DispatchAliquotWrapper> dsaList = getPendingErrorsDispatchAliquots();
        return dsaList != null && dsaList.size() > 0;
    }

}
