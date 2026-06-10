package org.odk.collect.android.dao;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.loader.content.CursorLoader;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.instances.DatabaseInstanceColumns;
import org.odk.collect.android.external.InstancesContract;
import org.odk.collect.android.projects.ProjectsDataService;
import org.odk.collect.android.wassan.app.FilterHelper;
import org.odk.collect.forms.instances.Instance;

import java.util.Arrays;

@Deprecated
public class CursorLoaderFactory {

    public static final String INTERNAL_QUERY_PARAM = "internal";
    private final ProjectsDataService projectsDataService;

    public CursorLoaderFactory(ProjectsDataService projectsDataService) {
        this.projectsDataService = projectsDataService;
    }

    //update bu Niranjan
    public CursorLoader createSentInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;

        String selection;
        String[] selectionArgs;

        if (charSequence.length() == 0) {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?)";
            selectionArgs = new String[]{Instance.STATUS_SUBMITTED, Instance.STATUS_SUBMISSION_FAILED};
        } else {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?) and "
                    + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[]{Instance.STATUS_SUBMITTED, Instance.STATUS_SUBMISSION_FAILED, "%" + charSequence + "%"};
        }

        // Append FILTER_ID from FilterHelper if available
        String filterId = FilterHelper.getInstance().getFilterId();
        if (filterId != null && !filterId.isEmpty()) {
            selection += " AND " + DatabaseInstanceColumns.JR_FORM_ID + " = ?";
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = filterId;
        }

        cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);

        return cursorLoader;
    }
    //update by Niranjan
    public CursorLoader createEditableInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;
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

        // Append dynamic FILTER_ID from FilterHelper
        String filterId = FilterHelper.getInstance().getFilterId();
        if (filterId != null && !filterId.isEmpty()) {
            selection += " AND " + DatabaseInstanceColumns.JR_FORM_ID + " = ?";
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = filterId;
        }


        // 🔹 Create CursorLoader with final selection
        cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);
        return cursorLoader;
    }

    // update by Niranjan
    public CursorLoader createFinalizedInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;

        String selection;
        String[] selectionArgs;

        if (charSequence.length() == 0) {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?)";
            selectionArgs = new String[]{Instance.STATUS_COMPLETE, Instance.STATUS_SUBMISSION_FAILED};
        } else {
            selection = "(" + DatabaseInstanceColumns.STATUS + "=? or " + DatabaseInstanceColumns.STATUS + "=?) and "
                    + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[]{Instance.STATUS_COMPLETE, Instance.STATUS_SUBMISSION_FAILED, "%" + charSequence + "%"};
        }

        // Append FILTER_ID from FilterHelper if available
        String filterId = FilterHelper.getInstance().getFilterId();
        if (filterId != null && !filterId.isEmpty()) {
            selection += " AND " + DatabaseInstanceColumns.JR_FORM_ID + " = ?";
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = filterId;
        }

        cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);

        return cursorLoader;
    }

    //update by Niranjan
    public CursorLoader createCompletedUndeletedInstancesCursorLoader(CharSequence charSequence, String sortOrder) {
        CursorLoader cursorLoader;

        String selection;
        String[] selectionArgs;

        if (charSequence.length() == 0) {
            selection = DatabaseInstanceColumns.DELETED_DATE + " IS NULL and ("
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=?)";

            selectionArgs = new String[]{Instance.STATUS_COMPLETE,
                    Instance.STATUS_SUBMISSION_FAILED,
                    Instance.STATUS_SUBMITTED};
        } else {
            selection = DatabaseInstanceColumns.DELETED_DATE + " IS NULL and ("
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=? or "
                    + DatabaseInstanceColumns.STATUS + "=?) and "
                    + DatabaseInstanceColumns.DISPLAY_NAME + " LIKE ?";

            selectionArgs = new String[]{Instance.STATUS_COMPLETE,
                    Instance.STATUS_SUBMISSION_FAILED,
                    Instance.STATUS_SUBMITTED,
                    "%" + charSequence + "%"};
        }

        // Append FILTER_ID from FilterHelper if available
        String filterId = FilterHelper.getInstance().getFilterId();
        if (filterId != null && !filterId.isEmpty()) {
            selection += " AND " + DatabaseInstanceColumns.JR_FORM_ID + " = ?";
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = filterId;
        }

        cursorLoader = getInstancesCursorLoader(selection, selectionArgs, sortOrder);

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
