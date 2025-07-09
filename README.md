# Observability Core - Backend

This repository contains the source code for the backend service of the "Observability Core" project. This service is responsible for polling target applications, collecting health and performance metrics, storing them in a database, and providing a REST API for the frontend dashboard.

## Features

-   **Health Polling:** Periodically checks the `/actuator/health` endpoints of registered services.
-   **Metric Storage:** Stores historical status and response time data in a PostgreSQL database.
-   **Load Testing:** Provides an API to initiate on-demand load tests against target services.
-   **REST API:** Secure API for the frontend to consume data and trigger actions.
-   **Authentication & Authorization:** JWT-based security with user roles.

## Tech Stack

-   **Language:** Java 21
-   **Framework:** Spring Boot 3.x (Web, Data JPA, Security)
-   **Database:** PostgreSQL
-   **Build Tool:** Apache Maven
-   **Containerization:** Docker

## Running The Entire System (including this backend)

This repository contains the main `docker-compose.yml` file to run the entire distributed system.

### Prerequisites

-   Docker & Docker Compose
-   An internet connection to pull images from GitHub Container Registry (GHCR).

### Steps

1.  **Clone this repository:**
    ```bash
    git clone https://github.com/auzienko/observability-core-backend.git
    cd observability-core-backend
    ```

2.  **Log in to GHCR (only needed for private images, but good practice):**
    You may need to log in to GHCR if the mock service images are private.
    ```bash
    echo $CR_PAT | docker login ghcr.io -u YOUR_USERNAME --password-stdin
    ```
    *(Replace `YOUR_USERNAME` and use a Personal Access Token `CR_PAT` with `read:packages` scope)*

3.  **Launch the system:**
    This command will build the backend service locally and pull the mock service images from GHCR.
    ```bash
    docker-compose up --build
    ```

4.  **The system is now running:**
    -   **Backend API:** `http://localhost:8080`
    -   **PostgreSQL DB:** `localhost:5432`
    -   **User Service (Mock):** `http://localhost:9001`
    -   **Transaction Service (Mock):** `http://localhost:9002`

## CI/CD Pipeline

A GitHub Actions workflow is configured to automatically build and publish the Docker image for this service to GHCR at `ghcr.io/auzienko/observability-core-backend:latest` on every push to the `main` branch.