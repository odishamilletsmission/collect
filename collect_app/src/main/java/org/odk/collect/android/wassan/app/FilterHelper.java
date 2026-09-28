package org.odk.collect.android.wassan.app;

public class FilterHelper {
    private static FilterHelper instance;
    private String filterId;

    private FilterHelper() {
        // private constructor
    }

    public static synchronized FilterHelper getInstance() {
        if (instance == null) {
            instance = new FilterHelper();
        }
        return instance;
    }

    public String getFilterId() {
        return filterId;
    }

    public void setFilterId(String filterId) {
        this.filterId = filterId;
    }

    public void clear() {
        this.filterId = null;
    }
}
