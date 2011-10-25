package edu.ualberta.med.biobank.mvp.presenter.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.pietschy.gwt.pectin.client.form.FieldModel;
import com.pietschy.gwt.pectin.client.form.ListFieldModel;
import com.pietschy.gwt.pectin.client.form.validation.ValidationPlugin;
import com.pietschy.gwt.pectin.client.form.validation.component.ValidationDisplay;
import com.pietschy.gwt.pectin.client.form.validation.validator.NotEmptyValidator;

import edu.ualberta.med.biobank.common.action.ActionCallback;
import edu.ualberta.med.biobank.common.action.Dispatcher;
import edu.ualberta.med.biobank.common.action.site.SiteGetInfoAction;
import edu.ualberta.med.biobank.common.action.site.SiteGetInfoAction.SiteInfo;
import edu.ualberta.med.biobank.common.action.site.SiteGetStudyInfoAction.StudyInfo;
import edu.ualberta.med.biobank.common.action.site.SiteSaveAction;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.Address;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.mvp.event.AlertEvent;
import edu.ualberta.med.biobank.mvp.event.model.site.SiteChangedEvent;
import edu.ualberta.med.biobank.mvp.event.presenter.site.SiteViewPresenterShowEvent;
import edu.ualberta.med.biobank.mvp.model.BaseModel;
import edu.ualberta.med.biobank.mvp.presenter.impl.SiteEntryPresenter.View;
import edu.ualberta.med.biobank.mvp.util.ObjectCloner;
import edu.ualberta.med.biobank.mvp.view.IFormView;
import edu.ualberta.med.biobank.mvp.view.IView;

public class SiteEntryPresenter extends BaseEntryPresenter<View> {
    private final Dispatcher dispatcher;
    private final AddressEditPresenter addressEditPresenter;
    private final ActivityStatusComboPresenter activityStatusComboPresenter;
    private final Model model;

    public interface View extends IFormView, ValidationDisplay {
        void setActivityStatusComboView(IView view);

        void setAddressEditView(IView view);

        HasValue<String> getName();

        HasValue<String> getNameShort();

        HasValue<String> getComment();

        HasValue<Collection<StudyInfo>> getStudies();
    }

    @Inject
    public SiteEntryPresenter(View view, EventBus eventBus,
        Dispatcher dispatcher,
        AddressEditPresenter addressEditPresenter,
        ActivityStatusComboPresenter activityStatusComboPresenter) {
        super(view, eventBus);
        this.dispatcher = dispatcher;
        this.addressEditPresenter = addressEditPresenter;
        this.activityStatusComboPresenter = activityStatusComboPresenter;

        this.model = new Model();

        // so this view can create the other views if create() is called
        view.setAddressEditView(addressEditPresenter.getView());
        view.setActivityStatusComboView(activityStatusComboPresenter.getView());
    }

    @Override
    public void onBind() {
        super.onBind();

        addressEditPresenter.bind();
        activityStatusComboPresenter.bind();

        binder.enable(view.getSave()).when(model.dirty());

        model.bind();
    }

    @Override
    protected void onUnbind() {
        model.unbind();

        activityStatusComboPresenter.unbind();
    }

    @Override
    public void doReload() {
        // TODO: this resets the form. To reload it from the database, something
        // different must be done (e.g. setting a Command that is re-run on
        // reload).
        model.revert();
    }

    @Override
    public void doSave() {
        if (!model.validate()) {
            return;
        }

        SiteSaveAction saveSite = new SiteSaveAction();
        saveSite.setId(model.siteId.getValue());
        saveSite.setName(model.name.getValue());
        saveSite.setNameShort(model.nameShort.getValue());
        saveSite.setComment(model.comment.getValue());
        saveSite.setAddress(model.getAddress());
        saveSite.setActivityStatusId(model.getActivityStatusId());
        saveSite.setStudyIds(model.getStudyIds());

        dispatcher.exec(saveSite, new ActionCallback<Integer>() {
            @Override
            public void onFailure(Throwable caught) {
                // TODO: better error message and show or log exception?
                eventBus.fireEvent(new AlertEvent(caught.getLocalizedMessage()));
            }

            @Override
            public void onSuccess(Integer siteId) {
                eventBus.fireEvent(new SiteChangedEvent(siteId));
                eventBus.fireEvent(new SiteViewPresenterShowEvent(siteId));
                close();
            }
        });
    }

    public View createSite() {
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setSite(new Site());
        siteInfo.getSite().setName("name");
        siteInfo.getSite().setAddress(new Address());
        siteInfo.getSite().getAddress().setStreet1("asdfasdfa");
        return editSite(siteInfo);
    }

    public View editSite(Integer siteId) {
        SiteGetInfoAction getSiteInfo = new SiteGetInfoAction(siteId);
        dispatcher.exec(getSiteInfo, new ActionCallback<SiteInfo>() {
            @Override
            public void onFailure(Throwable caught) {
                // TODO: better error message and show or log exception?
                eventBus.fireEvent(new AlertEvent(caught.getLocalizedMessage()));
                close();
            }

            @Override
            public void onSuccess(SiteInfo siteInfo) {
                editSite(siteInfo);
            }
        });

        return view;
    }

    public View editSite(SiteInfo siteInfo) {
        // get our own (deep) copy of the data
        SiteInfo clone = ObjectCloner.deepCopy(siteInfo);
        model.setValue(clone);
        return view;
    }

    public class Model extends BaseModel<SiteInfo> {
        final FieldModel<Integer> siteId;
        final FieldModel<String> name;
        final FieldModel<String> nameShort;
        final FieldModel<String> comment;
        final FieldModel<ActivityStatus> activityStatus;
        final FieldModel<Address> address;
        final ListFieldModel<StudyInfo> studies;

        private Model() {
            super(SiteInfo.class);

            siteId = fieldOfType(Integer.class)
                .boundTo(provider, "site.id");
            name = fieldOfType(String.class)
                .boundTo(provider, "site.name");
            nameShort = fieldOfType(String.class)
                .boundTo(provider, "site.nameShort");
            comment = fieldOfType(String.class)
                .boundTo(provider, "site.comment");
            activityStatus = fieldOfType(ActivityStatus.class)
                .boundTo(provider, "site.activityStatus");
            address = fieldOfType(Address.class)
                .boundTo(provider, "site.address");
            studies = listOfType(StudyInfo.class)
                .boundTo(provider, "studies");

            addChild(addressEditPresenter.getModel());
        }

        Integer getActivityStatusId() {
            ActivityStatus activityStatus = this.activityStatus.getValue();
            return activityStatus != null ? activityStatus.getId() : null;
        }

        Set<Integer> getStudyIds() {
            Set<Integer> studyIds = new HashSet<Integer>();
            for (StudyInfo studyInfo : studies) {
                studyIds.add(studyInfo.getStudy().getId());
            }
            return studyIds;
        }

        Address getAddress() {
            return model.address.getValue();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onBind() {
            binder.bind(name).to(view.getName());
            binder.bind(nameShort).to(view.getNameShort());
            binder.bind(comment).to(view.getComment());
            // binder.bind(studies).to(view.getStudies());
            binder.bind(activityStatus)
                .to(activityStatusComboPresenter.getActivityStatus());

            // necessary so when this model's value is set that the Address is
            // also updated in the Address presenter
            binder.bind(address)
                .to(addressEditPresenter.getModel().getMutableValueModel());

            // validation
            ValidationPlugin.validateField(name)
                .using(new NotEmptyValidator("Name is required"));
            ValidationPlugin.validateField(nameShort)
                .using(new NotEmptyValidator("Name Short is required"));

            // TODO: listen on changes to condition and to field, then
            // validate(), put in superclass?

            // validationBinder.bindValidationOf(this).to(view);
        }

        @Override
        public void onUnbind() {
        }
    }
}
