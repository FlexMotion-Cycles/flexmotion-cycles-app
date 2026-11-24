package pt.ua.tqs.flexmotion_cycles.domain;

/** Trip states: unlock requested → active (lock opened, BR7) → ended (locked at a dock, BR9). */
public enum TripStatus {
    UNLOCK_REQUESTED,
    ACTIVE,
    ENDED
}
