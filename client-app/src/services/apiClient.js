import { getClientId } from "./clientIdentity";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

export async function apiRequest(endpoint, options = {}) {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Client-ID": getClientId(),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const contentType = response.headers.get("content-type") || "";
    let errorMessage = `Request failed: ${response.status}`;

    if (contentType.includes("application/json")) {
      const problem = await response.json();
      errorMessage = problem.detail || problem.message || problem.title || errorMessage;
    } else {
      const responseText = await response.text();
      if (responseText) {
        errorMessage = responseText;
      }
    }

    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type");

  if (contentType?.includes("application/json")) {
    return response.json();
  }

  return null;
}