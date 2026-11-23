Room Booking Client Application
Overview

The Room Booking Client is an independent web application that allows users to interact with a Room Booking REST API.

The client communicates with the service exclusively through HTTP requests and JSON responses. It does not directly access the service's database or depend on its internal implementation.

The application can work with any service that implements the same REST API contract.

Features
Browse and search available spaces.
Filter spaces by place, time, capacity, and amenities.
Create room reservations.
View and modify existing reservations.
Cancel reservations and view refund information.
View personal usage: total spending, booked hours, upcoming bookings, and past bookings.
Technology Stack
React
Vite
JavaScript
HTML/CSS
Fetch API
Installation

Navigate to the client application directory:

bash
cd client-app


Install dependencies:

bash
npm install

Configuration

Create a .env file in the client-app directory:

env
VITE_API_BASE_URL=http://localhost:8080


Replace the URL with the deployed API service URL when connecting to the cloud service.

Running the Client

Start the development server:

bash
npm run dev


Open the URL displayed in the terminal, usually:

text
http://localhost:5173

Connecting to the Cloud Service

To connect to the deployed Room Booking API, update VITE_API_BASE_URL to the cloud service URL.

The client does not require the backend to run on the same machine.

Multiple Client Instances

Each browser installation generates a unique client instance UUID, stored in browser local storage.

The client sends this identifier with every API request through the X-Client-ID HTTP header.

The backend can use this identifier to distinguish requests from different client instances.

User identity is tracked separately through the Booker ID supplied when creating or retrieving reservations.

API Independence

The client communicates with the backend exclusively through documented REST endpoints.

All HTTP communication is encapsulated in the src/services/ directory.

The API base URL is configurable through an environment variable, allowing the client to connect to different implementations of the same API without changing the client code.