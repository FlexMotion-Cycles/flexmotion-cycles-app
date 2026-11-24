package pt.ua.tqs.flexmotion_cycles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Prototype: only what FM-27 needs (an open ticket blocks the vehicle, BR4). */
@Entity
@Table(name = "maintenance_tickets")
public class MaintenanceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = false)
    private Instant openedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketOrigin origin;

    private String faultCode;

    protected MaintenanceTicket() {
        // Required by JPA.
    }

    /** A new ticket starts OPEN. */
    public MaintenanceTicket(Vehicle vehicle, TicketOrigin origin, Instant openedAt) {
        this.vehicle = vehicle;
        this.origin = origin;
        this.openedAt = openedAt;
        this.status = TicketStatus.OPEN;
    }

    public static MaintenanceTicket forFaultEvent(Vehicle vehicle, String faultCode,
            Instant openedAt) {
        MaintenanceTicket ticket = new MaintenanceTicket(vehicle, TicketOrigin.FAULT_EVENT,
                openedAt);
        ticket.faultCode = faultCode;
        return ticket;
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public TicketOrigin getOrigin() {
        return origin;
    }

    public String getFaultCode() {
        return faultCode;
    }
}
