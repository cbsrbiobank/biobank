package edu.ualberta.med.biobank.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.Audited;

import edu.ualberta.med.biobank.validator.constraint.Unique;
import edu.ualberta.med.biobank.validator.group.PrePersist;

/**
 * Joins {@link Specimen}s to {@link CollectionEvent}s, which provides
 * generalised parentage information about where {@link Specimen}s came from (
 * {@link SpecimenProcessing} provides much more specific information). It
 * is possible for {@link Specimen}s to have at most one {@link CollectionEvent}
 * per {@link Study}.
 * <p>
 * Having one {@link CollectionEvent} per {@link Study} instead of one total
 * allows each {@link Study} to have their own metadata on a {@link Specimen}.
 * This allows for cases where one {@link Study} collects and transfers
 * {@link Specimens} to another {@link Study}.
 * 
 * @author Jonathan Ferland
 * @see SpecimenProcessing
 */
@Audited
@Entity
@Table(name = "STUDY_SPECIMEN")
@Unique(properties = { "study", "specimen" }, groups = PrePersist.class)
public class StudySpecimen
    extends VersionedLongIdModel {
    private static final long serialVersionUID = 1L;

    private Study study;
    private Specimen specimen;
    private CollectionEvent collectionEvent;

    @NaturalId
    @NotNull(message = "{StudySpecimen.study.NotNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STUDY_ID", nullable = false)
    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    @NaturalId
    @NotNull(message = "{StudySpecimen.specimen.NotNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPECIMEN_ID", nullable = false)
    public Specimen getSpecimen() {
        return specimen;
    }

    public void setSpecimen(Specimen specimen) {
        this.specimen = specimen;
    }

    @NotNull(message = "{StudySpecimen.collectionEvent.NotNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLLECTION_EVENT_ID", nullable = false)
    public CollectionEvent getCollectionEvent() {
        return collectionEvent;
    }

    public void setCollectionEvent(CollectionEvent collectionEvent) {
        this.collectionEvent = collectionEvent;
    }
}