import { useState } from "react";

import {
  getSpaces,
  searchSpaces,
} from "./services/spaceService";

import {
  getReservations,
  createReservation,
  cancelReservation,
} from "./services/reservationService";

import { getClientId } from "./services/clientIdentity";

import "./App.css";

export default function App() {
  const [page, setPage] = useState("spaces");

  const [userId, setUserId] = useState(
    localStorage.getItem("booker_id") || ""
  );

  const [spaces, setSpaces] = useState([]);
  const [reservations, setReservations] = useState([]);

  const [placeId, setPlaceId] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [capacity, setCapacity] = useState(1);
  const [amenities, setAmenities] = useState("");

  const [selectedSpace, setSelectedSpace] = useState("");
  const [groupSize, setGroupSize] = useState(1);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const clientId = getClientId();

  // Handle API operations and display errors.
  async function performAction(action) {
    setLoading(true);
    setMessage("");
    setError("");

    try {
      await action();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  // Save the current Booker ID.
  function saveUser() {
    if (!userId.trim()) {
      setError("Please enter a Booker ID.");
      return;
    }

    localStorage.setItem("booker_id", userId.trim());
    setUserId(userId.trim());

    setMessage("User ID saved successfully.");
    setError("");
  }

  // Load all spaces.
  function loadSpaces() {
    performAction(async () => {
      const data = await getSpaces();

      setSpaces(Array.isArray(data) ? data : []);
      setMessage("Spaces loaded successfully.");
    });
  }

  // Search available spaces.
  function handleSearch() {
    performAction(async () => {
      if (!placeId.trim() || !startTime || !endTime) {
        throw new Error(
          "Please enter a place name or place ID, start time, and end time."
        );
      }

      let placeName = placeId.trim();

      if (/^\d+$/.test(placeName)) {
        const matchingSpaces = await getSpaces(Number(placeName));

        if (!Array.isArray(matchingSpaces) || matchingSpaces.length === 0) {
          throw new Error("No place or spaces were found for that place ID.");
        }

        placeName = matchingSpaces[0].placeName;
      }

      const params = {
        placeName,
        startTime,
        endTime,
        minCapacity: Number(capacity),
      };

      if (amenities.trim()) {
        params.amenities = amenities.trim();
      }

      const data = await searchSpaces(params);

      setSpaces(Array.isArray(data) ? data : []);
      setMessage("Search completed.");
    });
  }

  // Create a reservation.
  function handleBooking() {
    performAction(async () => {
      if (!userId.trim()) {
        throw new Error("Please enter your Booker ID first.");
      }

      if (!selectedSpace || !startTime || !endTime) {
        throw new Error("Please select a space and booking times.");
      }

      const reservation = {
        bookerId: userId.trim(),
        spaceId: Number(selectedSpace),
        startTime,
        endTime,
        groupSize: Number(groupSize),
      };

      const result = await createReservation(reservation);

      setMessage(
        `Reservation created successfully! ${
          result?.reservationId
            ? `Reservation ID: ${result.reservationId}`
            : ""
        }`
      );

      setSelectedSpace("");

      const data = await getReservations(userId.trim());
      setReservations(Array.isArray(data) ? data : []);
    });
  }

  // Retrieve reservations for the current user.
  function loadReservations() {
    if (!userId.trim()) {
      setError("Please enter your Booker ID first.");
      return;
    }

    performAction(async () => {
      const data = await getReservations(userId.trim());

      setReservations(Array.isArray(data) ? data : []);
      setMessage("Reservations loaded successfully.");
    });
  }

  // Cancel a reservation.
  function handleCancel(id) {
    if (!userId.trim()) {
      setError("Please enter your Booker ID first.");
      return;
    }

    if (!window.confirm("Are you sure you want to cancel this reservation?")) {
      return;
    }

    performAction(async () => {
      const result = await cancelReservation(id, userId.trim());

      setMessage(
        `Reservation cancelled. Refund: ${
          result?.refundAmount ?? "See cancellation response"
        }`
      );

      const data = await getReservations(userId.trim());

      setReservations(Array.isArray(data) ? data : []);
    });
  }

  // Handle navigation.
  function navigate(nextPage) {
    setPage(nextPage);
    setMessage("");
    setError("");

    if ((nextPage === "reservations" || nextPage === "usage") && userId.trim()) {
      loadReservations();
    }
  }

  // Helpers for displaying different DTO field names.
  function getSpaceName(space) {
    return space.name || space.spaceName || `Space ${space.spaceId ?? space.id ?? ""}`;
  }

  function getReservationId(reservation) {
    return reservation.reservationId ?? reservation.id;
  }

  const now = new Date();
  const upcomingReservations = reservations.filter((reservation) => {
    const start = reservation.startTime ? new Date(reservation.startTime) : null;
    return reservation.status !== "CANCELLED" && start && start >= now;
  });
  const pastReservations = reservations.filter((reservation) => {
    const end = reservation.endTime ? new Date(reservation.endTime) : null;
    return end && end < now;
  });
  const paidReservations = reservations.filter(
    (reservation) => reservation.status !== "CANCELLED"
  );
  const totalSpent = paidReservations.reduce((sum, reservation) => {
    const price = Number(reservation.price ?? reservation.totalPrice ?? 0);
    return sum + (Number.isFinite(price) ? price : 0);
  }, 0);
  const totalBookedHours = paidReservations.reduce((sum, reservation) => {
    if (!reservation.startTime || !reservation.endTime) return sum;
    const start = new Date(reservation.startTime);
    const end = new Date(reservation.endTime);
    const hours = (end - start) / (1000 * 60 * 60);
    return sum + (Number.isFinite(hours) && hours > 0 ? hours : 0);
  }, 0);

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>SpaceBook</h1>
          <p>Room and Workspace Reservation</p>
        </div>

        <div className="client-info">
          <span>Client connected</span>
          <small>Client ID: {clientId.slice(0, 8)}...</small>
        </div>
      </header>

      <nav className="navigation">
        <button
          className={page === "spaces" ? "active" : ""}
          onClick={() => navigate("spaces")}
        >
          Spaces
        </button>

        <button
          className={page === "reservations" ? "active" : ""}
          onClick={() => navigate("reservations")}
        >
          My Reservations
        </button>

        <button
          className={page === "usage" ? "active" : ""}
          onClick={() => navigate("usage")}
        >
          My Usage
        </button>
      </nav>

      <main className="content">

        <section className="user-section">
          <label>Booker ID</label>

          <input
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="Enter your Booker ID"
          />

          <button onClick={saveUser}>
            Save User
          </button>
        </section>

        {message && (
          <div className="success">
            {message}
          </div>
        )}

        {error && (
          <div className="error">
            {error}
          </div>
        )}

        {loading && (
          <p className="loading">Loading...</p>
        )}

        {/* SPACES PAGE */}

        {page === "spaces" && (
          <section>
            <h2>Find Available Spaces</h2>

            <div className="panel">
              <label>Place ID or Place Name</label>

              <input
                value={placeId}
                onChange={(e) => setPlaceId(e.target.value)}
                placeholder="e.g. Butler Library or 1"
              />

              <label>Start Time</label>

              <input
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />

              <label>End Time</label>

              <input
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
              />

              <label>Minimum Capacity</label>

              <input
                type="number"
                min="1"
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
              />

              <label>Amenities (optional)</label>

              <input
                value={amenities}
                onChange={(e) => setAmenities(e.target.value)}
                placeholder="e.g. wifi"
              />

              <div className="button-row">
                <button onClick={handleSearch}>
                  Search Available Spaces
                </button>

                <button
                  className="secondary"
                  onClick={loadSpaces}
                >
                  Load All Spaces
                </button>
              </div>
            </div>

            <h3>Spaces</h3>

            {spaces.length === 0 && (
              <p>No spaces loaded yet.</p>
            )}

            <div className="cards">
              {spaces.map((space, index) => {
                const id = space.spaceId ?? space.id;

                return (
                  <div className="space-card" key={id ?? index}>
                    <h3>{getSpaceName(space)}</h3>

                    <p>Space ID: {id ?? "N/A"}</p>

                    <p>
                      Capacity: {space.capacity ?? space.maxCapacity ?? "N/A"}
                    </p>

                    <p>
                      Amenities: {
                        Array.isArray(space.amenities)
                          ? space.amenities.join(", ")
                          : space.amenities ?? "N/A"
                      }
                    </p>

                    <p>
                      Hourly Rate: {
                        space.hourlyRate ?? space.pricePerHour ?? "N/A"
                      }
                    </p>

                    <button
                      onClick={() => setSelectedSpace(String(id))}
                    >
                      Select Space
                    </button>
                  </div>
                );
              })}
            </div>

            {selectedSpace && (
              <div className="panel booking-panel">
                <h3>Book Space #{selectedSpace}</h3>

                <p>
                  Booking times are taken from the search fields above.
                </p>

                <label>Group Size</label>

                <input
                  type="number"
                  min="1"
                  value={groupSize}
                  onChange={(e) => setGroupSize(e.target.value)}
                />

                <button onClick={handleBooking}>
                  Confirm Reservation
                </button>
              </div>
            )}
          </section>
        )}

        {/* RESERVATIONS PAGE */}

        {page === "reservations" && (
          <section>
            <h2>My Reservations</h2>

            <button onClick={loadReservations}>
              Load My Reservations
            </button>

            {reservations.length === 0 && (
              <p>No reservations loaded.</p>
            )}

            <div className="cards">
              {reservations.map((reservation, index) => {
                const id = getReservationId(reservation);

                return (
                  <div className="space-card" key={id ?? index}>
                    <h3>
                      Reservation #{id ?? "N/A"}
                    </h3>

                    <p>
                      Space: {
                        reservation.spaceName ??
                        reservation.space?.name ??
                        reservation.spaceId ??
                        "N/A"
                      }
                    </p>

                    <p>
                      Start: {
                        reservation.startTime ?? "N/A"
                      }
                    </p>

                    <p>
                      End: {
                        reservation.endTime ?? "N/A"
                      }
                    </p>

                    <p>
                      Status: {
                        reservation.status ?? "N/A"
                      }
                    </p>

                    <p>
                      Price: {
                        reservation.price ??
                        reservation.totalPrice ??
                        "N/A"
                      }
                    </p>

                    {id != null && reservation.status !== "CANCELLED" && (
                      <button
                        className="danger"
                        onClick={() => handleCancel(id)}
                      >
                        Cancel Reservation
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* USAGE PAGE */}

        {page === "usage" && (
          <section>
            <h2>My Usage</h2>

            <button onClick={loadReservations}>Refresh Usage</button>

            <div className="cards">
              <div className="space-card">
                <h3>Total Spending</h3>
                <p>${totalSpent.toFixed(2)}</p>
              </div>

              <div className="space-card">
                <h3>Total Reservations</h3>
                <p>{reservations.length}</p>
              </div>

              <div className="space-card">
                <h3>Total Booked Hours</h3>
                <p>{totalBookedHours.toFixed(2)} hours</p>
              </div>

              <div className="space-card">
                <h3>Upcoming Bookings</h3>
                <p>{upcomingReservations.length}</p>
              </div>

              <div className="space-card">
                <h3>Past Bookings</h3>
                <p>{pastReservations.length}</p>
              </div>
            </div>

            <h3>Past Bookings</h3>
            {pastReservations.length === 0 && <p>No past bookings.</p>}

            <div className="cards">
              {pastReservations.map((reservation, index) => (
                <div className="space-card" key={getReservationId(reservation) ?? index}>
                  <h3>{reservation.spaceName || `Space ${reservation.spaceId ?? "N/A"}`}</h3>
                  <p>Start: {reservation.startTime ?? "N/A"}</p>
                  <p>End: {reservation.endTime ?? "N/A"}</p>
                  <p>Status: {reservation.status ?? "N/A"}</p>
                  <p>Price: ${Number(reservation.price ?? 0).toFixed(2)}</p>
                </div>
              ))}
            </div>

            <h3>Upcoming Bookings</h3>
            {upcomingReservations.length === 0 && <p>No upcoming bookings.</p>}

            <div className="cards">
              {upcomingReservations.map((reservation, index) => (
                <div className="space-card" key={getReservationId(reservation) ?? index}>
                  <h3>{reservation.spaceName || `Space ${reservation.spaceId ?? "N/A"}`}</h3>
                  <p>Start: {reservation.startTime ?? "N/A"}</p>
                  <p>End: {reservation.endTime ?? "N/A"}</p>
                  <p>Status: {reservation.status ?? "N/A"}</p>
                  <p>Price: ${Number(reservation.price ?? 0).toFixed(2)}</p>
                </div>
              ))}
            </div>
          </section>
        )}

      </main>

      <footer>
        Room Booking Client · Independent REST API Consumer
      </footer>
    </div>
  );
}