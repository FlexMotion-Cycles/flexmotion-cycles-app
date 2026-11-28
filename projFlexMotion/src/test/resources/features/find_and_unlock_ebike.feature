@FM-27
Feature: Find an e-bike and start a trip
  As a conventional user (Inês)
  I want to see the stations with available e-bikes and unlock one from the web app
  So that I can start my trip right away

  Background:
    Given a conventional station "Central" with capacity 10

  Rule: Only eligible e-bikes are listed (BR4, BR19)

    Scenario Outline: The battery level decides whether an e-bike is listed
      Given an available "E_BIKE" "EB-01" at "Central" with battery <battery>%
      When Inês searches for available e-bikes
      Then "EB-01" is <result> at "Central"

      Examples:
        | battery | result     |
        | 19      | not listed |
        | 20      | listed     |

    Scenario Outline: Vehicles that are not available are not listed
      Given a "<status>" "E_BIKE" "EB-02" at "Central" with battery 80%
      When Inês searches for available e-bikes
      Then "EB-02" is not listed at "Central"

      Examples:
        | status      |
        | RESERVED    |
        | MAINTENANCE |

    Scenario: An e-bike with an open maintenance ticket is not listed
      Given an available "E_BIKE" "EB-03" at "Central" with battery 80%
      And "EB-03" has an open maintenance ticket
      When Inês searches for available e-bikes
      Then "EB-03" is not listed at "Central"

    Scenario: Accessible vehicles never appear in the conventional search
      Given an available "WHEELCHAIR_BIKE" "WB-01" at "Central" with battery 90%
      When Inês searches for available e-bikes
      Then "WB-01" is not listed at "Central"

    Scenario: Free docks are shown per station
      Given 3 vehicles are docked at "Central"
      When Inês searches for available e-bikes
      Then "Central" shows 7 free docks

  Rule: Unlocking without a reservation starts an immediate trip (BR7)

    Scenario: The trip starts when the telemetry reports the lock open
      Given an available "E_BIKE" "EB-01" at "Central" with battery 50%
      And Inês is logged in
      And Inês has a linked card ending in "4242" that the payment sandbox accepts
      When Inês unlocks "EB-01"
      Then an unlock command is sent for "EB-01"
      And "EB-01" is still "AVAILABLE"
      When the telemetry reports the lock of "EB-01" open at "2026-11-22T10:15:30Z"
      Then "EB-01" is "IN_USE"
      And Inês has an active trip on "EB-01" started at "2026-11-22T10:15:30Z"

    Scenario Outline: A vehicle that is not eligible cannot be unlocked
      Given <vehicle>
      And Inês is logged in
      And Inês has a linked card ending in "4242" that the payment sandbox accepts
      When Inês unlocks "<code>"
      Then the unlock is refused
      And no unlock command is sent for "<code>"

      Examples:
        | vehicle                                                              | code  |
        | an available "E_BIKE" "EB-04" at "Central" with battery 19%          | EB-04 |
        | a "MAINTENANCE" "E_BIKE" "EB-05" at "Central" with battery 80%       | EB-05 |
        | an available "WHEELCHAIR_BIKE" "WB-02" at "Central" with battery 90% | WB-02 |
