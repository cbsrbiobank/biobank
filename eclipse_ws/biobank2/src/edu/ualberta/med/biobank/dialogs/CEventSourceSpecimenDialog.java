package edu.ualberta.med.biobank.dialogs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.others.CheckNoDuplicateAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenInfo;
import edu.ualberta.med.biobank.common.peer.SpecimenPeer;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.CommentWrapper;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.DateTimeWidget;
import edu.ualberta.med.biobank.gui.common.widgets.utils.ComboSelectionUpdate;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.SourceSpecimen;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.validators.DoubleNumberValidator;
import edu.ualberta.med.biobank.validators.NotNullValidator;
import edu.ualberta.med.biobank.widgets.BiobankLabelProvider;
import edu.ualberta.med.biobank.widgets.infotables.entry.CommentedSpecimenInfo;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class CEventSourceSpecimenDialog extends PagedDialog {
    private static final I18n i18n = I18nFactory.getI18n(CEventSourceSpecimenDialog.class);

    private CommentedSpecimenInfo editedSpecimen;

    private ComboViewer specimenTypeComboViewer;

    private final Map<String, SourceSpecimen> mapStudySourceSpecimen;

    private final List<SpecimenType> allSpecimenTypes;

    private String currentTitle;

    private boolean dialogCreated = false;

    private DateTimeWidget timeDrawnWidget;
    private Label timeDrawnLabel;
    private Label quantityLabel;
    private BgcBaseText inventoryIdWidget;
    private BgcBaseText quantityText;
    private DoubleNumberValidator quantityTextValidator;
    private ComboViewer activityStatusComboViewer;

    private final Date defaultTimeDrawn;

    private final CommentedSpecimenInfo internalSpecimen;

    private final List<String> inventoryIdExcludeList;

    private final CommentWrapper commentWrapper = new CommentWrapper(
        SessionManager.getAppService());

    private BgcBaseText commentWidget;

    protected IObservableValue uniqueInventoryId = new WritableValue(
        Boolean.TRUE, Boolean.class);

    private Binding uniqueInventoryIdBinding;

    private String duplicateInventoryId = null;

    @SuppressWarnings("nls")
    public CEventSourceSpecimenDialog(Shell parent, CommentedSpecimenInfo spec,
        Set<SourceSpecimen> studySourceSpecimen,
        List<SpecimenType> allSpecimenTypes,
        List<String> inventoryIdExcludeList, NewListener listener,
        Date defaultTimeDrawn) {
        super(parent, listener, spec == null);
        this.defaultTimeDrawn = defaultTimeDrawn;
        this.inventoryIdExcludeList = inventoryIdExcludeList;
        Assert.isNotNull(studySourceSpecimen);
        internalSpecimen = new CommentedSpecimenInfo(new SpecimenInfo());
        internalSpecimen.specimen = new Specimen();
        if (spec == null) {
            // FIXME ugly
            internalSpecimen.specimen.setActivityStatus(ActivityStatus.ACTIVE);
            internalSpecimen.specimen.setCreatedAt(defaultTimeDrawn);
        } else {
            internalSpecimen.specimen.setId(spec.specimen.getId());
            internalSpecimen.specimen.setSpecimenType(spec.specimen
                .getSpecimenType());
            internalSpecimen.specimen.setInventoryId(spec.specimen
                .getInventoryId());
            internalSpecimen.specimen.setQuantity(spec.specimen.getQuantity());
            internalSpecimen.specimen
                .setCreatedAt(spec.specimen.getCreatedAt());
            internalSpecimen.specimen.setActivityStatus(spec.specimen
                .getActivityStatus());
            // comments is special
            internalSpecimen.comments = spec.comments;

            editedSpecimen = spec;
        }
        mapStudySourceSpecimen = new HashMap<String, SourceSpecimen>();
        for (SourceSpecimen ss : studySourceSpecimen) {
            mapStudySourceSpecimen.put(ss.getSpecimenType().getName(), ss);
        }
        this.allSpecimenTypes = allSpecimenTypes;
        if (addMode) {
            // TR: add source specimen to collection event dialog title
            currentTitle = i18n.trc("dialog title", "Add specimen");
        } else {
            // TR: edit source specimen from collection event dialog title
            currentTitle = i18n.trc("dialog title", "Edit specimen");
        }
    }

    @Override
    protected String getDialogShellTitle() {
        return currentTitle;
    }

    @SuppressWarnings("nls")
    @Override
    protected String getTitleAreaMessage() {
        if (addMode) {
            // TR: add source specimen to collection event dialog title area
            // message
            return i18n.tr("Add a specimen to a collection event");
        }
        // TR: edit source specimen from collection event dialog title area
        // message
        return i18n.tr("Edit a specimen in a collection event");
    }

    @Override
    protected String getTitleAreaTitle() {
        return currentTitle;
    }

    @SuppressWarnings("nls")
    @Override
    protected void createDialogAreaInternal(final Composite parent) {
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(3, false));
        contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final NonEmptyStringValidator inventoryIdValidator = new NonEmptyStringValidator(
            // TR: validation error message
            i18n.tr("Enter a specimen inventory ID"));

        inventoryIdWidget = (BgcBaseText) createBoundWidgetWithLabel(contents, BgcBaseText.class,
            SWT.NONE, Specimen.PropertyName.INVENTORY_ID.toString(), null,
            internalSpecimen.specimen, SpecimenPeer.INVENTORY_ID.getName(), inventoryIdValidator);
        GridData gd = (GridData) inventoryIdWidget.getLayoutData();
        gd.horizontalSpan = 2;
        uniqueInventoryId.setValue(true);
        inventoryIdWidget.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (inventoryIdWidget.getText().isEmpty()) return;

                final String inventoryId = inventoryIdWidget.getText();

                if ((editedSpecimen != null)
                    && (editedSpecimen.specimen != null)
                    && editedSpecimen.specimen.getInventoryId().equals(inventoryId)) {
                    return;
                }

                BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Boolean duplicate = !SessionManager.getAppService().doAction(
                                new CheckNoDuplicateAction(Specimen.class,
                                    null, SpecimenPeer.INVENTORY_ID.getName(), inventoryId)).isTrue();

                            if (duplicate || inventoryIdExcludeList.contains(inventoryId)) {
                                duplicateInventoryId = inventoryId;
                                uniqueInventoryId.setValue(false);
                                inventoryIdValidator.showDecoration();

                                if (duplicate) {
                                    BgcPlugin.openAsyncError(
                                        // TR: dialog title
                                        i18n.tr("Specimen Inventory ID Error"),
                                        // TR: dialog message
                                        i18n.tr("The inventory ID {0} already exists in the system", inventoryId));
                                } else {

                                    BgcPlugin.openAsyncError(
                                        // TR: dialog title
                                        i18n.tr("Specimen Inventory ID Error"),
                                        // TR: dialog message
                                        i18n.tr("The inventory ID  {0} already exists in this collection event",
                                            inventoryId));
                                }
                            }
                        } catch (ApplicationException ex) {
                            BgcPlugin.openAsyncError(
                                // dialog title.
                                i18n.tr("Error checking inventory id"), ex);
                        }
                    }
                });

            }
        });
        inventoryIdWidget.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if ((duplicateInventoryId != null)
                    && !duplicateInventoryId.equals(inventoryIdWidget.getText())) {
                    duplicateInventoryId = null;
                    uniqueInventoryId.setValue(true);
                    inventoryIdValidator.hideDecoration();
                }
            }

        });

        uniqueInventoryIdBinding = widgetCreator.addBooleanBinding(
            new WritableValue(Boolean.FALSE, Boolean.class),
            uniqueInventoryId,
            // TR: validation error message
            i18n.tr("Inventory ID already exists"));

        widgetCreator.addBinding(uniqueInventoryIdBinding);

        addSpecimenTypeWidgets(contents);

        timeDrawnLabel = widgetCreator.createLabel(contents, i18n.trc("label", "Time drawn"));
        timeDrawnLabel.setToolTipText(
            // TR: source specimen time drawn tooltip
            i18n.tr("If the study doesn't require a specific time drawn, then the default time drawn is used"));
        timeDrawnWidget = createDateTimeWidget(contents, timeDrawnLabel,
            internalSpecimen.specimen.getCreatedAt(), internalSpecimen.specimen,
            SpecimenPeer.CREATED_AT.getName(),
            new NotNullValidator(
                // TR: validation error message
                i18n.tr("Time drawn should be set")),
            SWT.DATE | SWT.TIME, null);
        gd = (GridData) timeDrawnWidget.getLayoutData();
        gd.horizontalSpan = 2;

        activityStatusComboViewer = widgetCreator.createComboViewer(contents,
            ActivityStatus.NAME.format(1).toString(), ActivityStatus.valuesList(),
            internalSpecimen.specimen.getActivityStatus(),
            // TR: validation message if activity status not selected
            i18n.tr("An activity status should be selected"),
            new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    internalSpecimen.specimen.setActivityStatus((ActivityStatus) selectedObject);
                }
            }, new BiobankLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((ActivityStatus) element).getName();
                }
            });
        gd = (GridData) activityStatusComboViewer.getControl().getLayoutData();
        gd.horizontalSpan = 2;

        createCommentSection(contents);

        quantityLabel = widgetCreator.createLabel(contents, i18n.tr("Quantity (ml)"));
        quantityLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        quantityTextValidator = new DoubleNumberValidator(
            // TR: validation error message
            i18n.tr("Quantity is required."));
        quantityText = (BgcBaseText) createBoundWidget(contents, BgcBaseText.class,
            SWT.BORDER, quantityLabel, new String[0], internalSpecimen.specimen,
            SpecimenPeer.QUANTITY.getName(), quantityTextValidator);
        gd = (GridData) quantityText.getLayoutData();
        gd.horizontalSpan = 2;

        dialogCreated = true;
        updateWidgetVisibilityAndValues();
    }

    @SuppressWarnings("nls")
    private void createCommentSection(Composite contents) {
        commentWidget =
            (BgcBaseText) createBoundWidgetWithLabel(contents,
                BgcBaseText.class, SWT.MULTI,
                i18n.trc("label", "Add a comment"),
                null,
                commentWrapper, "message",
                null);
        GridData gd = new GridData();
        gd = (GridData) commentWidget.getLayoutData();
        gd.horizontalSpan = 2;
        gd.widthHint = 400;
        commentWidget.setLayoutData(gd);
    }

    @SuppressWarnings("nls")
    private void addSpecimenTypeWidgets(Composite contents) {
        boolean useStudyOnlySourceSpecimens = true;
        SourceSpecimen ss = null;
        SpecimenType type = internalSpecimen.specimen.getSpecimenType();
        if (type != null) {
            ss = mapStudySourceSpecimen.get(type.getName());
        }
        if (ss == null && type != null
            && allSpecimenTypes.contains(type)) {
            useStudyOnlySourceSpecimens = false;
        }
        specimenTypeComboViewer =
            getWidgetCreator().createComboViewer(contents,
                SpecimenType.NAME.singular().toString(),
                mapStudySourceSpecimen.values(), ss,
                // TR: validation error message
                i18n.tr("A specimen type should be selected"),
                new ComboSelectionUpdate() {
                    @Override
                    public void doSelection(Object selectedObject) {
                        if (selectedObject instanceof SourceSpecimen) {
                            internalSpecimen
                            .specimen
                                .setSpecimenType(((SourceSpecimen) selectedObject)
                                    .getSpecimenType());
                        } else {
                            internalSpecimen
                            .specimen
                                .setSpecimenType(((SpecimenType) selectedObject));
                        }
                        updateWidgetVisibilityAndValuesNoTimeReset();
                    }
                }, new BiobankLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof SourceSpecimen) {
                            return ((SourceSpecimen) element).getSpecimenType()
                                .getNameShort();
                        }
                        return ((SpecimenType) element).getNameShort();
                    }
                });
        if (!useStudyOnlySourceSpecimens) {
            specimenTypeComboViewer.setInput(allSpecimenTypes);
            specimenTypeComboViewer.setSelection(new StructuredSelection(
                type));
        }

        final Button allSpecimenTypesCheckBox = new Button(contents, SWT.CHECK);
        allSpecimenTypesCheckBox.setText(
            // TR: checkbox text
            i18n.tr("Show only study source specimens"));
        allSpecimenTypesCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (allSpecimenTypesCheckBox.getSelection()) {
                    specimenTypeComboViewer.setInput(mapStudySourceSpecimen
                        .values());
                } else {
                    specimenTypeComboViewer.setInput(allSpecimenTypes);
                }
            }
        });
        allSpecimenTypesCheckBox.setSelection(useStudyOnlySourceSpecimens);
    }

    public void updateWidgetVisibilityAndValues() {
        updateWidgetVisibilityAndValuesNoTimeReset();

        if (defaultTimeDrawn != null) {
            timeDrawnWidget.setDate(defaultTimeDrawn);
        }
    }

    @SuppressWarnings("nls")
    public void updateWidgetVisibilityAndValuesNoTimeReset() {
        if (!dialogCreated) return;

        SourceSpecimen ss = null;
        SpecimenType type = internalSpecimen.specimen.getSpecimenType();
        if (type != null) {
            ss = mapStudySourceSpecimen.get(type.getName());
        }
        boolean enableVolume = ((type != null) && ((ss == null) || ss.getNeedOriginalVolume()));
        boolean isVolumeRequired = ((ss != null) && ss.getNeedOriginalVolume());

        quantityLabel.setVisible(enableVolume);
        quantityText.setVisible(enableVolume);
        quantityTextValidator.setAllowEmpty(!enableVolume || !isVolumeRequired);
        String originalText = quantityText.getText();
        quantityText.setText(originalText + "*");
        quantityText.setText(originalText);
    }

    /**
     * Used only when editing
     */
    @Override
    protected void okPressed() {
        copy(editedSpecimen);
        super.okPressed();
    }

    @Override
    protected CommentedSpecimenInfo getNew() {
        return new CommentedSpecimenInfo(new SpecimenInfo());
    }

    @Override
    protected void resetFields() {
        inventoryIdWidget.setText(StringUtil.EMPTY_STRING);
        inventoryIdWidget.setFocus();
        quantityText.setText(StringUtil.EMPTY_STRING);
        timeDrawnWidget.setDate(null);
        specimenTypeComboViewer.getCombo().deselectAll();
        activityStatusComboViewer.setSelection(
            new StructuredSelection(ActivityStatus.ACTIVE));
        commentWidget.setText(StringUtil.EMPTY_STRING);
        updateWidgetVisibilityAndValues();
    }

    @SuppressWarnings("nls")
    @Override
    protected void copy(Object newModelObject) {
        CommentedSpecimenInfo spec =
            (CommentedSpecimenInfo) newModelObject;
        spec.specimen
            .setInventoryId(internalSpecimen.specimen.getInventoryId());
        spec.specimen.setSpecimenType(internalSpecimen.specimen
            .getSpecimenType());
        spec.specimen.setQuantity(internalSpecimen.specimen.getQuantity());
        spec.specimen.setCreatedAt(internalSpecimen.specimen.getCreatedAt());
        if (commentWrapper.getMessage() != null
            && !commentWrapper.getMessage().isEmpty()) {
            spec.comments.add(commentWrapper.getMessage());
        }
        if (spec.comments.size() > 0)
            i18n.trc("yes abbeviation", "Y");
        else
            i18n.trc("no abbeviation", "N");
        spec.specimen.setActivityStatus(internalSpecimen.specimen
            .getActivityStatus());
        inventoryIdExcludeList.add(internalSpecimen.specimen.getInventoryId());
    }
}
