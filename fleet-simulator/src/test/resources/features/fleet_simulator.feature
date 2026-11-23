@FM-25 @US-07
Feature: Fleet simulator
  As the operations manager
  I want a fleet simulator that publishes telemetry and accepts unlock commands
  So that the platform works without real hardware

  Background:
    Given the simulated vehicle "EB-0001" is locked with 80 % battery

  Scenario: Every vehicle publishes its telemetry
    When the next telemetry round is published
    Then a reading for "EB-0001" has its battery, position, lock state and no fault

  Scenario: An unlock command opens the lock
    When the platform sends an unlock command for "EB-0001"
    Then a reading for "EB-0001" shows the lock open

  Scenario: The battery drains while the vehicle is in use
    Given the platform unlocked "EB-0001"
    When the next telemetry round is published
    Then the battery of "EB-0001" is below 80 %

  Scenario: A locked vehicle keeps its battery
    When the next telemetry round is published
    Then the battery of "EB-0001" is 80 %

  Scenario: A fault event can be triggered for tests
    When a test triggers the fault "BRAKE_FAILURE" on "EB-0001"
    Then a reading for "EB-0001" reports the fault "BRAKE_FAILURE"