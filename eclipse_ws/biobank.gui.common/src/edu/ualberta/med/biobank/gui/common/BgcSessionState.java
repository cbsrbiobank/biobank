package edu.ualberta.med.biobank.gui.common;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

public class BgcSessionState extends AbstractSourceProvider {

    public final static String SESSION_STATE_SOURCE_NAME = "edu.ualberta.med.biobank.gui.common.sourceprovider.loginState";

    public final static String LOGGED_IN = "loggedIn";

    public final static String LOGGED_OUT = "loggedOut";

    private boolean loggedIn;

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] { SESSION_STATE_SOURCE_NAME };
    }

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, String> getCurrentState() {
        Map<String, String> currentStateMap = new HashMap<String, String>(1);
        String currentState = loggedIn ? LOGGED_IN : LOGGED_OUT;
        currentStateMap.put(SESSION_STATE_SOURCE_NAME, currentState);
        return currentStateMap;
    }

    public void setLoggedInState(boolean loggedIn) {
        if (this.loggedIn == loggedIn)
            return; // no change
        this.loggedIn = loggedIn;
        String currentState = loggedIn ? LOGGED_IN : LOGGED_OUT;
        fireSourceChanged(ISources.WORKBENCH, SESSION_STATE_SOURCE_NAME,
            currentState);
    }

}
