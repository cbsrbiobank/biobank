package edu.ualberta.med.biobank.dialogs.startup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.BiobankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.security.UserPermissionsGetAction;
import edu.ualberta.med.biobank.common.action.security.UserPermissionsGetAction.UserCreatePermissions;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.CenterWrapper;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.LoginPermissionSessionState;
import edu.ualberta.med.biobank.gui.common.validators.AbstractValidator;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.helpers.SessionHelper;
import edu.ualberta.med.biobank.preferences.PreferenceConstants;
import edu.ualberta.med.biobank.rcp.Application;
import edu.ualberta.med.biobank.rcp.perspective.MainPerspective;

public class LoginDialog extends TitleAreaDialog {
    private static final I18n i18n = I18nFactory
        .getI18n(LoginDialog.class);

    private final DataBindingContext dbc;

    private final ArrayList<String> servers;

    private final ArrayList<String> userNames;

    private Combo serverWidget;

    private Combo userNameWidget;

    private Text passwordWidget;

    @SuppressWarnings("nls")
    private static final String LAST_SERVER = "lastServer";

    @SuppressWarnings("nls")
    private static final String SAVED_USER_NAMES = "savedUserNames";

    @SuppressWarnings("nls")
    private static final String USER_NAME = "userName";

    @SuppressWarnings("nls")
    private static final String LAST_USER_NAME = "lastUserName";

    @SuppressWarnings("nls")
    private static final String DEFAULT_NON_SECURE_PORT = "8080";

    @SuppressWarnings("nls")
    private static final String DEFAULT_UNSECURE_PREFIX = "http://";

    private static final BgcLogger logger = BgcLogger
        .getLogger(LoginDialog.class.getName());

    public Preferences pluginPrefs = null;

    private Button secureConnectionButton;

    private final Authentication authentication;

    private Boolean okButtonEnabled;

    private boolean setupFinished = false;

    @SuppressWarnings("nls")
    public LoginDialog(Shell parentShell) {
        super(parentShell);

        authentication = new Authentication();

        dbc = new DataBindingContext();

        servers = new ArrayList<String>();
        userNames = new ArrayList<String>();

        pluginPrefs = InstanceScope.INSTANCE.getNode(Application.PLUGIN_ID);
        Preferences prefsUserNames = pluginPrefs.node(SAVED_USER_NAMES);

        IPreferenceStore prefsStore = BiobankPlugin.getDefault()
            .getPreferenceStore();

        String serverList = prefsStore
            .getString(PreferenceConstants.SERVER_LIST);
        StringTokenizer st = new StringTokenizer(serverList, "\n");
        while (st.hasMoreTokens()) {
            servers.add(st.nextToken());
        }

        try {
            String[] userNodeNames = prefsUserNames.childrenNames();
            for (String userNodeName : userNodeNames) {
                Preferences node = prefsUserNames.node(userNodeName);
                userNames.add(node.get(USER_NAME, StringUtil.EMPTY_STRING));
            }
        } catch (BackingStoreException e) {
            logger.error("Could not get " + USER_NAME + " preference", e);
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        // login dialog title
        shell.setText(i18n.tr("BioBank Login"));
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        // login dialog title
        setTitle(i18n.tr("Login to a BioBank server"));
        setTitleImage(BgcPlugin.getDefault().getImageRegistry()
            .get(BgcPlugin.IMG_LOGINWIZ));
        // login dialog title area message
        setMessage(i18n.tr("Enter server name and login details."));
        return contents;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control contents = super.createButtonBar(parent);
        if (okButtonEnabled != null) {
            // in case the binding wanted to modify it before its creation
            setOkButtonEnabled(okButtonEnabled);
        }
        return contents;
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite parentComposite = (Composite) super.createDialogArea(parent);

        Composite contents = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        contents.setLayout(layout);
        contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        String lastServer =
            pluginPrefs.get(LAST_SERVER, StringUtil.EMPTY_STRING);
        NonEmptyStringValidator validator = new NonEmptyStringValidator(
            // validation error when server text box is empty
            i18n.tr("Server field cannot be empty"));
        serverWidget = createWritableCombo(contents,
            // server combo box label
            i18n.tr("&Server"),
            servers.toArray(new String[0]),
            Authentication.SERVER_PROPERTY_NAME, lastServer, validator);

        NonEmptyStringValidator userNameValidator = null;
        NonEmptyStringValidator passwordValidator = null;
        if (BiobankPlugin.getDefault().isDebugging()) {
            new Label(contents, SWT.NONE);
            secureConnectionButton = new Button(contents, SWT.CHECK);
            secureConnectionButton.setText("Use secure connection");
            secureConnectionButton.setSelection(!lastServer
                .contains(DEFAULT_NON_SECURE_PORT));

            serverWidget.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String lastServer = serverWidget.getText();
                    secureConnectionButton.setSelection(!lastServer
                        .contains(DEFAULT_NON_SECURE_PORT));
                }
            });
        } else {
            userNameValidator = new NonEmptyStringValidator(
                // validation error when username text box is empty
                i18n.tr("Username field cannot be empty"));
            passwordValidator = new NonEmptyStringValidator(
                // validation error when password text box is empty
                i18n.tr("Password field cannot be empty"));

        }

        userNameWidget =
            createWritableCombo(contents,
                // TR: login dialog user name text box label
                i18n.tr("&User Name"),
                userNames.toArray(new String[0]),
                Authentication.USERNAME_PROPERTY_NAME,
                pluginPrefs.get(LAST_USER_NAME, StringUtil.EMPTY_STRING),
                userNameValidator);

        passwordWidget = createPassWordText(contents,
            // TR: login dialog password text box label
            "&Password",
            Authentication.PASSWORD_PROPERTY_NAME, passwordValidator);

        bindChangeListener();

        setupFinished = true;

        return contents;
    }

    private Text createPassWordText(Composite parent, String labelText,
        String propertyObserved, AbstractValidator validator) {
        createLabel(parent, labelText);
        Text text = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        arrangeAndBindControl(text, validator,
            SWTObservables.observeText(text, SWT.Modify), propertyObserved);
        return text;
    }

    private void arrangeAndBindControl(Control control,
        AbstractValidator validator, ISWTObservableValue observable,
        String propertyObserved) {
        control.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
            false));
        UpdateValueStrategy uvs = null;
        if (validator != null) {
            uvs = new UpdateValueStrategy();
            uvs.setAfterGetValidator(validator);
        }
        dbc.bindValue(observable,
            PojoObservables.observeValue(authentication, propertyObserved),
            uvs, null);
    }

    @SuppressWarnings("nls")
    private Label createLabel(Composite parent, String labelText) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText + ":");
        label.setLayoutData(new GridData(GridData.END, GridData.CENTER, false,
            false));
        return label;
    }

    private Combo createWritableCombo(Composite parent, String labelText,
        String[] values, String propertyObserved, String selection,
        AbstractValidator validator) {
        createLabel(parent, labelText);

        Combo combo = new Combo(parent, SWT.BORDER);
        combo.setItems(values);
        if (selection != null) {
            combo.select(combo.indexOf(selection));
        }
        arrangeAndBindControl(combo, validator,
            SWTObservables.observeSelection(combo), propertyObserved);
        combo.addListener(SWT.MouseWheel, new Listener() {

            @Override
            public void handleEvent(Event event) {
                event.doit = false;
            }

        });
        return combo;
    }

    protected void bindChangeListener() {
        final IObservableValue statusObservable = new WritableValue();
        statusObservable.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                IObservableValue validationStatus = (IObservableValue) event
                    .getSource();
                IStatus status = (IStatus) validationStatus.getValue();
                if (status.getSeverity() == IStatus.OK) {
                    setErrorMessage(null);
                    setOkButtonEnabled(true);
                } else {
                    if (setupFinished) {
                        setErrorMessage(status.getMessage());
                    }
                    setOkButtonEnabled(false);
                }
            }
        });
        dbc.bindValue(statusObservable,
            new AggregateValidationStatus(dbc.getBindings(),
                AggregateValidationStatus.MAX_SEVERITY));
    }

    protected void setOkButtonEnabled(boolean enabled) {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null && !okButton.isDisposed()) {
            okButton.setEnabled(enabled);
        } else {
            okButtonEnabled = enabled;
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected void okPressed() {
        try {
            new URL(DEFAULT_UNSECURE_PREFIX + serverWidget.getText());
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(),
                // TR: error dialog title
                i18n.tr("Invalid Server URL"),
                // TR: error dialog message
                i18n.tr("Please enter a valid server URL."));
            return;
        }

        if (!BiobankPlugin.getDefault().isDebugging()) {
            // until further notice, we still want to be able to specify the
            // port, even in non debug mode
            // if (url.getPort() != -1) {
            // MessageDialog
            // .openError(getShell(), "Invalid Server URL",
            // "You are not allowed to specify a port, only a hostname and path.");
            // return;
            // }
            if (userNameWidget.getText().isEmpty()) {
                MessageDialog.openError(getShell(),
                    // TR: error dialog title
                    i18n.tr("Invalid User Name"),
                    // TR: error dialog message
                    i18n.tr("Username field cannot be empty"));
                return;
            }
        }

        boolean secureConnection =
            ((secureConnectionButton == null) || secureConnectionButton
                .getSelection());

        SessionHelper sessionHelper = new SessionHelper(serverWidget.getText(),
            secureConnection, userNameWidget.getText(),
            passwordWidget.getText());

        BusyIndicator.showWhile(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell().getDisplay(), sessionHelper);

        if (sessionHelper.getUser() != null) {
            finalizeConnection(sessionHelper);
        }
        super.okPressed();
    }

    protected void finalizeConnection(final SessionHelper sessionHelper) {
        BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                selectWorkingCenter(sessionHelper);
                if (sessionHelper.getUser().getCurrentWorkingCenter() == null) {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    IWorkbenchWindow activeWindow = workbench
                        .getActiveWorkbenchWindow();
                    IWorkbenchPage page = activeWindow.getActivePage();
                    if (!page.getPerspective().getId()
                        .equals(MainPerspective.ID)) {
                        try {
                            workbench.showPerspective(MainPerspective.ID,
                                activeWindow);
                        } catch (WorkbenchException e) {
                            BgcPlugin.openAsyncError(
                                // error dialog title
                                i18n.tr("Error while opening main perspective"),
                                e);
                        }
                    }
                }
                if (sessionHelper.getUser().isSuperAdmin()
                    || sessionHelper.getUser().getCurrentWorkingCenter() != null) {
                    // login successful
                    savePreferences();
                    SessionManager.getInstance().addSession(
                        sessionHelper.getAppService(), serverWidget.getText(),
                        sessionHelper.getUser());
                }
            }
        });
    }

    @SuppressWarnings("nls")
    private void savePreferences() {
        pluginPrefs.put(LAST_SERVER, serverWidget.getText());
        pluginPrefs.put(LAST_USER_NAME, userNameWidget.getText());

        if ((serverWidget.getText().length() > 0)
            && (serverWidget.getSelectionIndex() == -1)
            && !servers.contains(serverWidget.getText())) {
            IPreferenceStore prefsStore = BiobankPlugin.getDefault()
                .getPreferenceStore();
            StringBuilder serverList = new StringBuilder();
            for (String server : servers) {
                serverList.append(server);
                serverList.append("\n");
            }
            prefsStore.putValue(PreferenceConstants.SERVER_LIST, serverList
                .append(serverWidget.getText().trim()).toString());
        }

        if ((userNameWidget.getText().length() > 0)
            && (userNameWidget.getSelectionIndex() == -1)
            && !userNames.contains(userNameWidget.getText())) {
            Preferences prefsUserNames = pluginPrefs.node(SAVED_USER_NAMES);
            Preferences prefsUserName = prefsUserNames.node(Integer
                .toString(userNames.size()));
            prefsUserName.put(USER_NAME, userNameWidget.getText().trim());
        }

        try {
            pluginPrefs.flush();
        } catch (BackingStoreException e) {
            logger.error("Could not save loggin preferences", e);
        }
    }

    @SuppressWarnings("nls")
    private void selectWorkingCenter(final SessionHelper sessionHelper) {

        List<CenterWrapper<?>> workingCenters = null;
        try {
            workingCenters = sessionHelper.getUser().getWorkingCenters();
        } catch (Exception e) {
            BgcPlugin.openAsyncError(
                // TR: error dialog title
                i18n.tr("Problem getting user working centers"), e);
        }

        if (workingCenters != null) {
            if (workingCenters.size() == 0) {
                if (!sessionHelper.getUser().isSuperAdmin())
                    // cannot access the application.
                    BgcPlugin
                        .openError(
                            // TR: error dialog title
                            i18n.tr("Problem getting user working centers"),
                            // TR: error dialog message
                            i18n.tr("No working center has been found for this user. Check with your manager or application administrator for user rights."));
            } else if (workingCenters.size() == 1)
                sessionHelper.getUser().setCurrentWorkingCenter(
                    workingCenters.get(0));
            else
                new WorkingCenterSelectDialog(getShell(),
                    sessionHelper.getUser(), workingCenters).open();
        }

        if (sessionHelper.getUser().getCurrentWorkingCenter() == null
            && !sessionHelper.getUser().isSuperAdmin()) {
            if (sessionHelper.getUser().isSuperAdmin()) {
                // connect in admin mode
                BgcPlugin
                    .openInformation(
                        // TR: information dialog title
                        i18n.tr("Super administrator mode"),
                        // TR: information dialog message
                        i18n.tr("No working center has been found or selected for this user. You are super administrator, so you will be logged on with no working center. Only non center specific actions will be available. "));
                // open the administration perspective if another
                // perspective is open
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow activeWindow = workbench
                    .getActiveWorkbenchWindow();
                IWorkbenchPage page = activeWindow.getActivePage();
                if (!page.getPerspective().getId().equals(MainPerspective.ID)) {
                    try {
                        workbench.showPerspective(MainPerspective.ID,
                            activeWindow);
                    } catch (WorkbenchException e) {
                        BgcPlugin.openAsyncError(
                            // TR: error dialog title
                            i18n.tr("Error while opening main perspective"),
                            e);
                    }
                }
            } else {
                // can't connect without a working center
                BgcPlugin
                    .openAsyncError(
                        // error dialog title
                        i18n.tr("Working center selection"),
                        // error dialog message
                        i18n.tr("You need to select the center you want to work with."));
            }
        }

        // get create permissions - note working center can be null
        try {
            Integer centerId = null;

            if (sessionHelper.getUser().getCurrentWorkingCenter() != null) {
                centerId =
                    sessionHelper.getUser().getCurrentWorkingCenter().getId();
            }

            UserCreatePermissions userCreatePermissions = sessionHelper
                .getAppService().doAction(
                    new UserPermissionsGetAction(centerId));

            LoginPermissionSessionState loginPermissionSessionState = BgcPlugin
                .getLoginStateSourceProvider();
            loginPermissionSessionState
                .setUserCreatePermissions(userCreatePermissions);
        } catch (Exception e) {
            BgcPlugin.openAsyncError(
                // TR: error dialog title
                i18n.tr("Problem getting user create permissions"), e);
        }
    }

    @SuppressWarnings("nls")
    public static class Authentication {
        public static final String SERVER_PROPERTY_NAME = "server";
        public static final String USERNAME_PROPERTY_NAME = "username";
        public static final String PASSWORD_PROPERTY_NAME = "password";

        public String server;
        public String username;
        public String password;

        public void setServer(String server) {
            this.server = server;
        }

        public String getServer() {
            return server;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return server + "/" + username + "/" + password;
        }
    }

}
