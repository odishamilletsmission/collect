/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.tasks;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.formmanagement.FormsDataService;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.formmanagement.ServerFormsDetailsFetcher;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.projects.ProjectsDataService;
import org.odk.collect.android.utilities.WebCredentialsUtils;
import org.odk.collect.android.wassan.app.UserProject;
import org.odk.collect.android.wassan.model.User;
import org.odk.collect.forms.FormSourceException;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.settings.keys.MetaKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Background task for downloading forms from urls or a formlist from a url. We overload this task
 * a bit so that we don't have to keep track of two separate downloading tasks and it simplifies
 * interfaces. If LIST_URL is passed to doInBackground(), we fetch a form list. If a hashmap
 * containing form/url pairs is passed, we download those forms.
 *
 * @author carlhartung
 *
 * @deprecated Server form list should be retrieved from {@link FormsDataService}
 */
@Deprecated
public class DownloadFormListTask extends AsyncTask<Void, String, Pair<List<ServerFormDetails>, FormSourceException>> {

    private final ServerFormsDetailsFetcher serverFormsDetailsFetcher;

    private FormListDownloaderListener stateListener;
    private WebCredentialsUtils webCredentialsUtils;
    private String url;
    private String username;
    private String password;

    @Inject
    FormsDataService formsDataService;

    @Inject
    ProjectsDataService projectsDataService;

    @Inject
    SettingsProvider settingsProvider;

    public DownloadFormListTask(ServerFormsDetailsFetcher serverFormsDetailsFetcher) {
        this.serverFormsDetailsFetcher = serverFormsDetailsFetcher;
        DaggerUtils.getComponent(Collect.getInstance()).inject(this);
    }

    @Override
    protected Pair<List<ServerFormDetails>, FormSourceException> doInBackground(Void... values) {
        formsDataService.refresh(projectsDataService.requireCurrentProject().getUuid());

        if (webCredentialsUtils != null) {
            setTemporaryCredentials();
        }

        List<ServerFormDetails> formList = null;
        FormSourceException exception = null;

        try {
            formList = serverFormsDetailsFetcher.fetchFormDetails();
            //added by niranjan
            processAndFilterFormList(formList);
        } catch (FormSourceException e) {
            exception = e;
        } finally {
            if (webCredentialsUtils != null) {
                clearTemporaryCredentials();
            }
        }

        return new Pair<>(formList, exception);
    }
    //add by niranjan
    private void processAndFilterFormList(List<ServerFormDetails> formList) {
        if (formList == null || formList.isEmpty()) return;

        String userJson = settingsProvider.getMetaSettings().getString(MetaKeys.KEY_USER);
        String currentProjectId = settingsProvider.getMetaSettings().getString(MetaKeys.CURRENT_PROJECT_ID);
        if (userJson == null || currentProjectId == null) return;

        Gson gson = new Gson();
        JsonObject root = JsonParser.parseString(userJson).getAsJsonObject();
        JsonArray projectsJson = root.getAsJsonArray("userProjects");

        if (projectsJson == null || projectsJson.size() == 0) return;

        // Find the matching UserProject for the current project
        JsonArray assignFormsArray = null;
        for (JsonElement element : projectsJson) {
            UserProject project = gson.fromJson(element, UserProject.class);
            if (currentProjectId.equals(project.getCentral_project_id())) {
                assignFormsArray = gson.toJsonTree(project.getAssign_forms()).getAsJsonArray();
                break;
            }
        }

        if (assignFormsArray == null || assignFormsArray.size() == 0) return;

        // Extract assign form IDs
        List<String> assignFormIds = new ArrayList<>();
        for (JsonElement element : assignFormsArray) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                assignFormIds.add(element.getAsString());
            }
        }

        if (assignFormIds.isEmpty()) return;

        // Filter formList based on assigned form IDs
        formList.removeIf(form -> form.getFormId() == null || !assignFormIds.contains(form.getFormId()));
    }


    @Override
    protected void onPostExecute(Pair<List<ServerFormDetails>, FormSourceException> result) {
        synchronized (this) {
            if (stateListener != null) {
                if (result.first != null) {
                    HashMap<String, ServerFormDetails> detailsHashMap = new HashMap<>();
                    for (ServerFormDetails details : result.first) {
                        detailsHashMap.put(details.getFormId(), details);
                    }

                    stateListener.formListDownloadingComplete(detailsHashMap, result.second);
                } else {
                    stateListener.formListDownloadingComplete(null, result.second);
                }
            }
        }
    }

    public void setDownloaderListener(FormListDownloaderListener sl) {
        synchronized (this) {
            stateListener = sl;
        }
    }

    public void setAlternateCredentials(WebCredentialsUtils webCredentialsUtils, String url, String username, String password) {
        this.webCredentialsUtils = webCredentialsUtils;
        serverFormsDetailsFetcher.updateCredentials(webCredentialsUtils);

        this.url = url;
        if (url != null && !url.isEmpty()) {
            serverFormsDetailsFetcher.updateUrl(url);
        }

        this.username = username;
        this.password = password;
    }

    private void setTemporaryCredentials() {
        if (url != null) {
            String host = Uri.parse(url).getHost();

            if (host != null) {
                if (username != null && password != null) {
                    webCredentialsUtils.saveCredentials(url, username, password);
                } else {
                    webCredentialsUtils.clearCredentials(url);
                }
            }
        }
    }

    private void clearTemporaryCredentials() {
        if (url != null) {
            String host = Uri.parse(url).getHost();

            if (host != null) {
                webCredentialsUtils.clearCredentials(url);
            }
        }
    }
}
