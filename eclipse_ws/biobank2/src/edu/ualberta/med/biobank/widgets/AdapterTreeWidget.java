package edu.ualberta.med.biobank.widgets;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.treeview.AdapterBase;
import edu.ualberta.med.biobank.treeview.NodeContentProvider;
import edu.ualberta.med.biobank.treeview.NodeLabelProvider;

public class AdapterTreeWidget extends Composite {

    private TreeViewer treeViewer;

    public AdapterTreeWidget(Composite parent) {
        super(parent, SWT.NONE);

        setLayout(new FillLayout());

        // FilteredTree filteredTree = new FilteredTree(parent, SWT.BORDER
        // | SWT.MULTI | SWT.V_SCROLL, new TreeFilter(), true);
        // filteredTree.setBackground(parent.getDisplay().getSystemColor(
        // SWT.COLOR_LIST_BACKGROUND));
        // filteredTree
        // .setCursor(new Cursor(parent.getDisplay(), SWT.CURSOR_HAND));

        treeViewer = new TreeViewer(this);
        treeViewer.setLabelProvider(new NodeLabelProvider());
        treeViewer.setContentProvider(new NodeContentProvider());
        treeViewer.addDoubleClickListener(SessionManager.getInstance()
            .getDoubleClickListener());
        treeViewer.addTreeListener(new ITreeViewerListener() {
            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
            }

            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                ((AdapterBase) e.getElement()).performExpand();
            }
        });
        treeViewer.setUseHashlookup(true);
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (!selection.isEmpty()
                    && selection instanceof IStructuredSelection) {
                    AdapterBase node = (AdapterBase) ((IStructuredSelection) selection)
                        .getFirstElement();
                    IWorkbenchPartSite site = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage()
                        .getActivePart().getSite();
                    if (site instanceof IViewSite) {
                        ((IViewSite) site).getActionBars()
                            .getStatusLineManager().setMessage(node.getName());
                    }
                }
            }
        });

        Menu menu = new Menu(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell(), SWT.NONE);
        menu.addListener(SWT.Show, new Listener() {

            @Override
            public void handleEvent(Event event) {
                Menu menu = treeViewer.getTree().getMenu();
                for (MenuItem menuItem : menu.getItems()) {
                    menuItem.dispose();
                }

                Object element = ((StructuredSelection) treeViewer
                    .getSelection()).getFirstElement();
                if (element != null) {
                    ((AdapterBase) element).popupMenu(treeViewer, treeViewer
                        .getTree(), menu);
                }
            }
        });

        treeViewer.getTree().setMenu(menu);

    }

    public TreeViewer getTreeViewer() {
        return treeViewer;
    }

}
