package org.odk.collect.android.wassan.app;

import android.net.Uri;

import androidx.loader.content.CursorLoader;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.instances.DatabaseInstanceColumns;
import org.odk.collect.android.external.InstancesContract;
import org.odk.collect.android.projects.ProjectsDataService;
import org.odk.collect.forms.instances.Instance;

import java.util.Arrays;

@Deprecated
public class MyCursorLoaderFactory {

    public static final String INTERNAL_QUERY_PARAM = "internal";
    private final ProjectsDataService projectsDataService;

    public MyCursorLoaderFactory(ProjectsDataService projectsDataService) {
        this.projectsDataService = projectsDataService;
    }

    public CursorLoader createSentInstancesCursorLoader(CharSequence charSequence, String sortOrder , String filterId) {
        CursorLoader cursorLoader;
        if (charSequence.length() == 0) {
            String selection = DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?";
            String[] selectionArgs = {Instance.STATUS_SUBMITTED, Instance.STATUS_SUBMISSION_FAILED};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        } else {
            String selection =
                    "(" + DatabaseInstanceColumns.STATUS + "=? or "
                            + DatabaseInstanceColumns.STATUS + "=?) and "
                            + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {
                    Instance.STATUS_SUBMITTED,
                    Instance.STATUS_SUBMISSION_FAILED,
                    "%" + charSequence + "%"};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        }

        return cursorLoader;
    }

    public CursorLoader createEditableInstancesCursorLoader(CharSequence charSequence, String sortOrder,String filterId) {
        String selection;
        String[] selectionArgs;

        if (charSequence.length() == 0) {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? OR " + DatabaseInstanceColumns.STATUS + "=? OR " + DatabaseInstanceColumns.STATUS + "=?)";
            selectionArgs = new String[]{Instance.STATUS_INCOMPLETE, Instance.STATUS_INVALID, Instance.STATUS_VALID};

        } else {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? OR " + DatabaseInstanceColumns.STATUS + "=? OR " + DatabaseInstanceColumns.STATUS + "=?)" +
                    " AND " + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[]{
                    Instance.STATUS_INCOMPLETE, Instance.STATUS_INVALID, Instance.STATUS_VALID,
                    "%" + charSequence + "%"
            };
        }

        // Add filterId condition without changing selectionArgs
        if (filterId != null) {
            selection += " AND " + DatabaseInstanceColumns.JR_FORM_ID + " = ?";
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = filterId.toString();

        }

        return getInstancesCursorLoader(selection, selectionArgs, sortOrder);
    }

    public CursorLoader createFinalizedInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;
        if (charSequence.length() == 0) {
            String selection = DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?";
            String[] selectionArgs = {Instance.STATUS_COMPLETE, Instance.STATUS_SUBMISSION_FAILED};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        } else {
            String selection =
                    "(" + DatabaseInstanceColumns.STATUS + "=? or "
                            + DatabaseInstanceColumns.STATUS + "=?) and "
                            + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {
                    Instance.STATUS_COMPLETE,
                    Instance.STATUS_SUBMISSION_FAILED,
                    "%" + charSequence + "%"};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        }

        return cursorLoader;
    }

    public CursorLoader createCompletedUndeletedInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;
        if (charSequence.length() == 0) {
            String selection = DatabaseInstanceColumns.DELETED_DATE + " IS NULL and ("
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=?)";

            String[] selectionArgs = {Instance.STATUS_COMPLETE,
                    Instance.STATUS_SUBMISSION_FAILED,
                    Instance.STATUS_SUBMITTED};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        } else {
            String selection = DatabaseInstanceColumns.DELETED_DATE + " IS NULL and ("
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=?) and "
                    + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";

            String[] selectionArgs = {
                    Instance.STATUS_COMPLETE,
                    Instance.STATUS_SUBMISSION_FAILED,
                    Instance.STATUS_SUBMITTED,
                    "%" + charSequence + "%"};

            cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        }
        return cursorLoader;
    }

    private CursorLoader getInstancesCursorLoader(String selection, String[] selectionArgs, String sortOrder) {
        Uri uri = InstancesContract.getUri(projectsDataService.requireCurrentProject().getUuid());

        return new CursorLoader(
                Collect.getInstance(),
                getUriWithAnalyticsParam(uri),
                null,
                selection,
                selectionArgs,
                sortOrder);
    }

    private Uri getUriWithAnalyticsParam(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(INTERNAL_QUERY_PARAM, "true")
                .build();
    }
}

