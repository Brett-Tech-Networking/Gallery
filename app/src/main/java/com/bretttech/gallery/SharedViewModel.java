package com.bretttech.gallery;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Event<Boolean>> refreshRequest = new MutableLiveData<>();

    public LiveData<Event<Boolean>> getRefreshRequest() {
        return refreshRequest;
    }

    public void requestRefresh() {
        refreshRequest.setValue(new Event<>(true));
    }

    private final MutableLiveData<Boolean> isSelectionMode = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsSelectionMode() {
        return isSelectionMode;
    }

    public void setIsSelectionMode(boolean isSelection) {
        isSelectionMode.setValue(isSelection);
    }
}