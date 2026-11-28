@FM-29 @US-11
Feature: Reserve an e-bike
  As a conventional user (Inês)
  I want to reserve an e-bike for 15 minutes
  So that it is waiting for me when I arrive

  Background:
    Given a conventional station "Central" with capacity 10
    And an available "E_BIKE" "EB-01" at "Central" with battery 80%
    And Inês is logged in
    And the time is "2026-11-30T07:45:00Z"

  Rule: A reservation holds the e-bike exclusively for one user (BR5)

    Scenario: Reserving an e-bike holds it for 15 minutes for a fixed fee
      When Inês reserves "EB-01"
      Then her reservation is "ACTIVE" until "2026-11-30T08:00:00Z" with a fee of 0.50 €
      And "EB-01" is "RESERVED"

    Scenario: An e-bike reserved by another user cannot be reserved or unlocked
      Given another user reserved "EB-01"
      When Inês reserves "EB-01"
      Then the reservation is refused
      When Inês unlocks "EB-01"
      Then the unlock is refused

    Scenario: A user can only have one active reservation
      Given an available "E_BIKE" "EB-02" at "Central" with battery 80%
      And Inês reserved "EB-01"
      When Inês reserves "EB-02"
      Then the reservation is refused
      And "EB-02" is still "AVAILABLE"

    Scenario: A user with a trip in progress cannot reserve
      Given an available "E_BIKE" "EB-02" at "Central" with battery 80%
      And Inês unlocks "EB-02"
      When Inês reserves "EB-01"
      Then the reservation is refused

  Rule: Only eligible e-bikes can be reserved (BR4)

    Scenario Outline: An e-bike that is not eligible cannot be reserved
      Given <vehicle>
      When Inês reserves "<code>"
      Then the reservation is refused

      Examples:
        | vehicle                                                              | code  |
        | an available "E_BIKE" "EB-04" at "Central" with battery 19%          | EB-04 |
        | a "MAINTENANCE" "E_BIKE" "EB-05" at "Central" with battery 80%       | EB-05 |
        | an available "WHEELCHAIR_BIKE" "WB-02" at "Central" with battery 90% | WB-02 |

  Rule: A reservation expires after 15 minutes (BR5)

    Scenario Outline: Unlocking at the end of the hold
      Given Inês reserved "EB-01"
      And the time is "<time>"
      When Inês unlocks "EB-01"
      Then her reservation is "<reservation>"

      Examples:
        | time                 | reservation |
        | 2026-11-30T07:59:59Z | FULFILLED   |
        | 2026-11-30T08:00:00Z | EXPIRED     |

    Scenario: An expired reservation makes the e-bike available again
      Given Inês reserved "EB-01"
      And the time is "2026-11-30T08:00:00Z"
      When the reservation expiry check runs
      Then her reservation is "EXPIRED"
      And "EB-01" is "AVAILABLE"

  Rule: A used reservation adds its fee to the trip (BR6, BR7)

    Scenario: Commute with a reservation (C1)
      Given Inês reserved "EB-01"
      And the time is "2026-11-30T07:52:00Z"
      When Inês unlocks "EB-01"
      And the telemetry reports the lock of "EB-01" open at "2026-11-30T07:52:00Z"
      And the telemetry reports the lock of "EB-01" closed at "Central" at "2026-11-30T08:06:20Z"
      Then Inês sees a trip cost on "EB-01" of 3.75 € for 15 minutes with 0 overtime minutes
