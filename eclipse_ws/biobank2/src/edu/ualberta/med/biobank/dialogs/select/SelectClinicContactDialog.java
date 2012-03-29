package edu.ualberta.med.biobank.dialogs.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.study.StudyGetFreeContactsAction;
import edu.ualberta.med.biobank.gui.common.dialogs.BgcBaseDialog;
import edu.ualberta.med.biobank.model.Clinic;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.widgets.infotables.entry.StudyContactEntryInfoTable;

public class SelectClinicContactDialog extends BgcBaseDialog {

    public static final int ADD_BTN_ID = 100;

    private static final String TITLE =
        Messages.SelectClinicContactDialog_dialog_title;

    private StudyContactEntryInfoTable contactInfoTable;

    private Contact selectedContact;

    private List<Contact> contacts;

    private ComboViewer clinicCombo;

    public SelectClinicContactDialog(Shell parent, List<Contact> contacts) {
        super(parent);
        this.contacts = contacts;
    }

    @Override
    protected String getDialogShellTitle() {
        return TITLE;
    }

    @Override
    protected String getTitleAreaMessage() {
        return Messages.SelectClinicContactDialog_description;
    }

    @Override
    protected String getTitleAreaTitle() {
        return Messages.SelectClinicContactDialog_main_title;
    }

    @Override
    protected void createDialogAreaInternal(final Composite parent)
        throws Exception {
        final Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(2, false));
        GridData cgd = new GridData(SWT.FILL, SWT.FILL, true, true);
        contents.setLayoutData(cgd);

        LabelProvider labelProvider = new LabelProvider() {
            @Override
            public String getText(Object o) {
                return ((Clinic) o).getNameShort();
            }
        };

        StudyGetFreeContactsAction action =
            new StudyGetFreeContactsAction();

        List<Contact> allContacts =
            (List<Contact>) SessionManager.getAppService().doAction(action)
                .getList();
        allContacts.removeAll(contacts);

        HashSet<Clinic> clinics = new HashSet<Clinic>();
        for (Contact contact : allContacts)
            clinics.add(contact.getClinic());

        clinicCombo = widgetCreator.createComboViewer(contents,
            Messages.SelectClinicContactDialog_clinic_label,
            new ArrayList<Clinic>(clinics), null, labelProvider);
        clinicCombo
            .addSelectionChangedListener(new ISelectionChangedListener() {

                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    filterContacts((Clinic) ((StructuredSelection) event
                        .getSelection()).getFirstElement());
                    getShell().setSize(
                        contents.getParent().getParent()
                            .computeSize(SWT.DEFAULT, getShell().getSize().y));
                }
            });

        contactInfoTable = new StudyContactEntryInfoTable(contents,
            new ArrayList<Contact>());
        contactInfoTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (contactInfoTable.getSelection() != null)
                    SelectClinicContactDialog.this.getButton(
                        IDialogConstants.OK_ID).setEnabled(true);
            }
        });
        GridData gd = new GridData(SWT.FILL, SWT.NONE, true, true);
        gd.horizontalSpan = 2;
        contactInfoTable.setLayoutData(gd);
        contactInfoTable.setEnabled(true);

    }

    protected void filterContacts(Clinic clinic) {
        Collection<Contact> clinicContacts = clinic.getContacts();
        for (Contact contact : contacts)
            clinicContacts.remove(contact);
        contactInfoTable.setList(new ArrayList<Contact>(clinicContacts));
    }

    @Override
    protected void okPressed() {
        selectedContact = contactInfoTable.getSelection();
        super.okPressed();
    }

    public Contact getSelection() {
        return selectedContact;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

}
