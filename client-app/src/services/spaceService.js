import { apiRequest } from "./apiClient";

export function getSpaces(placeId) {
  const query = placeId
    ? `?placeId=${encodeURIComponent(placeId)}`
    : "";

  return apiRequest(`/api/spaces${query}`);
}

export function searchSpaces(params) {
  const query = new URLSearchParams(params);

  return apiRequest(`/api/spaces/search?${query.toString()}`);
}