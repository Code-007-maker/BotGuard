# BotGuard - Backend Engineering Assignment: Core API & Guardrails

## 🚀 Project Overview

BotGuard is a robust, stateless Spring Boot microservice designed to act as a centralized API gateway and guardrail system.

The system is built to:

- Handle high concurrency efficiently
- Prevent AI compute runaway using strict mathematical guardrails
- Manage distributed state using Redis
- Maintain strong data integrity with PostgreSQL as the source of truth

---

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **PostgreSQL** — Source of Truth
- **Redis / Spring Data Redis** — Gatekeeper & Distributed State
- **Docker & Docker Compose**

---

## ⚙️ How to Run Locally

## 1. Start Infrastructure

Make sure Docker is running, then start PostgreSQL and Redis:

```bash
docker-compose up -d
```

---

## 2. Run the Application

Start the Spring Boot application using Maven wrapper:

```bash
./mvnw spring-boot:run
```

You can also run it directly from your IDE.

---

## 3. API Testing

Import the included `Postman_Collection.json` file into Postman to test all REST APIs.

---

# 🔒 Architecture & Concurrency Strategy

The biggest challenge in this assignment is preventing race conditions under heavy concurrency.

Example:

- 200 concurrent bot requests hitting the server at the exact same millisecond
- System must still guarantee limits are never violated

---

## 1. Horizontal Cap — Preventing the Check-Then-Act Race Condition

A naive implementation would:

1. Read current count from Redis
2. Check if count < 100
3. Increment the count

This approach fails under concurrency because multiple threads can read the same value before incrementing.

---

## ✅ Solution: Redis Lua Scripts

Redis Lua Scripts provide atomic execution.

Redis works on a single-threaded event loop, meaning:

- A Lua script executes completely before any other Redis command runs
- No race condition can occur during execution

The application sends a Lua script to Redis that:

- Checks current `bot_count`
- If count < 100:
  - Increment counter
  - Return success
- Else:
  - Reject operation

This guarantees:

- No 101st comment can ever be inserted
- Full thread safety
- Stateless application design
- No JVM/database locking required

---

## 2. Cooldown Cap — Distributed Atomic Locks

To prevent a bot from interacting with the same user multiple times within 10 minutes, Redis distributed locks are used.

The implementation uses:

```text
SET key value NX EX 600
```

Where:

- `NX` → Only set if key does not exist
- `EX 600` → Expire after 600 seconds (10 minutes)

This operation is atomic.

Result:

- First request succeeds
- Concurrent duplicate requests fail instantly
- Blocked requests return:

```text
429 Too Many Requests
```

---

## 3. Smart Batching & Notification Engine

To prevent notification spam:

### Notification Flow

1. Check if user is inside active 15-minute cooldown
2. If cooldown exists:
   - Push notification payload into Redis List

```text
RPUSH user:{id}:pending_notifs
```

3. A Spring `@Scheduled` cron job runs every 5 minutes
4. Job atomically:
   - Reads all pending notifications
   - Logs summarized batch
   - Clears queue

Redis operations used:

```text
LRANGE
DEL
```

---

# 🗄️ Data Integrity Strategy

The application follows a strict **Gatekeeper Pattern**.

## Flow

```text
Request → Redis Validation → PostgreSQL Write
```

PostgreSQL is only accessed after Redis mathematically proves the action is allowed.

If Redis rejects the request:

- Database transaction never starts
- Expensive IO operations are avoided
- System remains highly scalable

---

# 📂 Project Structure

```text
botguard/
│
├── src/
├── docker-compose.yml
├── pom.xml
├── README.md
└── Postman_Collection.json
```

---

# ▶️ Steps To Run

## Create README

Create a file named:

```text
README.md
```

Paste this content inside the file.

---

## Export Postman Collection

Export your API test collection as:

```text
Postman_Collection.json
```

Save it in the project root directory.

---

## Push Project to GitHub

Initialize repository and push code:

```bash
git init
git add .
git commit -m "Initial commit: Completed BotGuard backend assignment"
git branch -M main
git remote add originhttps://github.com/Code-007-maker/BotGuard
git push -u origin main
```

---

#  Key Highlights

- Fully Stateless Architecture
- Redis Atomic Operations
- Lua Script Concurrency Protection
- Distributed Cooldown Locks
- Smart Notification Batching
- PostgreSQL Data Integrity
- Dockerized Infrastructure
- Production-Oriented Design

---

# 📌 Author

Arbaj khan
