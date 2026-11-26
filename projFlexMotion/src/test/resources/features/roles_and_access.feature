@FM-20 @US-02
Feature: Roles and access
  As the platform admin
  I want to assign roles and have each user see only the features of their role
  So that nobody accesses what is not theirs to use

  Background:
    Given an admin account "admin@valeclaro.test"
    And an end user account "ines@valeclaro.test"

  Scenario: A new account starts as an end user
    Given a user account "rui@valeclaro.test" without an assigned role
    Then the role of "rui@valeclaro.test" is END_USER

  Scenario Outline: The admin assigns a staff role
    When the admin assigns the role <role> to "ines@valeclaro.test"
    Then the response status is 200
    And the role of "ines@valeclaro.test" is <role>

    Examples:
      | role           |
      | OPERATOR       |
      | CLIENT_MANAGER |

  Scenario: The admin removes a staff role
    Given "ines@valeclaro.test" has the role OPERATOR
    When the admin assigns the role END_USER to "ines@valeclaro.test"
    Then the response status is 200
    And the role of "ines@valeclaro.test" is END_USER

  Scenario: The admin role cannot be assigned
    When the admin assigns the role ADMIN to "ines@valeclaro.test"
    Then the response status is 400
    And the role of "ines@valeclaro.test" is END_USER

  Scenario: Only the admin changes roles
    Given an operator account "sofia@valeclaro.test"
    When "sofia@valeclaro.test" assigns the role OPERATOR to "ines@valeclaro.test"
    Then the response status is 403
    And the role of "ines@valeclaro.test" is END_USER

  Scenario Outline: A request to another role's feature is forbidden
    Given a user with the role <role>
    When that user calls <method> <path>
    Then the response status is 403

    Examples:
      | role           | method | path                    |
      | END_USER       | POST   | /api/vehicles           |
      | CLIENT_MANAGER | POST   | /api/stations           |
      | OPERATOR       | POST   | /api/trips              |
      | OPERATOR       | GET    | /api/stations           |
      | ADMIN          | POST   | /api/vehicles           |
      | END_USER       | PUT    | /api/admin/users/1/role |