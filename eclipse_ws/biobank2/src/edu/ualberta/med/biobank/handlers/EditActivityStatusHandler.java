package edu.ualberta.med.biobank.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.forms.ActivityStatusEntryForm;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.treeview.admin.SessionAdapter;
import edu.ualberta.med.biobank.treeview.admin.SiteAdapter;

public class EditActivityStatusHandler extends AbstractHandler {
    public static final String ID = "edu.ualberta.med.biobank.commands.editActivityStatusMethods";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SessionAdapter sessionAdapter = SessionManager.getInstance()
            .getSession();
        Assert.isNotNull(sessionAdapter);
        SiteWrapper siteWrapper = SessionManager.getInstance().getCurrentSite();
        SiteAdapter sa = new SiteAdapter(sessionAdapter, siteWrapper);
        try {
            PlatformUI
                .getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage()
                .openEditor(new FormInput(sa), ActivityStatusEntryForm.ID,
                    false, 0);
        } catch (Exception e) {
            throw new ExecutionException("Could not execute handler.", e);
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return (SessionManager.canCreate(ActivityStatusWrapper.class, null)
            || SessionManager.canUpdate(ActivityStatusWrapper.class, null) || SessionManager
            .canDelete(ActivityStatusWrapper.class, null))
            && (SessionManager.getInstance().getSession() != null);
    }
}
