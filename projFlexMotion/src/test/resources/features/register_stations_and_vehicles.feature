@FM-21 @US-03
Feature: Register stations and vehicles
  As the operations manager
  I want to register stations and vehicles
  So that the fleet and the network are in the system

  Background:
    Given I am logged in as an operator

  Scenario: A conventional station gets a 10 m overflow zone
    When I register a conventional station "Central Square" with 20 docks
    Then the station is registered
    And its overflow zone has a radius of 10 metres

  Scenario Outline: Dock capacity must be at least 1
    When I register a conventional station "Central Square" with <docks> docks
    Then the registration is <result>

    Examples:
      | docks | result   |
      | 0     | refused  |
      | 1     | accepted |

  Scenario: Register an accessible station
    When I register an accessible station "Marginal Accessible Hub" with 6 docks, step-free access and technician hours from "09:00" to "18:00"
    Then the station is registered

  Scenario: An accessible station without technician hours is refused
    When I register an accessible station "Marginal Accessible Hub" with 6 docks, step-free access and no technician hours
    Then the registration is refused

  Scenario: A new vehicle starts as available
    Given a conventional station "Central Square" with 20 docks and 19 vehicles docked
    When I register the vehicle "EB-0020" of type "E_BIKE" at "Central Square"
    Then the vehicle is registered as "AVAILABLE"

  Scenario: A vehicle cannot be placed at a full station
    Given a conventional station "Central Square" with 20 docks and 20 vehicles docked
    When I register the vehicle "EB-0021" of type "E_BIKE" at "Central Square"
    Then the registration is refused because the station is full