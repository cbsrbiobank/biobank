package edu.ualberta.med.biobank;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import edu.ualberta.med.biobank.model.Address;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.model.Container;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;
import edu.ualberta.med.biobank.model.ContainerPosition;
import edu.ualberta.med.biobank.model.ContainerType;
import edu.ualberta.med.biobank.model.Dispatch;
import edu.ualberta.med.biobank.model.DispatchSpecimen;
import edu.ualberta.med.biobank.model.Group;
import edu.ualberta.med.biobank.model.Membership;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Principal;
import edu.ualberta.med.biobank.model.ProcessingEvent;
import edu.ualberta.med.biobank.model.Request;
import edu.ualberta.med.biobank.model.RequestSpecimen;
import edu.ualberta.med.biobank.model.Role;
import edu.ualberta.med.biobank.model.ShipmentInfo;
import edu.ualberta.med.biobank.model.ShippingMethod;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenPosition;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.model.User;

/**
 * Tries to make setting up test data easier by requiring the absolute minimum
 * amount of data and remembering the last created object and using that as a
 * default for other objects.
 * 
 * @author Jonathan Ferland
 * 
 */
public class Factory {
    private static final Random R = new Random();

    private final ContainerLabelingSchemeGetter schemeGetter;
    private final NameGenerator nameGenerator;
    private final Session session;

    private Center defaultCenter;
    private ContainerType defaultTopContainerType;
    private ContainerType defaultContainerType;
    private SpecimenType defaultSourceSpecimenType;
    private SpecimenType defaultAliquotedSpecimenType;
    private Container defaultTopContainer;
    private Container defaultParentContainer;
    private Container defaultContainer;
    private Specimen defaultParentSpecimen;
    private Specimen defaultChildSpecimen;
    private ContainerLabelingScheme defaultContainerLabelingScheme;
    private Capacity defaultCapacity = new Capacity(5, 5);
    private Study defaultStudy;
    private Patient defaultPatient;
    private CollectionEvent defaultCollectionEvent;
    private User defaultUser;
    private Group defaultGroup;
    private Principal defaultPrincipal;
    private Membership defaultMembership;
    private Role defaultRole;
    private Dispatch defaultDispatch;
    private DispatchSpecimen defaultDispatchSpecimen;
    private Request defaultRequest;
    private RequestSpecimen defaultRequestSpecimen;
    private ProcessingEvent defaultProcessingEvent;
    private SourceSpecimen defaultSourceSpecimen;
    private AliquotedSpecimen defaultAliquotedSpecimen;
    private Contact defaultContact;
    private Comment defaultComment;
    private ShipmentInfo defaultShipmentInfo;
    private ShippingMethod defaultShippingMethod;

    public Factory(Session session) {
        this(session, new BigInteger(130, R).toString(32));
    }

    public Factory(Session session, String root) {
        this.session = session;
        this.nameGenerator = new NameGenerator(root);
        this.schemeGetter = new ContainerLabelingSchemeGetter();
    }

    /**
     * Made this public so that it can be used by other helpers. For example
     * {@link SpecimenCsvHelper} uses this to generate strings used as values
     * for attributes.
     */
    public NameGenerator getNameGenerator() {
        return nameGenerator;
    }

    public Comment getDefaultComment() {
        if (defaultComment == null) {
            defaultComment = createComment();
        }
        return defaultComment;
    }

    public void setDefaultComment(Comment defaultComment) {
        this.defaultComment = defaultComment;
    }

    public Contact getDefaultContact() {
        if (defaultContact == null) {
            defaultContact = createContact();
        }
        return defaultContact;
    }

    public void setDefaultContact(Contact defaultContact) {
        this.defaultContact = defaultContact;
    }

    public SourceSpecimen getDefaultSourceSpecimen() {
        if (defaultSourceSpecimen == null) {
            defaultSourceSpecimen = createSourceSpecimen();
        }
        return defaultSourceSpecimen;
    }

    public void setDefaultSourceSpecimen(SourceSpecimen defaultSourceSpecimen) {
        this.defaultSourceSpecimen = defaultSourceSpecimen;
        this.defaultSourceSpecimenType =
            defaultSourceSpecimen.getSpecimenType();
    }

    public AliquotedSpecimen getDefaultAliquotedSpecimen() {
        if (defaultAliquotedSpecimen == null) {
            defaultAliquotedSpecimen = createAliquotedSpecimen();
        }
        this.defaultAliquotedSpecimenType =
            defaultAliquotedSpecimen.getSpecimenType();
        return defaultAliquotedSpecimen;
    }

    public void setDefaultAliquotedSpecimen(
        AliquotedSpecimen defaultAliquotedSpecimen) {
        this.defaultAliquotedSpecimen = defaultAliquotedSpecimen;
    }

    public ProcessingEvent getDefaultProcessingEvent() {
        if (defaultProcessingEvent == null) {
            defaultProcessingEvent = createProcessingEvent();
        }
        return defaultProcessingEvent;
    }

    public void setDefaultProcessingEvent(ProcessingEvent defaultProcessingEvent) {
        this.defaultProcessingEvent = defaultProcessingEvent;
    }

    public Request getDefaultRequest() {
        if (defaultRequest == null) {
            defaultRequest = createRequest();
        }
        return defaultRequest;
    }

    public void setDefaultRequest(Request request) {
        this.defaultRequest = request;
    }

    public RequestSpecimen getDefaultRequestSpecimen() {
        if (defaultRequestSpecimen == null) {
            defaultRequestSpecimen = createRequestSpecimen();
        }
        return defaultRequestSpecimen;
    }

    public void setDefaultRequestSpecimen(RequestSpecimen requiestSpecimen) {
        this.defaultRequestSpecimen = requiestSpecimen;
    }

    public Dispatch getDefaultDispatch() {
        if (defaultDispatch == null) {
            defaultDispatch =
                createDispatch(getDefaultCenter(), createCenter());
        }
        return defaultDispatch;
    }

    public void setDefaultDispatch(Dispatch defaultDispatch) {
        this.defaultDispatch = defaultDispatch;
    }

    public DispatchSpecimen getDefaultDispatchSpecimen() {
        if (defaultDispatchSpecimen == null) {
            defaultDispatchSpecimen = createDispatchSpecimen();
        }
        return defaultDispatchSpecimen;
    }

    public void setDefaultDispatchSpecimen(
        DispatchSpecimen defaultDispatchSpecimen) {
        this.defaultDispatchSpecimen = defaultDispatchSpecimen;
    }

    public Role getDefaultRole() {
        if (defaultRole == null) {
            defaultRole = createRole();
        }
        return defaultRole;
    }

    public void setDefaultRole(Role defaultRole) {
        this.defaultRole = defaultRole;
    }

    public Center getDefaultCenter() {
        if (defaultCenter == null) {
            defaultCenter = createCenter();
        }
        return defaultCenter;
    }

    public void setDefaultCenter(Center defaultCenter) {
        this.defaultCenter = defaultCenter;
    }

    public Group getDefaultGroup() {
        if (defaultGroup == null) {
            defaultGroup = createGroup();
        }
        return defaultGroup;
    }

    public void setDefaultGroup(Group defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public Principal getDefaultPrincipal() {
        if (defaultPrincipal == null) {
            defaultPrincipal = createUser();
        }
        return defaultPrincipal;
    }

    public void setDefaultPrincipal(Principal defaultPrincipal) {
        this.defaultPrincipal = defaultPrincipal;
    }

    public User getDefaultUser() {
        if (defaultUser == null) {
            defaultUser = createUser();
        }
        return defaultUser;
    }

    public void setDefaultUser(User defaultUser) {
        this.defaultUser = defaultUser;
    }

    public Membership getDefaultMembership() {
        if (defaultMembership == null) {
            defaultMembership = createMembership();
        }
        return defaultMembership;
    }

    public void setDefaultMembership(Membership defaultMembership) {
        this.defaultMembership = defaultMembership;
    }

    public Container getDefaultParentContainer() {
        return defaultParentContainer;
    }

    public void setDefaultParentContainer(Container defaultParentContainer) {
        this.defaultParentContainer = defaultParentContainer;
    }

    public Container getDefaultTopContainer() {
        if (defaultTopContainer == null) {
            defaultTopContainer = createTopContainer();
        }
        return defaultTopContainer;
    }

    public void setDefaultTopContainer(Container defaultTopContainer) {
        this.defaultTopContainer = defaultTopContainer;
    }

    public ContainerType getDefaultTopContainerType() {
        if (defaultTopContainerType == null) {
            defaultTopContainerType = createTopContainerType();
        }
        return defaultTopContainerType;
    }

    public void setDefaultTopContainerType(ContainerType defaultTopContainerType) {
        this.defaultTopContainerType = defaultTopContainerType;
    }

    public Study getDefaultStudy() {
        if (defaultStudy == null) {
            defaultStudy = createStudy();
        }
        return defaultStudy;
    }

    public void setDefaultStudy(Study defaultStudy) {
        this.defaultStudy = defaultStudy;
    }

    public Patient getDefaultPatient() {
        if (defaultPatient == null) {
            defaultPatient = createPatient();
        }
        return defaultPatient;
    }

    public void setDefaultPatient(Patient defaultPatient) {
        this.defaultPatient = defaultPatient;
    }

    public CollectionEvent getDefaultCollectionEvent() {
        if (defaultCollectionEvent == null) {
            defaultCollectionEvent = createCollectionEvent();
        }
        return defaultCollectionEvent;
    }

    public void setDefaultCollectionEvent(CollectionEvent defaultCollectionEvent) {
        this.defaultCollectionEvent = defaultCollectionEvent;
    }

    public ContainerType getDefaultContainerType() {
        if (defaultContainerType == null) {
            defaultContainerType = createContainerType();
        }
        return defaultContainerType;
    }

    public void setDefaultContainerType(ContainerType defaultContainerType) {
        this.defaultContainerType = defaultContainerType;
    }

    public Container getDefaultContainer() {
        if (defaultContainer == null) {
            defaultContainer = createContainer();
        }
        return defaultContainer;
    }

    public void setDefaultContainer(Container defaultContainer) {
        this.defaultContainer = defaultContainer;
    }

    public ContainerLabelingScheme getDefaultContainerLabelingScheme() {
        if (defaultContainerLabelingScheme == null) {
            defaultContainerLabelingScheme = getScheme().getSbs();
        }
        return defaultContainerLabelingScheme;
    }

    public void setDefaultContainerLabelingScheme(
        ContainerLabelingScheme defaultContainerLabelingScheme) {
        this.defaultContainerLabelingScheme = defaultContainerLabelingScheme;
    }

    public Capacity getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(Capacity defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public SpecimenType getDefaultSourceSpecimenType() {
        if (defaultSourceSpecimenType == null) {
            defaultSourceSpecimenType = createSpecimenType();
        }
        return defaultSourceSpecimenType;
    }

    public void setDefaultSourceSpecimenType(SpecimenType defaultSpecimenType) {
        this.defaultSourceSpecimenType = defaultSpecimenType;
    }

    public SpecimenType getDefaultAliquotedSpecimenType() {
        if (defaultAliquotedSpecimenType == null) {
            defaultAliquotedSpecimenType = createSpecimenType();
        }
        return defaultAliquotedSpecimenType;
    }

    public void setDefaultAliquotedSpecimenType(SpecimenType defaultSpecimenType) {
        this.defaultAliquotedSpecimenType = defaultSpecimenType;
    }

    public Specimen getDefaultParentSpecimen() {
        if (defaultParentSpecimen == null) {
            defaultParentSpecimen = createParentSpecimen();
        }
        return defaultParentSpecimen;
    }

    public void setDefaultParentSpecimen(Specimen defaultSpecimen) {
        this.defaultParentSpecimen = defaultSpecimen;
    }

    public Specimen getDefaultChildSpecimen() {
        if (defaultChildSpecimen == null) {
            defaultChildSpecimen = createChildSpecimen();
        }
        return defaultChildSpecimen;
    }

    public void setDefaultChildSpecimen(Specimen defaultSpecimen) {
        this.defaultChildSpecimen = defaultSpecimen;
    }

    public ShipmentInfo getDefaultShipmentInfo() {
        return defaultShipmentInfo;
    }

    public void setDefaultShipmentInfo(ShipmentInfo defaultShipmentInfo) {
        this.defaultShipmentInfo = defaultShipmentInfo;
    }

    public ShippingMethod getDefaultShippingMethod() {
        return defaultShippingMethod;
    }

    public void setDefaultShippingMethod(ShippingMethod shippingMethod) {
        this.defaultShippingMethod = shippingMethod;
    }

    public Comment createComment() {
        Comment comment = new Comment();
        comment.setUser(getDefaultUser());
        comment.setTimeCreated(new Date());
        comment.setMessage("test");

        setDefaultComment(comment);
        session.save(comment);
        session.flush();
        return comment;
    }

    public Contact createContact() {
        String name = nameGenerator.next(Contact.class);
        Contact contact = new Contact();
        contact.setName(name);

        setDefaultContact(contact);
        session.save(contact);
        session.flush();
        return contact;
    }

    public SourceSpecimen createSourceSpecimen() {
        SourceSpecimen sourceSpecimen = new SourceSpecimen();
        sourceSpecimen.setStudy(getDefaultStudy());
        sourceSpecimen.setSpecimenType(getDefaultSourceSpecimenType());

        setDefaultSourceSpecimen(sourceSpecimen);
        session.save(sourceSpecimen);
        session.flush();
        return sourceSpecimen;
    }

    public AliquotedSpecimen createAliquotedSpecimen() {
        AliquotedSpecimen aliquotedSpecimen = new AliquotedSpecimen();
        aliquotedSpecimen.setStudy(getDefaultStudy());
        aliquotedSpecimen.setVolume(new BigDecimal("1.00"));
        aliquotedSpecimen.setQuantity(1);
        aliquotedSpecimen.setSpecimenType(getDefaultSourceSpecimenType());

        setDefaultAliquotedSpecimen(aliquotedSpecimen);
        session.save(aliquotedSpecimen);
        session.flush();
        return aliquotedSpecimen;
    }

    public ProcessingEvent createProcessingEvent() {
        String worksheet = nameGenerator.next(ProcessingEvent.class);

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setWorksheet(worksheet);
        processingEvent.setCenter(getDefaultCenter());
        processingEvent.setTimeDone(new Date());

        setDefaultProcessingEvent(processingEvent);
        session.save(processingEvent);
        session.flush();
        return processingEvent;
    }

    public Request createRequest() {
        Request request = new Request();

        Address address = request.getAddress();
        address.setCity("testville");

        session.save(address);

        request.setTimeCreated(new Date());
        request.setStudy(getDefaultStudy());

        setDefaultRequest(request);
        session.save(request);
        session.flush();
        return request;
    }

    public RequestSpecimen createRequestSpecimen() {
        RequestSpecimen requestSpecimen = new RequestSpecimen();
        requestSpecimen.setRequest(getDefaultRequest());

        Specimen specimen = createChildSpecimen();
        requestSpecimen.setSpecimen(specimen);

        setDefaultRequestSpecimen(requestSpecimen);
        session.save(requestSpecimen);
        session.flush();
        return requestSpecimen;
    }

    public Dispatch createDispatch(Center sender, Center receiver) {
        Dispatch dispatch = new Dispatch();

        dispatch.setSenderCenter(sender);
        dispatch.setReceiverCenter(receiver);

        setDefaultDispatch(dispatch);
        session.save(dispatch);
        session.flush();
        return dispatch;
    }

    public DispatchSpecimen createDispatchSpecimen() {
        DispatchSpecimen dispatchSpecimen = new DispatchSpecimen();
        dispatchSpecimen.setDispatch(getDefaultDispatch());

        Specimen specimen = createParentSpecimen();
        dispatchSpecimen.setSpecimen(specimen);

        session.save(dispatchSpecimen);
        session.flush();
        return dispatchSpecimen;
    }

    public Center createCenter() {
        String name = nameGenerator.next(Center.class);
        Center center = new Center();
        center.setName(name);
        center.setNameShort(name);
        center.getAddress().setCity("testville");

        setDefaultCenter(center);
        session.save(center);
        session.flush();
        return center;
    }

    public ContainerType createContainerType() {
        String name = nameGenerator.next(ContainerType.class);

        ContainerType containerType = new ContainerType();
        containerType.setName(name);
        containerType.setNameShort(name);
        containerType.setCenter(getDefaultCenter());
        containerType.setCapacity(new Capacity(getDefaultCapacity()));
        containerType
            .setChildLabelingScheme(getDefaultContainerLabelingScheme());

        setDefaultContainerType(containerType);
        session.save(containerType);
        session.flush();
        return containerType;
    }

    public ContainerType createTopContainerType() {
        ContainerType oldDefaultContainerType = getDefaultContainerType();
        ContainerType topContainerType = createContainerType();
        topContainerType.setTopLevel(true);

        // restore the old, non-topLevel ContainerType
        setDefaultContainerType(oldDefaultContainerType);
        setDefaultTopContainerType(topContainerType);
        session.update(topContainerType);
        session.flush();
        return topContainerType;
    }

    public Container createContainer() {
        String label = nameGenerator.next(Container.class);

        Container container = new Container();
        container.setCenter(getDefaultCenter());
        if (!getDefaultTopContainerType().getCenter()
            .equals(container.getCenter())) {
            // make sure sites match
            createTopContainerType();
        }

        // FIXME: why assign to a top container type here?
        container.setContainerType(getDefaultTopContainerType());
        container.setLabel(label);
        container.setTopContainer(container);

        Container parentContainer = getDefaultParentContainer();
        if (parentContainer != null) {
            ContainerType containerType = getDefaultContainerType();
            if (!containerType.getCenter().equals(container.getCenter())) {
                // make sure sites match
                containerType = createContainerType();
            }
            container.setContainerType(containerType);

            ContainerType parentCt = parentContainer.getContainerType();
            parentCt.getChildContainerTypes().add(containerType);

            session.update(parentCt);
            session.flush();

            Integer numChildren = parentContainer.getChildPositions().size();
            Integer row = numChildren / parentCt.getRowCapacity();
            Integer col = numChildren % parentCt.getColCapacity();

            ContainerPosition cp = new ContainerPosition();
            cp.setRow(row);
            cp.setCol(col);

            cp.setContainer(container);
            container.setPosition(cp);

            cp.setParentContainer(parentContainer);
            parentContainer.getChildPositions().add(cp);
            container.setTopContainer(parentContainer.getTopContainer());
        }

        setDefaultContainer(container);
        session.save(container);
        session.flush();
        return container;
    }

    public Container createTopContainer() {
        setDefaultParentContainer(null);
        Container topContainer = createContainer();

        setDefaultTopContainer(topContainer);
        setDefaultParentContainer(topContainer);
        session.update(topContainer);
        session.flush();
        return topContainer;
    }

    public Container createParentContainer() {
        Container parentContainer = createContainer();
        setDefaultParentContainer(parentContainer);
        session.update(parentContainer);
        session.flush();
        return parentContainer;
    }

    public SpecimenType createSpecimenType() {
        String name = nameGenerator.next(SpecimenType.class);

        SpecimenType specimenType = new SpecimenType();
        specimenType.setName(name);
        specimenType.setNameShort(name);

        setDefaultSourceSpecimenType(specimenType);
        session.save(specimenType);
        session.flush();
        return specimenType;
    }

    public Specimen createSpecimen() {
        String name = nameGenerator.next(Specimen.class);

        Specimen specimen = new Specimen();
        specimen.setInventoryId(name);
        specimen.setCurrentCenter(getDefaultCenter());
        specimen.setCollectionEvent(getDefaultCollectionEvent());
        specimen.setOriginCenter(getDefaultStudyCenter());
        specimen.setTimeCreated(new Date());

        return specimen;
    }

    public Specimen createParentSpecimen() {
        Specimen parentSpecimen = createSpecimen();
        parentSpecimen.setSpecimenType(getDefaultSourceSpecimenType());

        CollectionEvent cevent = getDefaultCollectionEvent();
        parentSpecimen.setOriginalCollectionEvent(cevent);
        cevent.getOriginalSpecimens().add(parentSpecimen);
        cevent.getAllSpecimens().add(parentSpecimen);

        session.save(parentSpecimen);
        session.flush();

        setDefaultParentSpecimen(parentSpecimen);
        return parentSpecimen;
    }

    public Specimen createChildSpecimen() {
        Specimen childSpecimen = createSpecimen();
        childSpecimen.setSpecimenType(getDefaultAliquotedSpecimenType());

        Specimen parentSpecimen = getDefaultParentSpecimen();
        childSpecimen.setParentSpecimen(parentSpecimen);
        childSpecimen.setCollectionEvent(parentSpecimen.getCollectionEvent());

        Specimen topSpecimen = parentSpecimen.getTopSpecimen();
        if (topSpecimen != null) {
            childSpecimen.setTopSpecimen(topSpecimen);
        } else {
            childSpecimen.setTopSpecimen(parentSpecimen);
        }

        ProcessingEvent pevent = getDefaultProcessingEvent();
        parentSpecimen.setProcessingEvent(pevent);
        pevent.getSpecimens().add(childSpecimen);

        childSpecimen.setQuantity(getDefaultAliquotedSpecimen().getVolume());
        parentSpecimen.getCollectionEvent().getAllSpecimens()
            .add(childSpecimen);

        session.save(childSpecimen);
        session.flush();

        setDefaultChildSpecimen(childSpecimen);
        return childSpecimen;
    }

    public Specimen createPositionedSpecimen() {
        Specimen assignedSpecimen = createChildSpecimen();

        Container parentContainer = getDefaultContainer();
        ContainerType parentCt = parentContainer.getContainerType();

        parentCt.getSpecimenTypes().add(assignedSpecimen.getSpecimenType());

        session.update(parentCt);
        session.flush();

        Integer numSpecimens = parentContainer.getSpecimenPositions().size();
        Integer row = numSpecimens / parentCt.getRowCapacity();
        Integer col = numSpecimens % parentCt.getColCapacity();

        SpecimenPosition sp = new SpecimenPosition();
        sp.setRow(row);
        sp.setCol(col);
        sp.setPositionString("asdf"); // TODO: set this right

        sp.setSpecimen(assignedSpecimen);
        assignedSpecimen.setSpecimenPosition(sp);

        sp.setContainer(parentContainer);
        parentContainer.getSpecimenPositions().add(sp);

        session.update(assignedSpecimen);
        session.flush();
        return assignedSpecimen;
    }

    public Study createStudy() {
        String name = nameGenerator.next(Study.class);

        Study study = new Study();
        study.setName(name);
        study.setNameShort(name);

        setDefaultStudy(study);
        session.save(study);
        session.flush();
        return study;
    }

    public CollectionEvent createCollectionEvent() {
        CollectionEvent collectionEvent = new CollectionEvent();

        // make sure the patient has this collection events so we can use the
        // set to generate a sensible default visit number.
        Patient patient = getDefaultPatient();
        collectionEvent.setPatient(patient);

        int numCEs = patient.getCollectionEvents().size();
        collectionEvent.setVisitNumber(numCEs + 1);
        patient.getCollectionEvents().add(collectionEvent);

        setDefaultCollectionEvent(collectionEvent);
        session.save(collectionEvent);
        session.update(patient);
        session.flush();
        return collectionEvent;
    }

    public Patient createPatient() {
        String name = nameGenerator.next(Patient.class);

        Patient patient = new Patient();
        patient.setPnumber(name);
        patient.setStudy(getDefaultStudy());
        patient.setTimeCreated(new Date());

        setDefaultPatient(patient);
        session.save(patient);
        session.flush();
        return patient;
    }

    public OriginInfo createOriginInfo() {
        OriginInfo originInfo = new OriginInfo();
        originInfo.setCenter(getDefaultCenter());

        ShipmentInfo shipmentInfo = getDefaultShipmentInfo();
        if (shipmentInfo != null) {
            originInfo.setShipmentInfo(shipmentInfo);
            originInfo.setReceiverSite(getDefaultSite());
        }

        setDefaultOriginInfo(originInfo);
        session.save(originInfo);
        session.flush();
        return originInfo;
    }

    public ShipmentInfo createShipmentInfo() {
        String waybill = nameGenerator.next(ShipmentInfo.class);

        ShipmentInfo shipmentInfo = new ShipmentInfo();

        // set packed at to 2 days ago
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, -2);

        shipmentInfo.setPackedAt(cal.getTime());
        shipmentInfo.setReceivedAt(new Date());
        shipmentInfo.setWaybill(waybill);
        shipmentInfo.setBoxNumber("");
        shipmentInfo.setShippingMethod(createShippingMethod());

        setDefaultShipmentInfo(shipmentInfo);
        session.save(shipmentInfo);
        session.flush();

        return shipmentInfo;
    }

    public ShippingMethod createShippingMethod() {
        String name = nameGenerator.next(ShippingMethod.class);
        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setName(name);

        setDefaultShippingMethod(shippingMethod);
        session.save(shippingMethod);
        session.flush();
        return shippingMethod;
    }

    public User createUser() {
        String name = nameGenerator.next(User.class);

        User user = new User();
        user.setLogin(name);
        user.setEmail(name);
        user.setFullName("joe testonson");

        // cheap fix to avoid actually having to create a CSM user
        user.setCsmUserId(-Math.abs(R.nextLong()));

        // temporary membership, for creating
        Membership m = new Membership();
        m.setPrincipal(user);
        user.getMemberships().add(m);

        setDefaultUser(user);
        setDefaultPrincipal(user);
        session.save(user);
        session.flush();

        // remove membership
        user.getMemberships().clear();
        session.delete(m);

        session.update(user);
        session.flush();
        return user;
    }

    public Group createGroup() {
        String name = nameGenerator.next(Group.class);

        Group group = new Group();
        group.setName(name);
        group.setDescription(name);

        // temporary membership, for creating
        Membership m = new Membership();
        m.setPrincipal(group);
        group.getMemberships().add(m);

        setDefaultGroup(group);
        setDefaultPrincipal(group);

        session.save(group);
        session.flush();

        // remove membership
        group.getMemberships().clear();
        session.delete(m);

        session.update(group);
        session.flush();
        return group;
    }

    public Membership createMembership() {
        return buildMembership().create();
    }

    public static class MembershipBuilder {
        private final Factory factory;
        private boolean userManager = false;
        private boolean everyPermission = false;
        private Quantity centerQuantity = Quantity.ONE;
        private Quantity studyQuantity = Quantity.ONE;

        public MembershipBuilder(Factory factory) {
            this.factory = factory;
        }

        public MembershipBuilder setCenter() {
            centerQuantity = Quantity.ONE;
            return this;
        }

        public MembershipBuilder setStudy() {
            studyQuantity = Quantity.ONE;
            return this;
        }

        public MembershipBuilder setGlobal() {
            centerQuantity = Quantity.ALL;
            studyQuantity = Quantity.ALL;
            return this;
        }

        public MembershipBuilder setAllCenters() {
            centerQuantity = Quantity.ALL;
            return this;
        }

        public MembershipBuilder setAllStudies() {
            studyQuantity = Quantity.ALL;
            return this;
        }

        public MembershipBuilder setUserManager(boolean userManager) {
            this.userManager = userManager;
            if (userManager) setEveryPermission(true);
            return this;
        }

        public MembershipBuilder setEveryPermission(boolean everyPermission) {
            this.everyPermission = everyPermission;
            if (!everyPermission) setUserManager(false);
            return this;
        }

        public Membership create() {
            return factory.createMembership(this);
        }

        enum Quantity {
            NONE,
            ONE,
            ALL;
        }
    }

    public MembershipBuilder buildMembership() {
        return new MembershipBuilder(this);
    }

    public Membership createMembership(MembershipBuilder builder) {
        Membership membership = new Membership();

        switch (builder.centerQuantity) {
        case ONE:
            membership.getDomain().getCenters().add(getDefaultCenter());
            break;
        case ALL:
            membership.getDomain().setAllCenters(true);
            break;
        }

        switch (builder.studyQuantity) {
        case ONE:
            membership.getDomain().getStudies().add(getDefaultStudy());
            break;
        case ALL:
            membership.getDomain().setAllStudies(true);
            break;
        }

        Principal p = getDefaultPrincipal();
        p.getMemberships().add(membership);
        membership.setPrincipal(p);

        membership.setEveryPermission(builder.everyPermission);
        membership.setUserManager(builder.userManager);

        setDefaultMembership(membership);
        session.save(membership);
        session.flush();
        return membership;
    }

    public Role createRole() {
        String name = nameGenerator.next(Role.class);

        Role role = new Role();

        role.setName(name);

        setDefaultRole(role);
        session.save(role);
        session.flush();
        return role;
    }

    public ContainerLabelingSchemeGetter getScheme() {
        return schemeGetter;
    }

    public class ContainerLabelingSchemeGetter {
        public ContainerLabelingScheme getSbs() {
            return (ContainerLabelingScheme) session
                .createCriteria(ContainerLabelingScheme.class)
                .add(Restrictions.idEq(1))
                .uniqueResult();
        }

        public ContainerLabelingScheme get2CharAlphabetic() {
            return (ContainerLabelingScheme) session
                .createCriteria(ContainerLabelingScheme.class)
                .add(Restrictions.idEq(6))
                .uniqueResult();
        }
    }

    public String getName(Class<?> klazz) {
        return nameGenerator.next(klazz);
    }
}