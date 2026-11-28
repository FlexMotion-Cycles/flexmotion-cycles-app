@FM-26
Feature: Vehicle state from telemetry
  As the operations manager (Sofia)
  I want telemetry to update each vehicle's battery, position, station and lock state,
  and fault events to open tickets
  So that I do not depend on complaints

  Background:
    Given a conventional station "Central" with capacity 10
    And an available "E_BIKE" "EB-01" at "Central" with battery 80%

  Rule: Telemetry updates battery, position, station and lock state

    Scenario: A reading at a station updates the battery and keeps the vehicle docked
      When the telemetry reports "EB-01" locked at "Central" with battery 54%
      Then "EB-01" has battery 54%
      And "EB-01" is locked at "Central"

    Scenario: A vehicle locked away from every station has no station
      When the telemetry reports "EB-01" locked 50 metres away from "Central"
      Then "EB-01" has no station

  Rule: A vehicle below 20% battery cannot be unlocked (BR4)

    Scenario Outline: The battery reported by telemetry decides whether the unlock is accepted
      Given Inês is logged in
      And Inês has a linked card ending in "4242" that the payment sandbox accepts
      When the telemetry reports "EB-01" locked at "Central" with battery <battery>%
      And Inês unlocks "EB-01"
      Then <outcome>

      Examples:
        | battery | outcome                               |
        | 19      | the unlock is refused                 |
        | 20      | an unlock command is sent for "EB-01" |

  Rule: A fault event moves the vehicle to MAINTENANCE and opens one fault ticket

    Scenario: A fault event on an available vehicle opens a ticket and moves it to maintenance
      When the telemetry reports the fault "BRAKE_FAILURE" on "EB-01"
      Then "EB-01" is "MAINTENANCE"
      And "EB-01" has 1 open fault ticket

    Scenario: A second fault event does not open another ticket
      When the telemetry reports the fault "BRAKE_FAILURE" on "EB-01"
      And the telemetry reports the fault "MOTOR_FAILURE" on "EB-01"
      Then "EB-01" has 1 open fault ticket

    Scenario: A vehicle in use gets a fault ticket but keeps its state
      Given a "IN_USE" "E_BIKE" "EB-02" at "Central" with battery 80%
      When the telemetry reports the fault "BRAKE_FAILURE" on "EB-02"
      Then "EB-02" is "IN_USE"
      And "EB-02" has 1 open fault ticket
