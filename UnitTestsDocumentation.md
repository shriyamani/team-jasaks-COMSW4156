# Unit Tests Documentation

This document describes the service-layer unit-test suite for the Jasaks
booking service. Each service has one test class in
`src/test/java/com/jasaks/service`, and the tables below define the
equivalence partitions used for every input and map each partition to the
tests that target it.

## Scope and automatic execution

| Test class | Unit under test |
|---|---|
| `ReservationServiceTest` | `ReservationService` |
| `ReportServiceTest` | `ReportService` |
| `SpaceServiceTest` | `SpaceService` |

Run only the unit tests locally:

```bash
./mvnw -B test -Dtest='*ServiceTest'
```

Run the complete test suite:

```bash
./mvnw -B clean test
```

The GitHub Actions workflow in `.github/workflows/ci.yml` runs
`./mvnw -B clean test` on every push. That command executes all three
service unit-test classes together with the controller/context tests, so
the entire unit-test collection runs automatically in CI.

## Test organization

Each test class is a `final` class using `@ExtendWith(MockitoExtension.class)`.
Related tests are grouped with JUnit 5 `@Nested` classes, one group per
public service method or closely related set of methods:

| Test class | `@Nested` groups |
|---|---|
| `ReservationServiceTest` | `ListOperations`, `CancelOperations`, `CreateOperations`, `ModifyOperations` |
| `ReportServiceTest` | `Validation`, `SingleSpaceMetrics`, `AllSpaces` |
| `SpaceServiceTest` | `ReadOperations`, `CreateOperations`, `UpdateOperations`, `DeleteOperations`, `SearchOperations`, `PlaceLookupOperations` |

Repositories are declared once as Mockito test doubles and injected into
the service with `@InjectMocks`. `@BeforeEach` rebuilds all entity fixtures
for every test, so the tests share setup and test doubles without sharing
mutable state. No `@AfterEach` teardown is needed because Mockito resets
mocks between tests. No test depends on another, so there are no method
ordering constraints.

## ReservationService partitions

### `listUserReservations(userId, startDate, endDate, placeId, spaceId)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `userId` | Non-blank `"jsw2219"` | `null`, `""`, whitespace-only | N/A | `testListWithoutFilters`, `testInvalidBookerIdsAreRejected` |
| `startDate` / `endDate` | Both null; one null; `start < end`; `start == end` | `start > end` | Equal dates are allowed | `testEqualDateFiltersAreAllowed`, `testSingleDateFilterIsAllowed`, `testReversedDateFiltersAreRejected` |
| `startDate` filter | `endTime > startDate` | `endTime <= startDate` | `endTime == startDate` excluded | `testStartFilterBoundaries` |
| `endDate` filter | `startTime < endDate` | `startTime >= endDate` | `startTime == endDate` excluded | `testEndFilterBoundaries` |
| `placeId` | `null` or exact place ID match | Unknown/mismatched ID | N/A | `testPlaceAndSpaceFilters` |
| `spaceId` | `null` or exact space ID match | Unknown/mismatched ID | N/A | `testPlaceAndSpaceFilters` |
| Repository result | Empty or any status | N/A | Cancelled reservations are still listed | `testEmptyRepositoryResult`, `testCancelledReservationIsListed` |

### `createReservation(ReservationRequest)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `startTime` / `endTime` | `end > start`, future, same date, inside hours | `end <= start`; past start; cross-day; outside hours | 1-minute and sub-minute windows | `testCreateValidReservation`, `testCreateOneMinuteReservation`, `testCreateSubMinuteReservation`, `testCreateRejectsUnorderedWindow`, `testCreateRejectsPastStart`, `testCreateRejectsHoursOutsideOperatingWindow`, `testCreateRejectsCrossDayWindow` |
| `bookerId` | Existing booker | Missing booker | N/A | `testCreateValidReservation`, `testCreateRejectsMissingBooker` |
| `spaceId` | Existing space | Missing space | N/A | `testCreateValidReservation`, `testCreateRejectsMissingSpace` |
| `groupSize` | `1..capacity` | `> capacity` | `groupSize == capacity` allowed; `capacity + 1` rejected | `testCreateAtCapacityBoundary`, `testCreateAboveCapacityBoundary` |
| Overlap result | `false` | `true` | N/A | `testCreateValidReservation`, `testCreateRejectsOverlap` |
| Price | Positive rate × billable hours | N/A | 1 minute rounds to `0.42`; 30 seconds truncates to `0.00` | `testCreateValidReservation`, `testCreateOneMinuteReservation`, `testCreateSubMinuteReservation` |

### `modifyReservation(reservationId, ReservationUpdateRequest)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `reservationId` | Existing reservation | Missing reservation | N/A | `testModifyWithOnlyBookerId`, `testModifyMissingReservation` |
| `bookerId` | Existing owner | Missing booker; different booker | N/A | `testModifyWithOnlyBookerId`, `testModifyMissingBooker`, `testModifyByWrongBooker` |
| Status | `CONFIRMED`, `COMPLETED` | `CANCELLED` | N/A | `testModifyWithOnlyBookerId`, `testModifyCancelledReservation` |
| Original `startTime` | Future | Already passed | N/A | `testModifyStartedReservation` |
| Omitted fields | `null` means keep current value | N/A | All omitted and partially omitted | `testModifyWithOnlyBookerId`, `testModifyAllProvidedFields` |
| Merged window | `end > start`, future, within hours | `end <= start`; past start; outside hours | `end == start` rejected | `testModifyRejectsEqualMergedTimes`, `testModifyRejectsPastMergedStart`, `testModifyRejectsHoursOutsideOperatingWindow` |
| Merged `groupSize` | `<= capacity` | `> capacity` | Above-capacity boundary rejected | `testModifyAllProvidedFields`, `testModifyRejectsGroupAboveCapacity` |
| Overlap result | `false` with self excluded | `true` | `hasOverlapExcluding` receives the reservation ID | `testModifyWithOnlyBookerId`, `testModifyRejectsOverlap` |

### `cancelReservation(reservationId, bookerId)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `reservationId` | Existing reservation | Missing reservation | N/A | `testCancelWithPartialRefund`, `testCancelMissingReservation` |
| `bookerId` | Exact owner match | Different/blank ID | N/A | `testCancelWithPartialRefund`, `testCancelByWrongBooker` |
| Status | `CONFIRMED` | `COMPLETED`, `CANCELLED` | N/A | `testCancelWithPartialRefund`, `testCancelNonConfirmedReservation` |
| Start vs now | Strictly future | Past or equal | N/A | `testCancelWithPartialRefund`, `testCancelPastReservation` |
| Lead time vs `refundHours` | `>= refundHours` gets a refund; `< refundHours` gets zero | N/A | Exact boundary uses a fixed clock; above/below use safe offsets | `testCancelAtRefundLeadTimeBoundary`, `testCancelWithPartialRefund`, `testCancelBelowRefundLeadTime` |
| `refundPercent` | `0`, `50`, `100` | `> 100` produces an invalid refund | 0% and 100% are inclusive boundaries | `testZeroRefundPercent`, `testCancelWithPartialRefund`, `testFullRefundPercent`, `testRefundAbovePriceIsRejected` |
| Response | Refund and explanation returned | N/A | 50% explanation contains `50%`; no-refund explanation uses the policy text | `testCancelWithPartialRefund`, `testCancelBelowRefundLeadTime` |

## ReportService partitions

### `generateSpaceReport(placeId, spaceId, startDate, endDate)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `placeId` | Existing ID; `0`/negative are treated as unknown lookups | `null`, missing ID | Missing ID and `0` both give 404 | `testNullPlaceIdIsRejected`, `testMissingPlaceThrowsNotFound`, `testZeroPlaceIdIsUnknownPartition` |
| `spaceId` | `null` (all spaces), owned space, same place ID on a different object | Missing space, null place, space from another place | Ownership compares IDs, not object identity | `testNullSpaceIdEvaluatesEverySpace`, `testMissingSpaceThrowsNotFound`, `testSpaceWithNullPlaceIsRejected`, `testSpaceFromOtherPlaceIsRejected`, `testSamePlaceIdDifferentObjectIsAccepted` |
| `startDate` | Non-null and strictly before `endDate` | `null`, equal to end, after end | Equality and reversed ranges rejected | `testNullStartDateIsRejected`, `testEqualDatesAreRejected`, `testReversedDatesAreRejected` |
| `endDate` | Non-null and strictly after `startDate` | `null`, equal to start, before start | N/A | `testNullEndDateIsRejected`, `testEqualDatesAreRejected`, `testReversedDatesAreRejected` |
| Window length | Sub-day, 24 h, 48 h, multi-day | N/A | Sub-day floors to one day | `testSubDayWindowUsesOneDay`, `testTwoDayWindowScalesAvailability` |
| Reservation times | Inside window; clipped at either side; touching endpoints | N/A | Equality keeps reservation time | `testReservationsAreClippedToWindow`, `testWindowEndpointsAreInclusive` |
| Reservation duration | Multi-hour, sub-minute | N/A | Sub-minute contributes zero hours | `testTwoReservationsProduceExpectedMetrics`, `testSubMinuteReservationCountsZeroHours` |
| Reservation price | Positive, zero, `null` | N/A | `null` price is skipped; zero price is summed | `testNullPriceIsSkippedAndZeroPriceIsSummed` |
| `groupSize` / `capacity` | Below/equal/above capacity; zero capacity | N/A | Equal gives 100%; above is capped at 100%; zero gives 0% | `testGroupSizeAtCapacityProducesFullUsage`, `testGroupSizeAboveCapacityIsCapped`, `testZeroCapacityProducesZeroUsage` |
| Utilization | Below/full/above availability | N/A | Above 100% capped before rounding | `testUtilizationAboveFullIsCapped`, `testTwoReservationsProduceExpectedMetrics` |
| Peak hour/date | One or many reservations | Empty list gives `N/A` | Ties choose lowest hour/date | `testTwoReservationsProduceExpectedMetrics`, `testDateTieChoosesLowestDate`, `testNoReservationsProducesZeroMetrics` |
| Operating hours | Normal, `null`, equal, overnight | N/A | `null`/equal mean 24 h; overnight still yields 12 h for 20:00–08:00 | `testNullOperatingHoursUseTwentyFourHours`, `testEqualOperatingHoursUseTwentyFourHours`, `testOvernightOperatingHours` |
| Names | Non-null and `null` | N/A | `null` names propagate into the response | `testTwoReservationsProduceExpectedMetrics`, `testNullNamesArePropagated` |
| Space list | One space, many spaces, empty list | N/A | Empty list gives zero revenue and no metrics | `testTwoReservationsProduceExpectedMetrics`, `testNullSpaceIdEvaluatesEverySpace`, `testNoSpacesProducesEmptyReport` |

## SpaceService partitions

### `getSpaces(placeId)`, `getSpace(spaceId)`, and `findPlaceIdByName(placeName)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `placeId` in `getSpaces` | `null` selects all; known ID filters; unknown ID returns empty | N/A | N/A | `testGetSpacesWithoutFilterUsesFindAll`, `testGetSpacesWithPlaceIdUsesFilteredLookup`, `testGetSpacesWithUnknownPlaceIdReturnsEmpty` |
| `spaceId` in `getSpace` | Existing ID | Missing ID | N/A | `testGetSpaceReturnsMappedResponse`, `testGetSpaceMissingThrowsNotFound` |
| `placeName` | Known name returns ID | Unknown name returns `null` | N/A | `testFindPlaceIdByNameReturnsId`, `testFindPlaceIdByNameReturnsNullWhenMissing` |

### `createSpace(SpaceRequest)` and `updateSpace(spaceId, SpaceRequest)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| Request object | Non-null | `null` causes NPE in `createSpace` | N/A | `testCreateSpaceWithValidRequest`, `testCreateSpaceWithNullRequestThrows` |
| `spaceName` | Non-blank name | Blank/null are rejected by `@Valid` at the controller, not by the service | Single-character name is valid | `testCreateSpaceWithValidRequest`, `testUpdateSpaceReplacesAllFields` |
| `capacity` | `1`, `6`, `12` | `0` and negative are invalid but not rechecked by the service | `1` is the lower valid boundary | `testCreateSpaceAtLowerValueBoundaries`, `testCreateSpaceLeavesBeanValidationToController`, `testUpdateSpaceReplacesAllFields` |
| `hourlyRate` | `0.00`, `20.00`, `30.00` | Negative is invalid but not rechecked by the service | `0.00` is the lower valid boundary | `testCreateSpaceAtLowerValueBoundaries`, `testCreateSpaceLeavesBeanValidationToController`, `testUpdateSpaceReplacesAllFields` |
| `amenities` | `null` → empty set; one or many; mixed case | Missing amenity in search | `null` is normalized to empty on create | `testCreateSpaceWithNullAmenitiesStoresEmptySet`, `testCreateSpaceWithValidRequest`, `testUpdateSpaceReplacesAllFields` |
| `placeId` | Existing place | Unknown place | N/A | `testCreateSpaceWithValidRequest`, `testCreateSpaceWithUnknownPlaceThrowsNotFound`, `testUpdateSpaceWithUnknownPlaceThrowsNotFound` |
| `spaceId` on update | Existing space | Missing space | Missing space is checked before reading the body | `testUpdateSpaceReplacesAllFields`, `testUpdateSpaceMissingSpaceThrowsNotFound` |

> Bean-validation partitions for `SpaceRequest` fields are enforced by
> `@Valid` in `SpaceController`; the controller integration tests cover
> those HTTP-level rejections. The service unit tests record the
> service-layer behavior explicitly so the boundary is documented rather
> than silently assumed.

### `deleteSpace(spaceId)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| `spaceId` | Existing space | Missing space | N/A | `testDeleteSpaceWithoutReservations`, `testDeleteSpaceMissingThrowsNotFound` |
| Reservation check | `false` allows delete | `true` gives 409 conflict | Any reservation, including cancelled, blocks delete | `testDeleteSpaceWithoutReservations`, `testDeleteSpaceWithReservationsThrowsConflict` |

### `findAvailableSpaces(SpaceSearchRequest)`

| Input | Valid partitions | Invalid partitions | Boundary cases | Tests |
|---|---|---|---|---|
| Request object | Non-null | `null` causes NPE | N/A | `testSearchAcceptsOperatingHourBoundaries` |
| `startTime` / `endTime` | Both non-null and `start < end` | Either `null`; `start == end`; `start > end` | Equality rejected | `testSearchRejectsNullStartTime`, `testSearchRejectsNullEndTime`, `testSearchRejectsEqualTimes`, `testSearchRejectsReversedTimes` |
| Duration | Up to 24 h 59 m 59 s accepted | 25 h rejected | 24 h accepted; 25 h is the first rejected value | `testSearchAcceptsTwentyFourHourBoundary`, `testSearchRejectsTwentyFiveHourDuration` |
| Operating hours | `start >= open` and `end <= close` | Before open or after close | Opening and closing times are inclusive | `testSearchAcceptsOperatingHourBoundaries`, `testSearchRejectsStartBeforeOpening`, `testSearchRejectsEndAfterClosing` |
| `placeId` | Existing place | Unknown place | N/A | `testSearchWithUnknownPlaceThrowsNotFound` |
| `minCapacity` | `0`, below/equal to capacity | Above capacity | Equality includes the space | `testSearchIncludesSpaceAtCapacityBoundary`, `testSearchExcludesSpaceAboveCapacityBoundary` |
| `amenities` | `null`; subset present; mixed case | Missing amenity | Request values are lowercased before matching | `testSearchIncludesSpaceWithRequiredAmenity`, `testSearchExcludesSpaceWithoutRequiredAmenity`, `testSearchNormalizesAmenityCase` |
| Overlap flag | `false` includes space | `true` excludes space | N/A | `testSearchExcludesOverlappingSpace`, `testSearchCalculatesFourHourPrice` |
| Price | Positive hourly rate × duration | N/A | 4 h → `80.00`; 5 min → `1.67` (HALF_UP) | `testSearchCalculatesFourHourPrice`, `testSearchRoundsFiveMinutePrice` |
| Space list | One matching space; empty list | N/A | Empty place list returns no results | `testSearchAcceptsTwentyFourHourBoundary`, `testSearchIncludesSpaceWithRequiredAmenity` |
