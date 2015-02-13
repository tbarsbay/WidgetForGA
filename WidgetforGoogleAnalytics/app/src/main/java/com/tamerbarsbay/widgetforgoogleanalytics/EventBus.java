package com.tamerbarsbay.widgetforgoogleanalytics;

import com.squareup.otto.Bus;

public class EventBus {

    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

}