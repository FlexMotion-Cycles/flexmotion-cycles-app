package pt.ua.tqs.flexmotion_cycles.acceptance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Clock the scenarios set ("the time is ..."); the system clock until then. */
public class MutableClock extends Clock {

    private volatile Clock current = Clock.systemUTC();

    public void setInstant(Instant instant) {
        current = Clock.fixed(instant, ZoneOffset.UTC);
    }

    public void reset() {
        current = Clock.systemUTC();
    }

    @Override
    public ZoneId getZone() {
        return current.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return current.withZone(zone);
    }

    @Override
    public Instant instant() {
        return current.instant();
    }
}
