package com.nilhcem.droidconde.ui.schedule.day;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.nilhcem.droidconde.data.app.model.ScheduleDay;
import com.nilhcem.droidconde.ui.BaseFragmentPresenter;

public class ScheduleDayPresenter extends BaseFragmentPresenter<ScheduleDayMvp.View> {

    private final ScheduleDay scheduleDay;

    public ScheduleDayPresenter(ScheduleDayMvp.View view, ScheduleDay scheduleDay) {
        super(view);
        this.scheduleDay = scheduleDay;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view.initSlotsList(scheduleDay.getSlots());
    }

    @Override
    public void onResume() {
        super.onResume();
        view.refreshSlotsList();
    }
}
