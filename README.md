# Load Balancer

A lightweight, configurable load balancer built in Java that supports multiple load balancing strategies and automatic health checking.

## Features

- **Multiple Load Balancing Strategies**: Round Robin
- **Automatic Health Checking**: Periodic health checks to ensure only healthy servers receive traffic
- **Service Discovery Integration**: Automatically discovers services from Eureka Server
- **Docker & Local Environment Support**: Works seamlessly in both Docker containers and local development
- **Lightweight**: Built with OkHttp for efficient HTTP handling

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Eureka Server (for service discovery)
- Target services with health endpoints (`/health` or `/actuator/health`)

## Quick Start

### 1. Build the Project

```bash
mvn clean compile
```
### 2. Run the Load Balancer
```bash
java -cp target/classes com.ezedin.loadbalancer.Main
```
The load balancer will start on port 8040 by default.

## Load Balancing Strategies
# Round Robin
Cycles through available healthy servers in sequence.

## Health Checking
The load balancer automatically performs health checks on registered services:

- TCP Check: Verifies the server is reachable on the network level

- HTTP Check: Verifies the /health endpoint returns 200 status with "healthy" in response body

- Automatic Interval: Health checks run every 2 seconds

- Automatic Failover: Unhealthy servers are automatically removed from rotation

## Service Discovery
The load balancer integrates with Eureka service discovery:

- Automatically discovers services registered with Eureka

- Supports both Docker internal IPs and localhost addresses

- Configurable Eureka server URL
## Eureka Configuration
By default, the load balancer connects to Eureka at http://localhost:8761/eureka/apps/[application-name].

## Docker Support
The load balancer works in both environments:

- Docker Environment: Uses container internal IP addresses

- Local Development: Uses localhost with mapped ports

- Automatic Detection: Health checks determine reachable endpoints
