package edu.ualberta.med.biobank.dialogs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.BiobankPlugin;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.dialogs.BgcBaseDialog;

public class ExportErrorsLogsDialog extends BgcBaseDialog {
    private static final I18n i18n = I18nFactory
        .getI18n(ExportErrorsLogsDialog.class);

    // TR: dialog title
    @SuppressWarnings("nls")
    public static final String TITLE = i18n.tr("Errors logs export options");

    @SuppressWarnings("nls")
    public static final String DEFAULT_FILE_NAME = "log_export.zip";

    @SuppressWarnings("nls")
    public static final String ZIP_EXTENSION = ".zip";

    @SuppressWarnings("nls")
    public static final String LOG_EXTENSION = ".log";

    @SuppressWarnings("nls")
    public static final String EXPORT_ERRORS_LOGS_PROPERTIES_FILE =
        "ExportErrorsLogs.properties";

    @SuppressWarnings("nls")
    public static final String SCANNER_FILES_KEY = "scanner_files";

    @SuppressWarnings("nls")
    public static final String OTHERS_KEY = "others_working_directory_files";

    @SuppressWarnings("nls")
    public static final String SEPARATOR_KEY = "separator";

    private Button scannerExportCheck;

    private boolean needExportScannerInfos;

    private Button linkAssignLogsCheck;

    private boolean needActivityLogsInfos;

    public ExportErrorsLogsDialog(Shell parentShell) {
        super(parentShell);
    }

    @SuppressWarnings("nls")
    @Override
    protected String getTitleAreaMessage() {
        // TR: dialog title area message
        return i18n.tr("Select the options that fits your needs");
    }

    @Override
    protected String getTitleAreaTitle() {
        return TITLE;
    }

    @Override
    protected String getDialogShellTitle() {
        return TITLE;
    }

    @SuppressWarnings("nls")
    @Override
    protected void createDialogAreaInternal(Composite parent) throws Exception {
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));
        contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        linkAssignLogsCheck = new Button(contents, SWT.CHECK);
        linkAssignLogsCheck
            .setText(
            // TR: checkbox text
            i18n.tr("Export activity logs from Scan Link or Scan Assign or Cabinet Link Assign"));
        scannerExportCheck = new Button(contents, SWT.CHECK);
        scannerExportCheck
            // TR: checkbox text
            .setText(i18n.tr("Export scanner informations"));
    }

    @SuppressWarnings("nls")
    @Override
    protected void okPressed() {
        String selected = openFileSelection();
        // need to get it now, later will be disposed
        needActivityLogsInfos = linkAssignLogsCheck.getSelection();
        needExportScannerInfos = scannerExportCheck.getSelection();
        if (selected != null) {
            if (!selected.endsWith(ZIP_EXTENSION))
                selected += ZIP_EXTENSION;
            File f = new File(selected);
            File parentFolder = f.getParentFile();
            if (parentFolder.canWrite())
                createZip(selected);
            else {
                BgcPlugin.openAsyncError(
                    // TR: dialog title
                    i18n.tr("Path problem"),
                    // TR: dialog message
                    i18n.tr("Cannot write in ''{0}''",
                        parentFolder.getAbsolutePath()));
                openFileSelection();
            }
        }
        super.okPressed();
    }

    private void createZip(final String zipFile) {
        Display.getDefault().asyncExec(new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                ZipOutputStream out = null;
                FileInputStream in = null;
                try {
                    out = new ZipOutputStream(new FileOutputStream(zipFile));

                    List<File> files = getFiles();
                    // Compress the files
                    for (File file : files) {
                        in = new FileInputStream(file);
                        // Add ZIP entry to output stream.
                        out.putNextEntry(new ZipEntry(file.getName()));
                        // Transfer bytes from the file to the ZIP file
                        int len;
                        byte[] buf = new byte[1024];
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        // Complete the entry
                        out.closeEntry();
                        in.close();
                    }
                } catch (FileNotFoundException e) {
                    openError(e);
                } catch (IOException e) {
                    openError(e);
                } finally {
                    try {
                        if (in != null)
                            in.close();
                        // Complete the ZIP file
                        if (out != null)
                            out.close();
                    } catch (IOException e) {
                        BgcPlugin
                            .openAsyncError(
                                // TR: dialog title
                                i18n.tr("Error closing files"),
                                e);
                    }
                }
                exportComplete(zipFile);
            }
        });
    }

    @SuppressWarnings("nls")
    protected List<File> getFiles() {
        List<File> files = new ArrayList<File>();
        // .log file
        files.add(Platform.getLogFileLocation().toFile());
        // activitylog files of the current day
        if (needActivityLogsInfos) {
            String activityLogsPath = BiobankPlugin.getActivityLogPath();
            File activityLogsFolder = new File(activityLogsPath);
            if (activityLogsFolder.exists() && activityLogsFolder.isDirectory()) {
                final Calendar todayDate = Calendar.getInstance();
                File[] activityLogsFiles = activityLogsFolder
                    .listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            if (file.isFile()
                                && file.getName().endsWith(LOG_EXTENSION)) {
                                Calendar modificationDate = Calendar
                                    .getInstance();
                                modificationDate.setTime(new Date(file
                                    .lastModified()));
                                return todayDate.get(Calendar.DATE) == modificationDate
                                    .get(Calendar.DATE)
                                    && todayDate.get(Calendar.MONTH) == modificationDate
                                        .get(Calendar.MONTH)
                                    && todayDate.get(Calendar.YEAR) == modificationDate
                                        .get(Calendar.YEAR);
                            }
                            return false;
                        }
                    });
                files.addAll(Arrays.asList(activityLogsFiles));
            }
        }
        // working directory files listed in properties file
        Properties props = new Properties();
        try {
            props.load(ExportErrorsLogsDialog.class
                .getResourceAsStream(EXPORT_ERRORS_LOGS_PROPERTIES_FILE));
        } catch (Exception e) {
            BgcPlugin.openAsyncError(
                // TR: dialog title
                i18n.tr("Error retrieving log file list"), e);
        }
        if (needExportScannerInfos)
            files
                .addAll(getFilesListFromPropertiesKey(props, SCANNER_FILES_KEY));
        files.addAll(getFilesListFromPropertiesKey(props, OTHERS_KEY));
        return files;
    }

    private List<File> getFilesListFromPropertiesKey(Properties props,
        String key) {
        List<File> files = new ArrayList<File>();
        String listString = props.getProperty(key);
        String separator = props.getProperty(SEPARATOR_KEY);
        if (listString != null) {
            for (String s : listString.split(separator)) {
                File f = new File(s);
                if (f.exists()) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    @SuppressWarnings("nls")
    protected void exportComplete(String zipFile) {
        BgcPlugin.openInformation(
            // TR: dialog title
            i18n.tr("Export complete"),
            // TR: dialog message
            i18n.tr("Log informations have been successfully exported in file "
                + zipFile));

    }

    @SuppressWarnings("nls")
    protected void openError(Exception e) {
        BgcPlugin.openAsyncError(
            // TR: dialog title
            i18n.tr("Problem while exporting"), e);
    }

    @SuppressWarnings("nls")
    private String openFileSelection() {
        FileDialog fd = new FileDialog(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell(), SWT.SAVE);
        // TR: file select dialog title
        fd.setText(i18n.tr("Select destination zip file"));
        fd.setFilterExtensions(new String[] { "*" + ZIP_EXTENSION });
        fd.setFileName(DEFAULT_FILE_NAME);
        String selected = fd.open();
        return selected;
    }

}
