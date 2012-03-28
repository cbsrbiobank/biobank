package edu.ualberta.med.biobank.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import edu.ualberta.med.biobank.validator.constraint.Unique;
import edu.ualberta.med.biobank.validator.group.PrePersist;

@Entity
@DiscriminatorValue("User")
@Unique.List({
    @Unique(properties = "csmUserId", groups = PrePersist.class),
    @Unique(properties = "email", groups = PrePersist.class),
    @Unique(properties = "login", groups = PrePersist.class)
})
public class User extends Principal {
    private static final long serialVersionUID = 1L;

    private String login;
    private Long csmUserId;
    private boolean recvBulkEmails = true;
    private String fullName;
    private String email;
    private boolean needPwdChange = true;
    private Set<Comment> comments = new HashSet<Comment>(0);
    private Set<Group> groups = new HashSet<Group>(0);

    @NotEmpty(message = "{edu.ualberta.med.biobank.model.User.login.NotEmpty}")
    @Column(name = "LOGIN", unique = true)
    public String getLogin() {
        return this.login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @NotNull(message = "{edu.ualberta.med.biobank.model.User.csmUserId.NotNull}")
    @Column(name = "CSM_USER_ID", unique = true)
    public Long getCsmUserId() {
        return this.csmUserId;
    }

    public void setCsmUserId(Long csmUserId) {
        this.csmUserId = csmUserId;
    }

    @Column(name = "RECV_BULK_EMAILS")
    // TODO: rename to isRecvBulkEmails
    public boolean getRecvBulkEmails() {
        return this.recvBulkEmails;
    }

    public void setRecvBulkEmails(boolean recvBulkEmails) {
        this.recvBulkEmails = recvBulkEmails;
    }

    @Column(name = "FULL_NAME")
    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // TODO: write an email check that allows null @Email
    @Column(name = "EMAIL", unique = true)
    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(name = "NEED_PWD_CHANGE")
    // TODO: rename to isRecvBulkEmails
    public boolean getNeedPwdChange() {
        return this.needPwdChange;
    }

    public void setNeedPwdChange(boolean needPwdChange) {
        this.needPwdChange = needPwdChange;
    }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", updatable = false)
    public Set<Comment> getComments() {
        return this.comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    public Set<Group> getGroups() {
        return this.groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    @Override
    public boolean isFullyManageable(User user) {
        if (!super.isFullyManageable(user)) return false;
        for (Group group : getGroups()) {
            if (!group.isFullyManageable(user)) return false;
        }
        return true;
    }

    /**
     * Returns all of this {@link User}'s {@link Memberships}, i.e. from both
     * the {@link User} directly and from the {@link Group}-s.
     * 
     * @return
     */
    @Transient
    public Set<Membership> getAllMemberships() {
        Set<Membership> memberships = new HashSet<Membership>();
        memberships.addAll(getMemberships());

        for (Group group : getGroups()) {
            memberships.addAll(group.getMemberships());
        }

        return memberships;
    }

    @Transient
    public Set<Domain> getManageableDomains() {
        Set<Domain> domains = new HashSet<Domain>();
        for (Membership membership : getAllMemberships()) {
            if (membership.isUserManager()) {
                domains.add(membership.getDomain());
            }
        }
        return domains;
    }
}