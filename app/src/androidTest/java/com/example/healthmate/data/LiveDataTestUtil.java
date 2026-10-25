package com.example.healthmate.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * Waits for a LiveData to give its first value (only used in tests).
 */
public final class LiveDataTestUtil {

    private LiveDataTestUtil() {
    }

    public static <T> T getOrAwaitValue(LiveData<T> liveData) throws InterruptedException, TimeoutException {
        final Object[] data = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                data[0] = value;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        if (!latch.await(3, TimeUnit.SECONDS)) {
            liveData.removeObserver(observer);
            throw new TimeoutException("LiveData value was never set.");
        }
        @SuppressWarnings("unchecked")
        T result = (T) data[0];
        return result;
    }
}