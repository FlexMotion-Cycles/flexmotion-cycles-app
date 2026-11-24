@FM-28 @US-10
Feature: End a trip and its cost
  As a conventional user (Inês)
  I want my trip to end when I lock the e-bike at a dock and to see what it cost
  So that I stop paying and understand the price

  Background:
    Given a conventional station "Central" with capacity 10
    And an available "E_BIKE" "EB-01" at "Central" with battery 50%
    And Inês is logged in
    And Inês unlocks "EB-01"
    And the telemetry reports the lock of "EB-01" open at "2026-11-22T10:00:00Z"

  Rule: A trip ends only when the lock closes at an official dock (BR9)

    Scenario: Locking at a dock ends the trip and frees the e-bike at that station
        When the telemetry reports the lock of "EB-01" closed at "Central" at "2026-11-22T10:20:00Z"
        Then Inês's trip on "EB-01" is "ENDED"
        And "EB-01" is "AVAILABLE" at "Central"

    Scenario: Locking outside a dock keeps the trip running
        When the telemetry reports the lock of "EB-01" closed 50 m away from "Central" at "2026-11-22T10:20:00Z"
        Then Inês's trip on "EB-01" is "ACTIVE"
        And "EB-01" is "IN_USE"

    Scenario: The time after locking outside a dock is still charged
        When the telemetry reports the lock of "EB-01" closed 50 m away from "Central" at "2026-11-22T10:05:00Z"
        And the telemetry reports the lock of "EB-01" closed at "Central" at "2026-11-22T10:10:00Z"
        Then Inês sees a trip cost on "EB-01" of 2.50 € for 10 minutes with 0 overtime minutes

  Rule: Cost = 1.00 € unlock + 0.15 €/min, a started minute counts; beyond 90 min, 0.30 €/min (BR7, BR17)

    Scenario Outline: The trip cost follows the minute and overtime boundaries
        When the telemetry reports the lock of "EB-01" closed at "Central" at "<closedAt>"
        Then Inês sees a trip cost on "EB-01" of <total> € for <minutes> minutes with <overtime> overtime minutes

      Examples:
        | closedAt             | minutes | overtime | total |
        | 2026-11-22T10:01:00Z | 1       | 0        | 1.15  |
        | 2026-11-22T10:01:01Z | 2       | 0        | 1.30  |
        | 2026-11-22T11:30:00Z | 90      | 0        | 14.50 |
        | 2026-11-22T11:30:01Z | 91      | 1        | 14.80 |