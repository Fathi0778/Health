package com.example.healthmate.ui.addrecord;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.data.repository.SaveResult;
import com.example.healthmate.ui.common.RecordFormController;
import com.example.healthmate.ui.editrecord.EditRecordFragment;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/*
 * Add screen (CREATE).
 * Checks the form, stops two records on the same date, then saves through the ViewModel.
 */
public class AddRecordFragment extends Fragment {

    private static final String STATE_SELECTED_DATE = "selected_date";

    private HealthRecordViewModel viewModel;
    private RecordFormController form;
    private View duplicateWarningCard;
    private MaterialButton saveButton;

    // the record that already exists for the picked date (null if there is none)
    @Nullable
    private HealthRecord existingRecordForDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        form = new RecordFormController(this, view.findViewById(R.id.record_form));
        form.setOnDateChangedListener(viewModel::checkDate);

        duplicateWarningCard = view.findViewById(R.id.card_duplicate_warning);
        saveButton = view.findViewById(R.id.button_save);

        String restoredDate = savedInstanceState != null
                ? savedInstanceState.getString(STATE_SELECTED_DATE) : null;
        form.setDate(restoredDate != null ? restoredDate : DateUtils.today());

        // check straight away if the picked date already has a record
        viewModel.getRecordForCheckedDate().observe(getViewLifecycleOwner(), record -> {
            existingRecordForDate = record;
            duplicateWarningCard.setVisibility(record != null ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.button_edit_existing).setOnClickListener(v -> {
            if (existingRecordForDate != null) {
                openEditor(existingRecordForDate.getId());
            }
        });
        saveButton.setOnClickListener(v -> saveRecord());
        view.findViewById(R.id.button_clear).setOnClickListener(v -> {
            form.clear(DateUtils.today());
            ((MainActivity) requireActivity()).showMessage(R.string.msg_form_cleared);
        });
    }

    private void saveRecord() {
        HealthRecord record = form.validateAndBuild();
        if (record == null) {
            return; // the errors are already shown under each box
        }
        if (existingRecordForDate != null) {
            showDuplicateDialog(existingRecordForDate.getId(), record.getDate());
            return;
        }

        saveButton.setEnabled(false);
        viewModel.insert(record).observe(getViewLifecycleOwner(), result -> {
            saveButton.setEnabled(true);
            handleResult(result, record.getDate());
        });
    }

    private void handleResult(SaveResult result, String date) {
        MainActivity activity = (MainActivity) requireActivity();
        switch (result.status) {
            case SUCCESS:
                activity.showMessage(getString(R.string.msg_record_saved, DateUtils.formatLong(date)));
                form.clear(DateUtils.today());
                activity.openTab(R.id.dashboardFragment); // go back to Home to see the new numbers
                break;
            case DUPLICATE_DATE:
                showDuplicateDialog(result.recordId, date);
                break;
            default:
                activity.showMessage(R.string.error_generic);
                break;
        }
    }

    private void showDuplicateDialog(long existingId, String date) {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.duplicate_title)
                .setMessage(getString(R.string.duplicate_message, DateUtils.formatLong(date)))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_edit_existing, (dialog, which) -> {
                    if (existingId > 0) {
                        openEditor(existingId);
                    }
                })
                .show();
    }

    private void openEditor(long recordId) {
        NavHostFragment.findNavController(this).navigate(
                R.id.editRecordFragment, EditRecordFragment.createArgs(recordId));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (form != null && form.getSelectedDate() != null) {
            outState.putString(STATE_SELECTED_DATE, form.getSelectedDate());
        }
    }
}