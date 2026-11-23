import { apiRequest } from "./apiClient";

export function getSpaceReports(placeId, startDate, endDate) {
  const query = new URLSearchParams({
    placeId,
    startDate,
    endDate,
  });

  return apiRequest(`/api/reports/spaces?${query.toString()}`);
}