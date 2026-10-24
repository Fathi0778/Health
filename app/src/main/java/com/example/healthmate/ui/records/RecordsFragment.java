package com.example.healthmate.ui.records;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.ui.common.DatePickerHelper;
import com.example.healthmate.ui.editrecord.EditRecordFragment;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.viewmodel.HealthRecordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/*
 * Records screen (READ and DELETE, and it opens EDIT).
 * Shows all records with the newest first. I can search by date,
 * pick a date from the calendar, or use the quick filter chips.
 */
public class RecordsFragment extends Fragment implements HealthRecordAdapter.Listener {

    private static final String FILTER_PICKER_TAG = "records_filter_picker";

    private HealthRecordViewModel viewModel;
    private HealthRecordAdapter adapter;

    private TextInputEditText searchInput;
    private Chip clearFilterChip;
    private TextView countText;
    private CircularProgressIndicator loading;
    private View emptyLayout;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyAction;
    private ChipGroup quickFilters;
    // true while I change the chips from code, so the listener ignores it
    private boolean updatingChips;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HealthRecordViewModel.class);

        countText = view.findViewById(R.id.text_records_count);
        loading = view.findViewById(R.id.progress_records);
        emptyLayout = view.findViewById(R.id.layout_empty);
        emptyTitle = view.findViewById(R.id.text_empty_title);
        emptyMessage = view.findViewById(R.id.text_empty_message);
        emptyAction = view.findViewById(R.id.button_empty_action);
        clearFilterChip = view.findViewById(R.id.chip_clear_filter);

        // list
        adapter = new HealthRecordAdapter(this);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_records);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // search box
        searchInput = view.findViewById(R.id.input_search);
        String currentQuery = viewModel.getSearchQuery().getValue();
        if (currentQuery != null && !currentQuery.isEmpty()
                && (searchInput.getText() == null || searchInput.getText().length() == 0)) {
            searchInput.setText(currentQuery);
        }
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        // calendar icon in the search box -> filter by one exact date
        TextInputLayout searchLayout = view.findViewById(R.id.input_layout_search);
        searchLayout.setEndIconOnClickListener(v -> {
            String query = viewModel.getSearchQuery().getValue();
            DatePickerHelper.show(getChildFragmentManager(), FILTER_PICKER_TAG,
                    DateUtils.isValidIso(query) ? query : null,
                    R.string.records_pick_date, this::applyDateFilter);
        });
        DatePickerHelper.reattach(getChildFragmentManager(), FILTER_PICKER_TAG, this::applyDateFilter);

        // quick filter chips: All / Today / This month
        quickFilters = view.findViewById(R.id.chip_group_quick_filters);
        quickFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingChips || checkedIds.isEmpty()) {
                return;
            }
            int chipId = checkedIds.get(0);
            String today = DateUtils.today();
            if (chipId == R.id.chip_filter_all) {
                clearFilter();
            } else if (chipId == R.id.chip_filter_today) {
                applyDateFilter(today);
            } else if (chipId == R.id.chip_filter_month) {
                applyDateFilter(today.substring(0, 7)); // "2026-10"
            }
        });

        clearFilterChip.setOnCloseIconClickListener(v -> clearFilter());
        clearFilterChip.setOnClickListener(v -> clearFilter());

        // watch the data
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            boolean filtering = query != null && !query.isEmpty();
            syncQuickFilters(query);
            clearFilterChip.setVisibility(filtering ? View.VISIBLE : View.GONE);
            if (filtering) {
                clearFilterChip.setText(getString(R.string.records_filter_active, query));
                clearFilterChip.setContentDescription(getString(R.string.records_clear_filter));
            }
        });

        viewModel.getFilteredRecords().observe(getViewLifecycleOwner(), records -> {
            loading.setVisibility(View.GONE);
            adapter.submitList(records);
            updateCountAndEmptyState(records);
        });
    }

    private void applyDateFilter(@NonNull String isoDate) {
        searchInput.setText(isoDate); // the TextWatcher updates the ViewModel
        searchInput.setSelection(isoDate.length());
    }

    // select the chip that matches what is typed in the search box
    private void syncQuickFilters(@Nullable String query) {
        String today = DateUtils.today();
        updatingChips = true;
        if (query == null || query.isEmpty()) {
            quickFilters.check(R.id.chip_filter_all);
        } else if (query.equals(today)) {
            quickFilters.check(R.id.chip_filter_today);
        } else if (query.equals(today.substring(0, 7))) {
            quickFilters.check(R.id.chip_filter_month);
        } else {
            quickFilters.clearCheck();
        }
        updatingChips = false;
    }

    private void clearFilter() {
        searchInput.setText("");
        hideKeyboard(searchInput);
    }

    private void updateCountAndEmptyState(@Nullable List<HealthRecord> records) {
        int count = records == null ? 0 : records.size();
        String query = viewModel.getSearchQuery().getValue();
        boolean filtering = query != null && !query.isEmpty();

        countText.setText(getResources().getQuantityString(R.plurals.records_count, count, count)
                + " · " + getString(R.string.records_subtitle_all));

        if (count > 0) {
            emptyLayout.setVisibility(View.GONE);
            return;
        }
        emptyLayout.setVisibility(View.VISIBLE);
        if (filtering) {
            emptyTitle.setText(R.string.records_no_match_title);
            emptyMessage.setText(getString(R.string.records_no_match_message, query));
            emptyAction.setText(R.string.records_clear_filter);
            emptyAction.setIconResource(R.drawable.ic_close);
            emptyAction.setOnClickListener(v -> clearFilter());
        } else {
            emptyTitle.setText(R.string.records_empty_title);
            emptyMessage.setText(R.string.records_empty_message);
            emptyAction.setText(R.string.records_empty_add);
            emptyAction.setIconResource(R.drawable.ic_add);
            emptyAction.setOnClickListener(v ->
                    ((MainActivity) requireActivity()).openTab(R.id.addRecordFragment));
        }
    }

    // ---------------- Clicks from the list ----------------

    @Override
    public void onRecordClicked(@NonNull HealthRecord record) {
        openEditor(record);
    }

    @Override
    public void onEditClicked(@NonNull HealthRecord record) {
        openEditor(record);
    }

    @Override
    public void onDeleteClicked(@NonNull HealthRecord record) {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, DateUtils.formatLong(record.getDate())))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteRecord(record))
                .show();
    }

    private void deleteRecord(@NonNull HealthRecord record) {
        HealthRecord backup = record.copy();
        viewModel.delete(record).observe(getViewLifecycleOwner(), result -> {
            MainActivity activity = (MainActivity) requireActivity();
            if (result.isSuccess()) {
                Snackbar snackbar = activity.showMessage(R.string.msg_record_deleted);
                snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.hm_primary_light));
                snackbar.setAction(R.string.action_undo, v -> viewModel.insert(backup));
            } else {
                activity.showMessage(R.string.error_generic);
            }
        });
    }

    private void openEditor(@NonNull HealthRecord record) {
        NavHostFragment.findNavController(this).navigate(
                R.id.editRecordFragment, EditRecordFragment.createArgs(record.getId()));
    }

    private void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }
}
