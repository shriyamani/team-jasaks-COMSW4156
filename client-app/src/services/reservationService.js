import { apiRequest } from "./apiClient";

export function getReservations(userId) {
  const query = new URLSearchParams({ userId });

  return apiRequest(`/api/reservations?${query.toString()}`);
}

export function createReservation(reservation) {
  return apiRequest("/api/reservations", {
    method: "POST",
    body: JSON.stringify(reservation),
  });
}

export function cancelReservation(id, bookerId) {
  const query = new URLSearchParams({ bookerId });

  return apiRequest(
    `/api/reservations/${id}/cancel?${query.toString()}`,
    {
      method: "POST",
    }
  );
}