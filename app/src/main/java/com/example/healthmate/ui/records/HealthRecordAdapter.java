package com.example.healthmate.ui.records;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmate.R;
import com.example.healthmate.data.model.HealthRecord;
import com.example.healthmate.utils.DateUtils;
import com.example.healthmate.utils.HealthStats;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

/*
 * Adapter for the list on the Records screen.
 * ListAdapter + DiffUtil only redraws the cards that really changed.
 */
public class HealthRecordAdapter extends ListAdapter<HealthRecord, HealthRecordAdapter.RecordViewHolder> {

    // the fragment gets the clicks through this
    public interface Listener {
        void onRecordClicked(@NonNull HealthRecord record);

        void onEditClicked(@NonNull HealthRecord record);

        void onDeleteClicked(@NonNull HealthRecord record);
    }

    private static final DiffUtil.ItemCallback<HealthRecord> DIFF = new DiffUtil.ItemCallback<HealthRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull HealthRecord oldItem, @NonNull HealthRecord newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull HealthRecord oldItem, @NonNull HealthRecord newItem) {
            return oldItem.hasSameContent(newItem);
        }
    };

    private final Listener listener;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    public HealthRecordAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView month;
        private final TextView day;
        private final TextView weekday;
        private final TextView fullDate;
        private final TextView steps;
        private final TextView calories;
        private final TextView water;
        private final TextView sleep;
        private final MaterialButton editButton;
        private final MaterialButton deleteButton;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            month = itemView.findViewById(R.id.text_month);
            day = itemView.findViewById(R.id.text_day);
            weekday = itemView.findViewById(R.id.text_weekday);
            fullDate = itemView.findViewById(R.id.text_full_date);
            steps = itemView.findViewById(R.id.text_steps);
            calories = itemView.findViewById(R.id.text_calories);
            water = itemView.findViewById(R.id.text_water);
            sleep = itemView.findViewById(R.id.text_sleep);
            editButton = itemView.findViewById(R.id.button_edit);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }

        void bind(HealthRecord record) {
            Context context = itemView.getContext();
            String date = record.getDate();
            String longDate = DateUtils.formatLong(date);

            month.setText(DateUtils.monthShortUpper(date));
            day.setText(DateUtils.dayOfMonth(date));
            weekday.setText(DateUtils.dayOfWeekShort(date));
            fullDate.setText(longDate);

            steps.setText(context.getString(R.string.value_steps, numberFormat.format(record.getSteps())));
            calories.setText(context.getString(R.string.value_kcal, numberFormat.format(record.getCalories())));
            water.setText(context.getString(R.string.value_ml, numberFormat.format(record.getWater())));
            sleep.setText(HealthStats.formatSleep(record.getSleepMinutes()));

            editButton.setContentDescription(context.getString(R.string.cd_edit_record, longDate));
            deleteButton.setContentDescription(context.getString(R.string.cd_delete_record, longDate));

            itemView.setOnClickListener(v -> listener.onRecordClicked(record));
            editButton.setOnClickListener(v -> listener.onEditClicked(record));
            deleteButton.setOnClickListener(v -> listener.onDeleteClicked(record));
        }
    }
}
