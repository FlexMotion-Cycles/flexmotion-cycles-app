package pt.ua.tqs.flexmotion_cycles.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** Business constants of the conventional service, shared by search, reservation and unlock. */
public final class ConventionalUseRules {

    /** BR4: minimum battery level (%) to book or unlock a vehicle. */
    public static final int MIN_BATTERY_LEVEL = 20;

    /** BR19: accessible vehicles never appear in the conventional search. */
    public static final List<VehicleType> CONVENTIONAL_TYPES = Arrays.stream(VehicleType.values())
            .filter(type -> !type.isAccessible())
            .toList();

    /** A user with a trip requested or active cannot unlock or reserve another vehicle. */
    public static final Set<TripStatus> TRIP_IN_PROGRESS =
            Collections.unmodifiableSet(EnumSet.of(TripStatus.UNLOCK_REQUESTED, TripStatus.ACTIVE));

    private ConventionalUseRules() {
        // Constants only.
    }
}
