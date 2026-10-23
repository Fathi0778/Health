package com.example.healthmate.data.repository;

/*
 * The answer after saving, editing or deleting a record.
 * It tells the screen what happened so it can show the right message.
 */
public final class SaveResult {

    public enum Status {
        SUCCESS,
        // another record already exists for that date.
        DUPLICATE_DATE,
        // the record to update/delete is not in the database any more.
        NOT_FOUND,
        ERROR
    }

    public final Status status;
    // new/updated record id on SUCCESS, or the conflicting record id on DUPLICATE_DATE.
    public final long recordId;
    public final String errorMessage;

    private SaveResult(Status status, long recordId, String errorMessage) {
        this.status = status;
        this.recordId = recordId;
        this.errorMessage = errorMessage;
    }

    public static SaveResult success(long recordId) {
        return new SaveResult(Status.SUCCESS, recordId, null);
    }

    public static SaveResult duplicate(long existingRecordId) {
        return new SaveResult(Status.DUPLICATE_DATE, existingRecordId, null);
    }

    public static SaveResult notFound() {
        return new SaveResult(Status.NOT_FOUND, -1, null);
    }

    public static SaveResult error(String message) {
        return new SaveResult(Status.ERROR, -1, message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}