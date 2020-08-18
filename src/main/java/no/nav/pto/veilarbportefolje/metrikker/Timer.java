package no.nav.pto.veilarbportefolje.metrikker;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;

import java.util.HashMap;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Timer {

    private Event event;
    private Timing timing = new Timing();
    private MetricsClient metricsClient;


    /*
        Bruker både measureTimestamp og startTime fordi System.nanoTime()
        skal brukes for tidsmåling og System.currentTimeMillis() for å
        rapportere når målingen ble gjort.
     */
    private long measureTimestamp;
    private long startTime;
    private long stopTime;

    Timer(MetricsClient metricsClient, String name) {
        this.event = new Event( name + ".timer");
        this.metricsClient = metricsClient;
    }

    public Timer start() {
        measureTimestamp = timing.currentTimeMillis();
        startTime = timing.nanoTime();
        return this;
    }

    public Timer stop() {
        stopTime = timing.nanoTime();
        event.addFieldToReport("value", getElpasedTimeInMillis());
        return this;
    }

    long getElpasedTimeInMillis() {
        long elapsedTimeNanos = stopTime - startTime;

        return NANOSECONDS.toMillis(elapsedTimeNanos);
    }

    protected Timer setFailed() {
        event.setFailed();
        return this;
    }

    public Timer report() {
        ensureTimerIsStopped();
        metricsClient.report(event);
        reset();
        return this;
    }

    private void ensureTimerIsStopped() {
        if (!event.getFields().containsKey("value")) {
            throw new IllegalStateException("Must stop timer before reporting!");
        }
    }

    /**
     * Timer er ikke threadsafe, bruk en ny timer heller enn å resette en eksisterende
     * om flere tråder kan aksessere målepunktet samtidig
     */
    private void reset() {
        event = null;
        measureTimestamp = 0;
        startTime = 0;
        stopTime = 0;
    }

    public static final class Timing {
        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        long nanoTime() {
            return System.nanoTime();
        }
    }

}