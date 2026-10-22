package com.example.healthmate.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * One background thread for all database work.
 * Room does not allow database work on the main (UI) thread because the screen
 * would freeze. Using only one thread also means saves happen one after another.
 */
public final class AppExecutors {

    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();

    private AppExecutors() {
    }

    public static ExecutorService database() {
        return DATABASE_EXECUTOR;
    }
}