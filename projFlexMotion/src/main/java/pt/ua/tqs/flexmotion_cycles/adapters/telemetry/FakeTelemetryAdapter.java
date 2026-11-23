package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;
import pt.ua.tqs.flexmotion_cycles.ports.LockOpenedEvent;
import pt.ua.tqs.flexmotion_cycles.ports.TelemetryPort;

/**
 * In-memory stand-in for the fleet simulator, for tests and demos without the simulator.
 * With auto-confirm on, every unlock command is answered at once with a lock-opened
 * event; with it off, tests decide when the lock opens.
 */
@Component
@ConditionalOnProperty(name = "flexmotion.telemetry.adapter", havingValue = "fake")
public class FakeTelemetryAdapter implements TelemetryPort {

    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final boolean autoConfirm;
    private final List<String> unlockCommands = new CopyOnWriteArrayList<>();

    public FakeTelemetryAdapter(ApplicationEventPublisher eventPublisher, Clock clock,
            @Value("${flexmotion.telemetry.fake.auto-confirm}") boolean autoConfirm) {
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.autoConfirm = autoConfirm;
    }

    @Override
    public void sendUnlockCommand(String vehicleCode) {
        unlockCommands.add(vehicleCode);
        if (autoConfirm) {
            reportLockOpened(vehicleCode, clock.instant());
        }
    }

    /** Simulates the telemetry message "lock of this vehicle opened at this time". */
    public void reportLockOpened(String vehicleCode, Instant openedAt) {
        eventPublisher.publishEvent(new LockOpenedEvent(vehicleCode, openedAt));
    }

    public void reportReading(TelemetryReading reading) {
        eventPublisher.publishEvent(reading);
    }

    public List<String> unlockCommands() {
        return List.copyOf(unlockCommands);
    }

    /** Forgets the recorded commands, so each test starts clean. */
    public void reset() {
        unlockCommands.clear();
    }
}
