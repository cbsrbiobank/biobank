package edu.ualberta.med.biobank.common.peer;

import edu.ualberta.med.biobank.common.util.TypeReference;
import java.util.Collections;
import edu.ualberta.med.biobank.common.wrappers.Property;
import java.util.List;
import java.util.ArrayList;
import edu.ualberta.med.biobank.model.ResearchGroup;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.model.Patient;
import edu.ualberta.med.biobank.model.Comment;
import edu.ualberta.med.biobank.model.Membership;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.StudyEventAttr;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import java.util.Collection;
import edu.ualberta.med.biobank.model.Study;

public class StudyPeer {
	public static final Property<Integer, Study> ID = Property.create(
		"id" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Integer>() {}
		, new Property.Accessor<Integer, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Integer get(Study model) {
				return model.getId();
			}
			@Override
			public void set(Study model, Integer value) {
				model.setId(value);
			}
		});

	public static final Property<String, Study> NAME = Property.create(
		"name" //$NON-NLS-1$
		, Study.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Study model) {
				return model.getName();
			}
			@Override
			public void set(Study model, String value) {
				model.setName(value);
			}
		});

	public static final Property<String, Study> NAME_SHORT = Property.create(
		"nameShort" //$NON-NLS-1$
		, Study.class
		, new TypeReference<String>() {}
		, new Property.Accessor<String, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public String get(Study model) {
				return model.getNameShort();
			}
			@Override
			public void set(Study model, String value) {
				model.setNameShort(value);
			}
		});

	public static final Property<ResearchGroup, Study> RESEARCH_GROUP = Property.create(
		"researchGroup" //$NON-NLS-1$
		, Study.class
		, new TypeReference<ResearchGroup>() {}
		, new Property.Accessor<ResearchGroup, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public ResearchGroup get(Study model) {
				return model.getResearchGroup();
			}
			@Override
			public void set(Study model, ResearchGroup value) {
				model.setResearchGroup(value);
			}
		});

	public static final Property<Collection<Contact>, Study> CONTACT_COLLECTION = Property.create(
		"contactCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<Contact>>() {}
		, new Property.Accessor<Collection<Contact>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Contact> get(Study model) {
				return model.getContactCollection();
			}
			@Override
			public void set(Study model, Collection<Contact> value) {
				model.setContactCollection(value);
			}
		});

	public static final Property<Collection<Patient>, Study> PATIENT_COLLECTION = Property.create(
		"patientCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<Patient>>() {}
		, new Property.Accessor<Collection<Patient>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Patient> get(Study model) {
				return model.getPatientCollection();
			}
			@Override
			public void set(Study model, Collection<Patient> value) {
				model.setPatientCollection(value);
			}
		});

	public static final Property<Collection<Comment>, Study> COMMENT_COLLECTION = Property.create(
		"commentCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<Comment>>() {}
		, new Property.Accessor<Collection<Comment>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Comment> get(Study model) {
				return model.getCommentCollection();
			}
			@Override
			public void set(Study model, Collection<Comment> value) {
				model.setCommentCollection(value);
			}
		});

	public static final Property<Collection<Membership>, Study> MEMBERSHIP_COLLECTION = Property.create(
		"membershipCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<Membership>>() {}
		, new Property.Accessor<Collection<Membership>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Membership> get(Study model) {
				return model.getMembershipCollection();
			}
			@Override
			public void set(Study model, Collection<Membership> value) {
				model.setMembershipCollection(value);
			}
		});

	public static final Property<Collection<Site>, Study> SITE_COLLECTION = Property.create(
		"siteCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<Site>>() {}
		, new Property.Accessor<Collection<Site>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<Site> get(Study model) {
				return model.getSiteCollection();
			}
			@Override
			public void set(Study model, Collection<Site> value) {
				model.setSiteCollection(value);
			}
		});

	public static final Property<Collection<StudyEventAttr>, Study> STUDY_EVENT_ATTR_COLLECTION = Property.create(
		"studyEventAttrCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<StudyEventAttr>>() {}
		, new Property.Accessor<Collection<StudyEventAttr>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<StudyEventAttr> get(Study model) {
				return model.getStudyEventAttrCollection();
			}
			@Override
			public void set(Study model, Collection<StudyEventAttr> value) {
				model.setStudyEventAttrCollection(value);
			}
		});

	public static final Property<ActivityStatus, Study> ACTIVITY_STATUS = Property.create(
		"activityStatus" //$NON-NLS-1$
		, Study.class
		, new TypeReference<ActivityStatus>() {}
		, new Property.Accessor<ActivityStatus, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public ActivityStatus get(Study model) {
				return model.getActivityStatus();
			}
			@Override
			public void set(Study model, ActivityStatus value) {
				model.setActivityStatus(value);
			}
		});

	public static final Property<Collection<AliquotedSpecimen>, Study> ALIQUOTED_SPECIMEN_COLLECTION = Property.create(
		"aliquotedSpecimenCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<AliquotedSpecimen>>() {}
		, new Property.Accessor<Collection<AliquotedSpecimen>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<AliquotedSpecimen> get(Study model) {
				return model.getAliquotedSpecimenCollection();
			}
			@Override
			public void set(Study model, Collection<AliquotedSpecimen> value) {
				model.setAliquotedSpecimenCollection(value);
			}
		});

	public static final Property<Collection<SourceSpecimen>, Study> SOURCE_SPECIMEN_COLLECTION = Property.create(
		"sourceSpecimenCollection" //$NON-NLS-1$
		, Study.class
		, new TypeReference<Collection<SourceSpecimen>>() {}
		, new Property.Accessor<Collection<SourceSpecimen>, Study>() { private static final long serialVersionUID = 1L;
			@Override
			public Collection<SourceSpecimen> get(Study model) {
				return model.getSourceSpecimenCollection();
			}
			@Override
			public void set(Study model, Collection<SourceSpecimen> value) {
				model.setSourceSpecimenCollection(value);
			}
		});

   public static final List<Property<?, ? super Study>> PROPERTIES;
   static {
      List<Property<?, ? super Study>> aList = new ArrayList<Property<?, ? super Study>>();
      aList.add(ID);
      aList.add(NAME);
      aList.add(NAME_SHORT);
      aList.add(RESEARCH_GROUP);
      aList.add(CONTACT_COLLECTION);
      aList.add(PATIENT_COLLECTION);
      aList.add(COMMENT_COLLECTION);
      aList.add(MEMBERSHIP_COLLECTION);
      aList.add(SITE_COLLECTION);
      aList.add(STUDY_EVENT_ATTR_COLLECTION);
      aList.add(ACTIVITY_STATUS);
      aList.add(ALIQUOTED_SPECIMEN_COLLECTION);
      aList.add(SOURCE_SPECIMEN_COLLECTION);
      PROPERTIES = Collections.unmodifiableList(aList);
   };
}
