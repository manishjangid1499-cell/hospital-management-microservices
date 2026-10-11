# Hospital Management System (HMS) - Microservices Architecture

A robust, enterprise-grade Hospital Management System built with a Spring Boot microservices backend and a responsive role-based web frontend. The system provides role-based access control and integrated workflows for Hospital Administrators, Doctors, Receptionists, Patients, Pharmacists, and Billing Officers.

---

## 1. System Architecture

The project is structured as a multi-module Maven reactor project comprising 8 microservice modules and a role-based web frontend:

```
hospital-management-microservices/
├── eureka-server/          # Service Registry & Discovery (Port 8761)
├── api-gateway/            # Reactive Gateway with Global JWT Filter & Routing (Port 9090)
├── auth-service/           # User Auth, Role Management, JWT Issuance (Port 8081)
├── patient-service/        # Patient Registration, Demographics & Medical Profile (Port 8082)
├── appointment-service/    # Doctor Availability & Appointment Scheduling (Port 8083)
├── ehr-service/            # Electronic Health Records & Clinical Encounters (Port 8084)
├── pharmacy-service/       # Medication Inventory & Prescription Dispensing (Port 8085)
├── billing-service/        # Invoicing, Payment Tracking & Insurance Copay (Port 8086)
└── frontend/               # Role-based Web Application UI
```

### Key Architectural Characteristics
- **Service Discovery**: Netflix Eureka registry facilitates dynamic discovery and client-side load balancing.
- **Edge Routing & Security**: Spring Cloud Gateway intercepts all external HTTP traffic on port 9090, validates JWT bearer tokens, and routes traffic downstream with user identity headers.
- **Inter-Service Communication**: Declarative Spring Cloud OpenFeign clients enable secure, synchronous communication between microservices.
- **Isolated Datastores**: Each operational service connects to its dedicated MySQL database schema.

---

## 2. Microservices & Port Allocations

| Service Name | Port | Description | Database Schema |
| :--- | :--- | :--- | :--- |
| **Eureka Server** | `8761` | Service Registry & Discovery Server | N/A |
| **API Gateway** | `9090` | Edge Router, Route Validator & JWT Filter | N/A |
| **Auth Service** | `8081` | Authentication, JWT Generation, RBAC | `auth_db` |
| **Patient Service** | `8082` | Patient Records & Medical Information | `patient_db` |
| **Appointment Service** | `8083` | Booking, Doctor Slots & Appointments | `appointment_db` |
| **EHR Service** | `8084` | Clinical Records, Diagnoses & Treatment History | `ehr_db` |
| **Pharmacy Service** | `8085` | Pharmacy Inventory, Stock & Prescriptions | `pharmacy_db` |
| **Billing Service** | `8086` | Invoices, Payments, Billing Statements | `billing_db` |
| **Frontend UI** | Static | Bootstrap 5 + Vanilla JavaScript Web UI | N/A |

---

## 3. Technology Stack

- **Java Version**: Java 17+ (Compatible with JDK 17 & JDK 21)
- **Backend Framework**: Spring Boot 3.2.5
- **Cloud Components**: Spring Cloud 2023.0.1 (Eureka Server/Client, Spring Cloud Gateway, OpenFeign)
- **Security**: Spring Security 6.x, JJWT (`io.jsonwebtoken` 0.11.5)
- **Data & Persistence**: Spring Data JPA, Hibernate, MySQL Connector/J
- **Build Tool**: Apache Maven (Multi-Module Reactor POM)
- **Frontend**: Multi-page HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3

---

## 4. Security & Environment Configuration

All microservices adhere to 12-factor application security practices. Credentials and signing keys are read dynamically from environment variables, preventing hardcoded secrets in the codebase.

### Mandatory Environment Variables

Before launching the services, ensure the following environment variables are exported or configured in your deployment environment:

| Variable | Description | Default Fallback |
| :--- | :--- | :--- |
| `JWT_SECRET` | **Required.** Cryptographic secret for signing and validating JWT tokens (must match between Auth Service and Gateway). | *None (Strictly Required)* |
| `JWT_EXPIRATION` | Token validity duration in milliseconds. | `86400000` (24 hours) |
| `DB_HOST` | Host address of the MySQL database server. | `localhost` |
| `DB_PORT` | Port number of the MySQL database server. | `3306` |
| `DB_USERNAME` | Database connection username. | `root` |
| `DB_PASSWORD` | Database connection password. | `root` |
| `APP_SEED_DEMO_DATA` | Opt-in flag to populate sample demonstration accounts and inventory. | `false` |

> **Security Note:** Demo data initialization (`app.seed-demo-data=true`) is **disabled by default** to protect production and staging environments from test accounts and known default credentials.

---

## 5. Getting Started & Local Development

### Prerequisites
1. **Java 17 or 21** installed and configured on your `PATH`.
2. **Apache Maven 3.8+** installed.
3. **MySQL Server 8.0+** running on `localhost:3306`.

### Building the Project
From the repository root directory, build all microservice modules using Maven:

```bash
mvn clean install -DskipTests
```

### Starting the Microservices
Start the services in the following order:

1. **Eureka Server**:
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```
   *Verify dashboard at `http://localhost:8761`.*

2. **API Gateway**:
   ```bash
   cd ../api-gateway
   export JWT_SECRET="your-secure-base64-encoded-jwt-secret-string-here"
   mvn spring-boot:run
   ```
   *Gateway runs on `http://localhost:9090`.*

3. **Core Backend Services** (Run in separate terminal windows with `JWT_SECRET` set):
   ```bash
   # Auth Service
   cd ../auth-service && mvn spring-boot:run

   # Patient Service
   cd ../patient-service && mvn spring-boot:run

   # Appointment Service
   cd ../appointment-service && mvn spring-boot:run

   # EHR Service
   cd ../ehr-service && mvn spring-boot:run

   # Pharmacy Service
   cd ../pharmacy-service && mvn spring-boot:run

   # Billing Service
   cd ../billing-service && mvn spring-boot:run
   ```

4. **Running the Frontend Application**:
   Serve the `frontend/` directory with any static web server (such as Python's built-in HTTP server, VS Code Live Server, or Nginx):
   ```bash
   cd frontend
   python -m http.server 3000
   ```
   Open your browser and navigate to `http://localhost:3000`.

---

## 6. Demonstration & Testing Mode

To seed sample data (administrator, doctor, receptionist, pharmacist, and billing officer accounts, along with initial pharmacy inventory):

1. Set `app.seed-demo-data=true` in `auth-service` and `pharmacy-service`, or export the environment variable:
   ```bash
   export APP_SEED_DEMO_DATA=true
   ```
2. The initial demo administrator account will be provisioned idempotently:
   - **Email**: `admin@hospital.com`
   - **Password**: `admin123`
   - **Role**: `ROLE_ADMIN`

*(Note: Admin password resets are not forced on subsequent restarts; existing passwords are preserved.)*
