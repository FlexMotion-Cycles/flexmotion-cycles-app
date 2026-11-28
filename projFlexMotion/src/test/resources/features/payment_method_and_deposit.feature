@FM-32 @US-14
Feature: Payment method and deposit
  As a conventional user (Inês)
  I want to link a card in the payment sandbox, with a deposit held while I ride
  So that I can start trips and unpaid trips are covered

  Background:
    Given a conventional station "Central" with capacity 10
    And an available "E_BIKE" "EB-01" at "Central" with battery 50%
    And Inês is logged in

  Rule: Only a card accepted by the payment sandbox is linked (BR2)

    Scenario: A card accepted by the sandbox is linked and only its last four digits are shown
      When Inês links a card ending in "4242" that the payment sandbox accepts
      Then Inês has a linked card ending in "4242"

    Scenario: A card declined by the sandbox is not linked
      When Inês links a card ending in "0002" that the payment sandbox declines
      Then the card is refused
      And Inês has no linked card

  Rule: Without a linked payment method, trips are refused (BR2)

    Scenario: Unlocking without a linked card is refused
      When Inês unlocks "EB-01"
      Then the unlock is refused
      And no unlock command is sent for "EB-01"
      And no deposit is held for Inês

  Rule: A 10.00 € deposit is held on the linked card when a trip is requested (BR16)

    Scenario: An accepted deposit hold lets the trip start
      Given Inês has a linked card ending in "4242" that the payment sandbox accepts
      When Inês unlocks "EB-01"
      Then a deposit of 10.00 € is held for Inês's trip on "EB-01"
      And an unlock command is sent for "EB-01"

    Scenario: A declined deposit hold refuses the trip
      Given Inês has a linked card ending in "0341" whose deposit hold the payment sandbox declines
      When Inês unlocks "EB-01"
      Then the unlock is refused
      And no unlock command is sent for "EB-01"
      And "EB-01" is still "AVAILABLE"
      And no deposit is held for Inês
