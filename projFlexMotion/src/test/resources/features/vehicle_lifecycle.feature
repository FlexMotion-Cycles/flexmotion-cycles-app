@FM-22
Feature: Vehicle lifecycle
  As the operations manager,
  I want vehicle states to change only through valid transitions,
  so that the system reflects the real fleet.

  Scenario Outline: Valid transitions are accepted
    Given a vehicle in state "<from>"
    When the vehicle changes to state "<to>"
    Then the vehicle is in state "<to>"

    Examples:
      | from        | to          |
      | AVAILABLE   | RESERVED    |
      | AVAILABLE   | IN_USE      |
      | AVAILABLE   | MAINTENANCE |
      | AVAILABLE   | REMOVED     |
      | RESERVED    | IN_USE      |
      | RESERVED    | AVAILABLE   |
      | IN_USE      | AVAILABLE   |
      | MAINTENANCE | AVAILABLE   |
      | MAINTENANCE | REMOVED     |

  Scenario Outline: Invalid transitions are rejected
    Given a vehicle in state "<from>"
    When the vehicle changes to state "<to>"
    Then the change is rejected
    And the vehicle is in state "<from>"

    Examples:
      | from        | to          |
      | AVAILABLE   | AVAILABLE   |
      | RESERVED    | RESERVED    |
      | RESERVED    | MAINTENANCE |
      | RESERVED    | REMOVED     |
      | IN_USE      | IN_USE      |
      | IN_USE      | MAINTENANCE |
      | MAINTENANCE | MAINTENANCE |
      | MAINTENANCE | RESERVED    |
      | MAINTENANCE | IN_USE      |

  Scenario Outline: REMOVED is final
    Given a vehicle in state "REMOVED"
    When the vehicle changes to state "<to>"
    Then the change is rejected
    And the vehicle is in state "REMOVED"

    Examples:
      | to          |
      | AVAILABLE   |
      | RESERVED    |
      | IN_USE      |
      | MAINTENANCE |
      | REMOVED     |

  Scenario: A vehicle in use cannot be reserved
    Given a vehicle in state "IN_USE"
    When the vehicle changes to state "RESERVED"
    Then the change is rejected
    And the vehicle is in state "IN_USE"

  Scenario: A vehicle in use cannot be removed
    Given a vehicle in state "IN_USE"
    When the vehicle changes to state "REMOVED"
    Then the change is rejected
    And the vehicle is in state "IN_USE"

  Scenario: The operator deactivates an available vehicle
    Given a vehicle in state "AVAILABLE"
    When the operator deactivates the vehicle
    Then the vehicle is in state "MAINTENANCE"

  Scenario: The operator activates a vehicle in maintenance
    Given a vehicle in state "MAINTENANCE"
    When the operator activates the vehicle
    Then the vehicle is in state "AVAILABLE"

  Scenario: The operator cannot deactivate a vehicle in use
    Given a vehicle in state "IN_USE"
    When the operator deactivates the vehicle
    Then the change is rejected
    And the vehicle is in state "IN_USE"