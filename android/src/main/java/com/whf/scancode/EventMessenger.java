package com.whf.scancode;


import android.os.Handler;
import android.os.Looper;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;


class EventMessenger {

    private static EventMessenger instance;

    private Handler mainHandler;
    private EventChannel.EventSink eventSink;

    public static EventMessenger getInstance() {
        if (instance == null) {
            instance = new EventMessenger();
        }
        return instance;
    }

    private EventMessenger() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setEventSink(EventChannel.EventSink eventSink) {
        this.eventSink = eventSink;
    }

    public void cleanEventSink() {
        this.eventSink = null;
    }

    public void sendEvent(final String event) {
        if (eventSink == null){
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                eventSink.success(event);
            }
        });
    }
}
