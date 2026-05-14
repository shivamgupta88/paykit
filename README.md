# PayKit

Multi-tenant invoicing and payment collection platform. Business owners create invoices, share payment links with customers, collect payments via Razorpay, and withdraw earnings to their bank/UPI.

Built with Java 21, Spring Boot 3.2, PostgreSQL, Redis.

---

## How it works

```
Owner signs up        Customer receives       Payment lands in
  and creates    --->   payment link and   ---> owner's PayKit wallet
  an invoice          pays via Razorpay        (2% platform fee deducted)
                                                     |
                                                     v
                                              Owner withdraws to
                                              bank account or UPI
```

Each signup creates an isolated tenant. All data (customers, invoices, payments) is scoped to that tenant — no cross-tenant access.

---

## Tech stack

| Layer | Tech |
|---|---|
| Runtime | Java 21, Spring Boot 3.2.5 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Payments | Razorpay Orders API |
| Migrations | Flyway |
| Auth | JWT (HMAC-SHA256, jjwt 0.12.5) |
| PDF | iText7 8.0.4 |
| Observability | Micrometer + Prometheus, Resilience4j |
| Build | Maven, multi-stage Docker |

---

## Project structure

```
src/main/java/com/paykit/
|
+-- common/
|   +-- BaseEntity.java            # id, tenantId, timestamps, version (all entities extend this)
|   +-- AppConstants.java          # header names, defaults, crypto params
|
+-- config/
|   +-- AsyncConfig.java           # thread pools for email, pdf, webhooks
|   +-- CorsConfig.java            # allowed origins
|   +-- JacksonConfig.java         # json serialization
|   +-- MetricsConfig.java         # prometheus counters
|   +-- OpenApiConfig.java         # swagger/openapi setup
|   +-- RazorpayClientConfig.java  # razorpay sdk bean
|   +-- RazorpayHealthIndicator.java # /actuator/health razorpay check
|   +-- RedisConfig.java           # cache manager, template
|   +-- RequestLoggingFilter.java  # request-id, method/uri/status/duration logging
|
+-- security/
|   +-- SecurityConfig.java        # filter chain, public/private routes
|   +-- JwtAuthenticationFilter.java # extracts token, sets security + tenant context
|   +-- JwtService.java            # interface
|   +-- JwtServiceImpl.java        # token create/validate, claims extraction
|
+-- tenant/
|   +-- TenantContext.java         # ThreadLocal<UUID> for current tenant
|   +-- TenantEntityListener.java  # @PrePersist — auto-sets tenantId on every entity
|
+-- exception/
|   +-- GlobalExceptionHandler.java
|   +-- ResourceNotFoundException.java      # 404
|   +-- DuplicateResourceException.java     # 409
|   +-- InvalidStateTransitionException.java # 422
|   +-- PaymentProcessingException.java     # 502
|   +-- IdempotencyConflictException.java   # 409
|   +-- EncryptionException.java            # 500
|   +-- TenantResolutionException.java      # 400
|   +-- ErrorResponse.java                  # standard error body
|
+-- domain/
    +-- auth/
    |   +-- AuthController.java        # POST /api/auth/register, /login
    |   +-- AuthService.java
    |   +-- AuthServiceImpl.java
    |   +-- dto/
    |
    +-- tenant/
    |   +-- Tenant.java                # name, slug, status, contactEmail
    |   +-- TenantController.java      # POST /api/tenants, GET by slug/id
    |   +-- TenantService[Impl].java
    |   +-- TenantMapper.java
    |
    +-- user/
    |   +-- User.java                  # email, passwordHash, fullName, role, status
    |   +-- UserRepository.java
    |
    +-- customer/
    |   +-- Customer.java              # name, email*, phone*, billingAddress*, gstin
    |   +-- CustomerController.java    # CRUD + paginated list
    |   +-- CustomerService[Impl].java
    |   +-- CustomerMapper.java
    |   +-- EncryptedStringConverter.java  # AES-GCM for PII fields (*)
    |   +-- EncryptionUtil.java
    |
    +-- invoice/
    |   +-- Invoice.java               # number, status, dates, amounts, items
    |   +-- InvoiceItem.java           # description, qty, unitPrice, taxRate, lineTotal
    |   +-- InvoiceController.java     # CRUD + status transition + PDF download
    |   +-- InvoiceService[Impl].java  # auto-numbering, state machine, email on send
    |   +-- InvoicePdfService.java     # iText7 PDF generation
    |   +-- InvoiceMapper.java
    |
    +-- payment/
    |   +-- Payment.java               # razorpay ids, amount, status, capturedAt
    |   +-- PaymentController.java     # initiate, verify, get, list by invoice
    |   +-- PublicPaymentController.java # unauthenticated — customer-facing payment
    |   +-- PaymentService[Impl].java  # razorpay order, signature verify, wallet credit
    |   +-- RazorpayProperties.java    # key-id, key-secret, webhook-secret
    |   +-- WebhookController.java     # POST /api/webhooks/razorpay
    |   +-- PaymentMapper.java
    |
    +-- wallet/
    |   +-- TenantWallet.java          # balance, totalEarned, totalWithdrawn
    |   +-- PayoutRequest.java         # amount, bank/upi details, status
    |   +-- WalletController.java      # balance, withdraw, payout history
    |   +-- WalletService[Impl].java   # 2% commission, pessimistic lock on balance
    |
    +-- email/
    |   +-- EmailService[Impl].java    # async smtp, sends on invoice/payment events
    |
    +-- idempotency/
        +-- IdempotencyLog.java        # key, request hash, cached response
        +-- IdempotencyFilter.java     # POST/PUT/PATCH dedup via X-Idempotency-Key
        +-- RateLimitFilter.java       # token bucket, 30 req/min per IP
```

---

## API endpoints

### Public (no auth)

```
POST   /api/tenants                         create workspace
GET    /api/tenants/{slug}                  lookup by slug
POST   /api/auth/register                   register user
POST   /api/auth/login                      login, returns JWT

GET    /api/public/invoices/{id}            invoice details (SENT/OVERDUE only)
POST   /api/public/payments/initiate        create razorpay order
POST   /api/public/payments/verify          verify payment signature

POST   /api/webhooks/razorpay               razorpay event handler
```

### Private (JWT required)

```
Customers:
  POST   /api/customers                     create
  GET    /api/customers                     list (paginated)
  GET    /api/customers/{id}               get
  PUT    /api/customers/{id}               update
  DELETE /api/customers/{id}               delete

Invoices:
  POST   /api/invoices                      create with line items
  GET    /api/invoices                      list (optional ?status=SENT)
  GET    /api/invoices/{id}                get
  GET    /api/invoices/number/{num}         get by invoice number
  PATCH  /api/invoices/{id}/status          transition status
  GET    /api/invoices/{id}/pdf             download PDF

Payments:
  POST   /api/payments/initiate             create razorpay order
  POST   /api/payments/verify               verify signature
  GET    /api/payments/{id}                get
  GET    /api/payments/invoice/{id}         list by invoice

Wallet:
  GET    /api/wallet/balance                balance + earnings + withdrawn
  POST   /api/wallet/withdraw               request payout (bank or UPI)
  GET    /api/wallet/payouts                payout history (paginated)
```

---

## Database

9 Flyway migrations, all incremental:

```
V1  — uuid-ossp extension
V2  — tenants (name, slug, status)
V3  — users (email, password_hash, role, tenant FK)
V4  — customers (name, email, phone, address — PII encrypted at rest)
V5  — invoices (number, status, dates, amounts, customer FK)
V6  — invoice_items (description, qty, unit_price, tax_rate, line_total)
V7  — payments (razorpay_order_id, razorpay_payment_id, status)
V8  — idempotency_logs (key, request_hash, cached response, 24h TTL)
V9  — tenant_wallets + payout_requests
```

Every table has: `id` (UUID PK), `tenant_id` (FK), `created_at`, `updated_at`, `version` (optimistic locking).

---

## Multi-tenancy

Row-level isolation using `tenant_id` on every table.

```
Request comes in
    |
    v
JwtAuthenticationFilter extracts tenantId from token claims
    |
    v
TenantContext.set(tenantId)  ← ThreadLocal
    |
    v
Service layer queries filter by tenantId
    |
    v
TenantEntityListener auto-sets tenantId on @PrePersist
```

No shared data between tenants. Public endpoints (customer payment page) manually set/clear TenantContext since there's no JWT.

---

## Payment flow

```
1. Owner creates invoice, marks it SENT
2. Owner copies payment link:  /pay/{invoiceId}
3. Customer opens link, sees invoice details
4. Customer clicks Pay:
     frontend  -->  POST /api/public/payments/initiate
                         |
                         v
                    Razorpay order created
                    Payment row saved (INITIATED)
                         |
                         v
                    Razorpay checkout opens
                         |
                         v
                    Customer pays
                         |
                         v
     frontend  -->  POST /api/public/payments/verify
                         |
                         v
                    Signature verified (HMAC-SHA256)
                    Payment marked CAPTURED
                    Invoice marked PAID
                    Wallet credited (amount - 2% commission)
                    Email sent to customer

5. Owner sees updated balance in wallet
6. Owner requests withdrawal → bank or UPI
```

Razorpay webhooks (`POST /api/webhooks/razorpay`) handle `payment.captured` and `payment.failed` as a fallback if the frontend verify call doesn't reach the server.

---

## Security

- **JWT** — HMAC-SHA256, 24h expiry, carries userId + tenantId + role
- **PII encryption** — customer email, phone, address encrypted with AES-256-GCM before storing in DB
- **Idempotency** — `X-Idempotency-Key` header on POST/PUT/PATCH, SHA-256 body hash, 24h dedup window
- **Rate limiting** — token bucket, 30 req/min per IP on auth endpoints
- **Razorpay signature verification** — HMAC-SHA256 on order_id + payment_id + signature
- **Optimistic locking** — `@Version` on all entities
- **Pessimistic locking** — `SELECT ... FOR UPDATE` on wallet balance during credit/withdrawal

---

## Resilience

Razorpay API calls are wrapped with:

- **CircuitBreaker** — trips after 50% failures in a 10-call window, waits 30s before half-open
- **Retry** — 3 attempts with 1s wait between retries

Configured via Resilience4j annotations (`@CircuitBreaker`, `@Retry`).

---

## Running locally

**Prerequisites:** Docker, Java 21, Maven 3.9+

```bash
# start postgres + redis
docker-compose up -d

# run the app
RAZORPAY_KEY_ID=rzp_test_xxx \
RAZORPAY_KEY_SECRET=xxx \
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

App starts on `http://localhost:8080`. Swagger UI at `/swagger-ui.html`.

### Environment variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/paykit` | database connection |
| `DB_USERNAME` | no | `paykit` | db user |
| `DB_PASSWORD` | no | `paykit_secret` | db password |
| `REDIS_HOST` | no | `localhost` | redis host |
| `REDIS_PORT` | no | `6379` | redis port |
| `ENCRYPTION_SECRET_KEY` | no | set in dev profile | AES-256 key (Base64) |
| `JWT_SECRET` | prod | hardcoded in dev | HMAC signing key |
| `RAZORPAY_KEY_ID` | yes | — | razorpay api key |
| `RAZORPAY_KEY_SECRET` | yes | — | razorpay secret |
| `RAZORPAY_WEBHOOK_SECRET` | no | — | webhook signature key |
| `CORS_ORIGINS` | no | `localhost:3000,5173,5174` | allowed frontend origins |
| `MAIL_USERNAME` | no | — | smtp username |
| `MAIL_PASSWORD` | no | — | smtp password |
| `SERVER_PORT` | no | `8080` | http port |

---

## Docker

```bash
# build image
docker build -t paykit .

# run
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/paykit \
  -e DB_USERNAME=paykit \
  -e DB_PASSWORD=secret \
  -e RAZORPAY_KEY_ID=rzp_xxx \
  -e RAZORPAY_KEY_SECRET=xxx \
  paykit
```

Multi-stage build: Maven + JDK 21 for compilation, JRE 21 for runtime. JVM limited to 280MB heap for container environments.

---

## Monitoring

- **Health check:** `GET /actuator/health` (includes Razorpay connectivity)
- **Metrics:** `GET /actuator/prometheus` (payment timing, request counts)
- **Request logging:** every request logged with request-id, method, path, status, duration
- **Flyway:** `GET /actuator/flyway` (migration status)
