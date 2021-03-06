package edu.ualberta.med.biobank.preferences;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.BiobankPlugin;
import edu.ualberta.med.biobank.utils.FilePromptUtil;

public class LinkAssignPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {
    private static final I18n i18n = I18nFactory
        .getI18n(LinkAssignPreferencePage.class);

    private DirectoryFieldEditor logPath;

    public LinkAssignPreferencePage() {
        super(GRID);
        setPreferenceStore(BiobankPlugin.getDefault().getPreferenceStore());
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @SuppressWarnings("nls")
    @Override
    public void createFieldEditors() {
        addField(new StringFieldEditor(PreferenceConstants.GENERAL_CONFIRM,
            i18n.tr("Confirm barcode:"),
            getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.GENERAL_CANCEL,
            i18n.tr("Cancel barcode:"),
            getFieldEditorParent()));
        addField(new BooleanFieldEditor(
            PreferenceConstants.LINK_ASSIGN_ACTIVITY_LOG_INTO_FILE,
            i18n.tr("Save activity logs into a file"),
            getFieldEditorParent()));
        logPath = new DirectoryFieldEditor(
            PreferenceConstants.LINK_ASSIGN_ACTIVITY_LOG_PATH,
            i18n.tr("Path for activity log files"),
            getFieldEditorParent());
        addField(logPath);
        addField(new BooleanFieldEditor(
            PreferenceConstants.LINK_ASSIGN_ACTIVITY_LOG_ASK_PRINT,
            i18n.tr("Ask to print activity log"),
            getFieldEditorParent()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
        //
    }

    @Override
    public boolean performOk() {
        File file = new File(logPath.getStringValue());

        if (!FilePromptUtil.isWritableDir(file)) {
            return false;
        }

        return super.performOk();
    }
}