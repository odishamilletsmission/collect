package org.odk.collect.android.upload;

import androidx.annotation.NonNull;

import org.odk.collect.android.instancemanagement.send.FormUploadException;
import org.odk.collect.forms.instances.Instance;
import org.odk.collect.forms.instances.InstancesRepository;

public abstract class InstanceUploader {
    protected final InstancesRepository instancesRepository;

    public InstanceUploader(InstancesRepository instancesRepository) {
        this.instancesRepository = instancesRepository;
    }

    public abstract String uploadOneSubmission(Instance instance, String urlString) throws FormUploadException;

    @NonNull
    public abstract String getUrlToSubmitTo(Instance currentInstance, String deviceId, String overrideURL, String urlFromSettings);

    protected void markSubmissionFailed(Instance instance) {
        instancesRepository.save(new Instance.Builder(instance)
                .status(Instance.STATUS_SUBMISSION_FAILED)
                .build());
    }

    protected void markSubmissionComplete(Instance instance) {
        instancesRepository.save(new Instance.Builder(instance)
                .status(Instance.STATUS_SUBMITTED)
                .build());
    }

    public static final String FAIL = "Error: ";
}
