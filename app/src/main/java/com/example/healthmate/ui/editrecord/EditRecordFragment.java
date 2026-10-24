package com.example.healthmate.ui.editrecord;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.repository.SaveResult;
import com.example.healthmate.ui.common.RecordFormController;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/*
 * Edit screen (UPDATE, and DELETE from the bin icon at the top).
 * It loads the saved values, checks them the same way as the Add screen,
 * and keeps the same id so the same row in the database is updated.
 */
public class EditRecordFragment extends Fragment {

    public static final String ARG_RECORD_ID = "recordId";
    private static final String STATE_LOADED = "loaded";
    private static final String STATE_SELECTED_DATE = "selected_date";

    private HealthRecordViewModel viewModel;
    private RecordFormController form;
    private MaterialButton saveButton;
    private TextView editingDateText;

    private long recordId;
    // true after the form is filled from the database, so my changes are not overwritten
    private boolean loaded;
    @Nullable
    private HealthRecord originalRecord;

    public static Bundle createArgs(long recordId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RECORD_ID, recordId);
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);
        recordId = getArguments() != null ? getArguments().getLong(ARG_RECORD_ID, -1) : -1;

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> close());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_record) {
                confirmDelete();
                return true;
            }
            return false;
        });

        View content = view.findViewById(R.id.scroll_content);
        CircularProgressIndicator loading = view.findViewById(R.id.progress_loading);
        editingDateText = view.findViewById(R.id.text_editing_date);
        saveButton = view.findViewById(R.id.button_save_changes);
        form = new RecordFormController(this, view.findViewById(R.id.record_form));

        if (savedInstanceState != null) {
            loaded = savedInstanceState.getBoolean(STATE_LOADED, false);
            String date = savedInstanceState.getString(STATE_SELECTED_DATE);
            if (date != null) {
                form.setDate(date);
            }
        }

        // load the record from the database
        viewModel.getRecordById(recordId).observe(getViewLifecycleOwner(), record -> {
            if (record == null) {
                // it was deleted (or never existed), so there is nothing to edit
                if (originalRecord == null) {
                    ((MainActivity) requireActivity()).showMessage(R.string.msg_record_missing);
                    close();
                }
                return;
            }
            originalRecord = record;
            editingDateText.setText(getString(R.string.edit_subtitle,
                    DateUtils.formatLong(record.getDate())));
            if (!loaded) {
                form.fill(record);
                loaded = true;
            }
            loading.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        });

        saveButton.setOnClickListener(v -> saveChanges());
        view.findViewById(R.id.button_cancel).setOnClickListener(v -> close());
    }

    private void saveChanges() {
        HealthRecord updated = form.validateAndBuild();
        if (updated == null) {
            return;
        }
        updated.setId(recordId); // keep the same id

        saveButton.setEnabled(false);
        viewModel.update(updated).observe(getViewLifecycleOwner(), result -> {
            saveButton.setEnabled(true);
            MainActivity activity = (MainActivity) requireActivity();
            if (result.status == SaveResult.Status.SUCCESS) {
                activity.showMessage(R.string.msg_record_updated);
                close();
            } else if (result.status == SaveResult.Status.DUPLICATE_DATE) {
                form.showDateError(getString(R.string.error_date_conflict));
            } else if (result.status == SaveResult.Status.NOT_FOUND) {
                activity.showMessage(R.string.msg_record_missing);
                close();
            } else {
                activity.showMessage(R.string.error_generic);
            }
        });
    }

    private void confirmDelete() {
        if (originalRecord == null) {
            return;
        }
        HealthRecord toDelete = originalRecord;
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, DateUtils.formatLong(toDelete.getDate())))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    // close the screen first, so the "record missing" message doesn't show
                    MainActivity activity = (MainActivity) requireActivity();
                    viewModel.delete(toDelete);
                    activity.showMessage(R.string.msg_record_deleted);
                    close();
                })
                .show();
    }

    private void close() {
        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.editRecordFragment) {
            navController.popBackStack();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_LOADED, loaded);
        if (form != null && form.getSelectedDate() != null) {
            outState.putString(STATE_SELECTED_DATE, form.getSelectedDate());
        }
    }
}