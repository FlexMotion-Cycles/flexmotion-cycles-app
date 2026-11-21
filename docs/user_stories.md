# FlexMotion Cycles: user stories

Personas, user stories and acceptance criteria of the FlexMotion Cycles MVP.
Business rules (BR1–BR24) are defined in `TQS26- Product Specs report.pdf`.

## Personas

| Persona | Actor | User stories |
|---|---|---|
| Inês Carvalho | End user (conventional service) | US-01, US-09 to US-15, US-20 |
| Rui Tavares | End user (accessible service, independent) | US-13, US-16 |
| Helena Sousa | End user (accessible service, companion) | US-06, US-16, US-17 |
| Bruno Lopes | Operator: accessible-hub technician | US-18, US-19, US-22 |
| Sofia Marques | Operator: fleet operations manager | US-03 to US-08, US-20 to US-22 |
| Paulo Andrade | Institutional client manager | US-17, US-23, US-24 |
| (no persona) | Platform admin | US-02 |

## Accounts and access

### US-01 Sign up and log in
As a new user (Inês), I want to create an account, confirm my email and log in, so that I can use the service.

- Name, email, date of birth and password are required.
- Users under 18 on the registration date are refused (BR1): turning 18 tomorrow = refused, turning 18 today = accepted.
- The email is unique; the password has at least 8 characters and is stored hashed.
- An account with an unverified email cannot reserve or start a trip (BR2). The confirmation link works only once; tests use a fake mail server.
- 3 failed login attempts lock the account for 15 minutes (BR3): at 14:59 still locked, at 15:00 unlocked. A successful login resets the count.

### US-02 Roles and access
As the platform admin, I want to assign roles and have each user see only the features of their role, so that nobody accesses what is not theirs to use.

- Roles: end user (default for new accounts), operator (including technician), client manager, admin.
- Only the admin assigns or removes the operator and client manager roles.
- A request to another role's feature returns 403.

## Fleet and hub management

### US-03 Register stations and vehicles
As the operations manager (Sofia), I want to register stations and vehicles, so that the fleet and the network are in the system.

- Station: location, dock capacity (at least 1), type (conventional or accessible) and, if accessible, technician hours and step-free access. Each conventional station has an overflow zone with a 10 m radius (BR11).
- Vehicle: identifier, type (e-bike, cargo e-bike, wheelchair-bike, handbike, adapted tricycle) and current station. A new vehicle starts as AVAILABLE and cannot be placed at a full station.

### US-04 Vehicle lifecycle
As the operations manager (Sofia), I want vehicle states to change only through valid transitions, so that the system reflects the real fleet.

- States: AVAILABLE, RESERVED, IN_USE, MAINTENANCE, REMOVED.
- Invalid transitions are rejected; REMOVED is final.
- A vehicle IN_USE cannot be reserved or removed.
- The operator can deactivate an AVAILABLE vehicle (it moves to MAINTENANCE) and activate it again (back to AVAILABLE).

### US-05 Operator dashboard
As the operations manager (Sofia), I want one dashboard with the fleet status and the open tickets and tasks, with alerts on SLA risk, so that I can decide rebalancing and priorities.

- Vehicles by state and by station, with occupancy against capacity; battery and position from the latest telemetry.
- Open maintenance tickets and battery-swap tasks sorted by deadline.
- Alert for tickets without a technician 8 hours before their deadline (BR24): deadline − 8 h 00 min = alert; − 8 h 01 min = no alert.

### US-06 Reallocate bookings
As a user with a booking (Helena), I want it moved to another vehicle when mine becomes unavailable, so that the outing does not fail.

- Applies BR23: same type at the same station, or cancelled and refunded.
- Until notifications exist, the change is shown on the user dashboard.
- Covers the operations manager's removal of a bike with future bookings.

## Fleet telemetry

### US-07 Fleet simulator
As the operations manager (Sofia), I want a fleet simulator that publishes battery, position, lock state and fault events for every vehicle and accepts unlock commands, so that the platform works without real hardware.

- The simulator runs in its own container and communicates only through the MQTT broker.
- Battery drains while a vehicle is in use; fault events can be triggered for tests.

### US-08 Vehicle state from telemetry
As the operations manager (Sofia), I want telemetry to update each vehicle's battery, position, station and lock state, and fault events to open tickets, so that I do not depend on complaints.

- The platform reads telemetry through one adapter, replaceable without changing business logic.
- A vehicle below 20 % battery cannot be reserved or unlocked (BR4): 19 % = refused, 20 % = accepted.
- A fault event moves the vehicle to MAINTENANCE and opens a ticket; at most one open fault ticket per vehicle.

## Finding and using e-bikes

### US-09 Find an e-bike and start a trip
As a conventional user (Inês), I want to see the stations with available e-bikes and unlock one from the web app, so that I can start my trip right away.

- Only AVAILABLE vehicles with battery ≥ 20 % and no open ticket are listed (BR4); accessible vehicles are never listed (BR19). Free docks are shown per station.
- The platform sends the unlock command to the simulator; the vehicle becomes IN_USE when telemetry reports the lock open.
- The trip time starts at lock release (BR7). Unlocking without a reservation is an immediate use.

### US-10 End a trip and its cost
As a conventional user (Inês), I want my trip to end when I lock the e-bike at a dock and to see what it cost, so that I stop paying and understand the price.

- The trip ends only when telemetry reports the lock closed at an official dock (BR9); lock open or outside a dock = the trip keeps being charged.
- Cost = 1.00 € unlock fee + 0.15 €/min; a started minute counts as a full minute: 60 s = 1 min, 61 s = 2 min (BR7).
- The reservation fee is added when the trip used a reservation (BR6); minutes beyond the maximum duration use the overtime rate (BR17).
- The vehicle returns to AVAILABLE at that station.

### US-11 Reserve an e-bike
As a conventional user (Inês), I want to reserve an e-bike for 15 minutes, so that it is waiting for me when I arrive.

- The e-bike becomes RESERVED exclusively for me (BR5); only one active reservation per user.
- If two users reserve the same e-bike at the same time, only one succeeds.
- Unlock at 14:59 = accepted; at 15:00 = reservation expired and e-bike AVAILABLE.
- The 0.50 € reservation fee is charged even when the reservation expires (BR6).

### US-12 Full station and improper parking
As a conventional user (Inês), I want to end my trip in the overflow zone when the station is full, so that I am not fined.

- Allowed only when the station is at 100 % capacity: 19 of 20 occupied = refused, 20 of 20 = allowed (BR11).
- Position within 10 m: 10.0 m = accepted, 10.1 m = refused. A photo is required.
- Locking outside a dock and outside a valid overflow zone = fine 20.00 € plus recovery 10.00 € (BR10).

### US-13 User dashboard
As an end user (Inês, Rui), I want to see my active trip with its running cost and my past trips and bookings with their breakdown, so that I can keep track of my spending.

- Past trips show unlock fee, minutes, reservation fee, overtime, deposit and fines.
- Pending payments are highlighted.

## Payments

### US-14 Payment method and deposit
As a conventional user (Inês), I want to link a card in the payment sandbox, with a deposit held while I ride, so that I can start trips and unpaid trips are covered.

- Without a linked payment method, reservations and trips are refused (BR2); a card declined by the sandbox is not linked.
- Applies BR16: deposit hold declined = trip refused; released when the trip is paid; captured when the payment fails.

### US-15 Charge at trip end
As a conventional user (Inês), I want the trip total charged automatically when it ends, and to settle any failed payment myself, so that I can keep riding.

- Payment accepted = trip paid (BR8).
- Payment declined = pending payment, and new trips are refused until it is settled.
- After a successful payment of the pending amount, new trips are allowed again.

## Reduced mobility and assisted bookings

### US-16 Find and book an accessible vehicle
As an accessible-vehicle user (Rui, Helena), I want to choose an accessible vehicle and station, book a day and time and pay the fixed price, so that the vehicle and a technician are waiting for me.

- Filter by vehicle type, with a description of who each type suits; stations show step-free access and technician hours.
- Applies BR20 (technician hours, maximum duration, fixed price) and BR21 (no overlapping bookings).
- Paid in the payment sandbox; a declined payment creates no booking. Pick-up and return stations must both be accessible.
- The pages pass automated accessibility checks and work with the keyboard only (Rui).

### US-17 Support needs and access to the accessible fleet
As a companion (Helena), I want to state who will use the vehicle and their support needs, so that the technician can prepare; and the client manager (Paulo) defines which support needs give access to each accessible vehicle type.

- Fields: passenger name, wheelchair use, free-text notes; at least one support need (BR19).
- Bookings without a support need that matches the vehicle type are refused.
- Only the technicians of the booking's stations can see this data. Tests and demos use synthetic data only.

### US-18 Hub operations
As a hub technician (Bruno), I want to see today's bookings at my station and record the start and the return of each use, so that vehicles are ready and their state is correct.

- Today's bookings and expected returns sorted by time, with vehicle, battery and support needs.
- Start allowed only for today's bookings with pick-up at my station; the vehicle becomes IN_USE.
- Return allowed only at the booking's return station; a late return is recorded with its actual time, and if it affects the next booking BR23 applies.
- Condition "faulty" moves the vehicle to MAINTENANCE and opens a ticket.

### US-19 No-show
As a hub technician (Bruno), I want to mark a booking as no-show, so that the vehicle is released.

- Applies BR22: 29:59 = refused, 30:00 = accepted; fixed price not refunded.

## Maintenance and SLA

### US-20 Maintenance tickets
As the operations manager (Sofia), I want tickets opened from user damage reports and by staff, with an SLA deadline, assigned to a workshop technician and closed after the repair, so that the contract is met.

- A user (Inês) can report damage within 5 minutes of unlocking or ending the trip; after 5 minutes, refused (BR12). The report opens a ticket and moves the vehicle to MAINTENANCE.
- Deadline = opening time + 48 h (BR24).
- Only the assigned technician can close the ticket; closing returns the vehicle to AVAILABLE.

### US-21 Damage liability
As the operations manager (Sofia), I want confirmed damage assigned to the last user who did not report it, so that the cost is recovered.

- Damage reported by the next user at the start of their trip = assigned to the previous user (BR13).
- That user is charged the repair and recovery cost and the account is banned.

### US-22 Battery-swap tasks
As the operations manager (Sofia), I want a battery-swap task created when a vehicle's battery drops below 20 %, so that technicians like Bruno swap it in time.

- 19 % = task created, 20 % = no task; at most one open task per vehicle (BR15).
- Below 20 % the vehicle moves to MAINTENANCE (at the end of the trip, if it is in use) and returns to AVAILABLE only after the swap is recorded.

## Municipal rules of use

### US-23 Rules of use
As the client manager (Paulo), I want to set the maximum trip duration and each station's dock capacity and allowed vehicle types, so that the service matches demand.

- Maximum duration by BR18: 14 = refused, 15 and 240 = accepted, 241 = refused.
- A capacity below the vehicles currently docked is refused.
- Changes apply only to trips and bookings started after the change.

## KPIs and metrics

### US-24 Contract indicators
As the client manager (Paulo), I want to see business and technical indicators for a chosen period, so that I can check contract compliance and that the service handles demand.

- Business: utilisation, availability against the 90 % SLA target during service hours, uses per day, mean time to repair against the 48 h SLA.
- Technical: API latency, error rate and telemetry lag.
