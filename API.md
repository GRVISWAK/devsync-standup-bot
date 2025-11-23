## API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication
Currently, the API doesn't require authentication. For production, implement JWT or OAuth2.

---

## Standup Endpoints

### 1. Start New Standup

**Endpoint:** `POST /api/standup/start`

**Description:** Initiates a new standup session for a user.

**Request Body:**
```json
{
  "zohoUserId": "12345",
  "userEmail": "john.doe@example.com",
  "userName": "John Doe"
}
```

**Response:**
```json
{
  "standupId": 1,
  "userName": "John Doe",
  "standupDate": "2025-11-22",
  "currentStep": 1,
  "status": "IN_PROGRESS",
  "nextQuestion": "What did you work on yesterday?"
}
```

---

### 2. Submit Standup Response

**Endpoint:** `POST /api/standup/submit`

**Description:** Submit answer for current standup step.

**Request Body:**
```json
{
  "userEmail": "john.doe@example.com",
  "response": "Worked on the authentication module and fixed bugs"
}
```

**Response:**
```json
{
  "standupId": 1,
  "userName": "John Doe",
  "standupDate": "2025-11-22",
  "currentStep": 2,
  "status": "IN_PROGRESS",
  "nextQuestion": "What are you planning to work on today?"
}
```

---

### 3. Get User's Standups

**Endpoint:** `GET /api/standup/user/{email}?limit=5`

**Description:** Retrieve recent standups for a user.

**Response:**
```json
[
  {
    "standupId": 1,
    "userName": "John Doe",
    "standupDate": "2025-11-22",
    "yesterdayWork": "Worked on authentication",
    "todayPlan": "Implement user dashboard",
    "blockers": "None",
    "aiSummary": "✅ Completed authentication module...",
    "githubCommits": ["repo/project: Fixed login bug", "repo/project: Added JWT support"],
    "jiraTasks": ["PROJ-123: Implement OAuth - In Progress"],
    "calendarEvents": [],
    "status": "COMPLETED",
    "currentStep": 4
  }
]
```

---

### 4. Get Standup by Date

**Endpoint:** `GET /api/standup/user/{email}/date/{date}`

**Description:** Get standup for specific user and date.

**Example:** `GET /api/standup/user/john.doe@example.com/date/2025-11-22`

**Response:**
```json
{
  "standupId": 1,
  "userName": "John Doe",
  "standupDate": "2025-11-22",
  "yesterdayWork": "Worked on authentication",
  "todayPlan": "Implement user dashboard",
  "blockers": "None",
  "status": "COMPLETED"
}
```

---

### 5. Get All Standups by Date

**Endpoint:** `GET /api/standup/date/{date}`

**Description:** Get all completed standups for a specific date.

**Example:** `GET /api/standup/date/2025-11-22`

**Response:**
```json
[
  {
    "standupId": 1,
    "userName": "John Doe",
    "standupDate": "2025-11-22",
    "aiSummary": "..."
  },
  {
    "standupId": 2,
    "userName": "Jane Smith",
    "standupDate": "2025-11-22",
    "aiSummary": "..."
  }
]
```

---

### 6. Health Check

**Endpoint:** `GET /api/standup/health`

**Description:** Check application health status.

**Response:**
```json
{
  "status": "UP",
  "service": "DevSync Standup Bot",
  "timestamp": "2025-11-22T10:30:00"
}
```

---

## Zoho Cliq Webhook Endpoints

### 1. Handle Slash Commands

**Endpoint:** `POST /api/webhook/cliq/command`

**Description:** Handle Zoho Cliq slash commands like `/standup`.

**Request Body (from Zoho Cliq):**
```json
{
  "name": "John Doe",
  "user": "12345",
  "text": "/standup now",
  "command": "/standup"
}
```

**Response:**
```json
{
  "text": "🚀 **Daily Standup Started!**\n\n**Step 1/3**\n\nWhat did you work on yesterday?\n\n_Reply with your answer to continue..._"
}
```

---

### 2. Handle Messages

**Endpoint:** `POST /api/webhook/cliq/message`

**Description:** Handle user responses in conversation.

**Request Body:**
```json
{
  "text": "Worked on the API integration",
  "user": {
    "id": "12345",
    "email": "john.doe@example.com",
    "name": "John Doe"
  }
}
```

**Response:**
```json
{
  "text": "**Step 2/3**\n\nWhat are you planning to work on today?"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-22T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-22T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-11-22T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Rate Limiting

Currently not implemented. Recommended for production:
- 100 requests per minute per user
- 1000 requests per hour per IP

---

## Webhook Security

### Validating Zoho Cliq Webhooks

Zoho Cliq sends webhooks with signatures. To validate:

```java
// TODO: Implement signature validation
String signature = request.getHeader("X-Zoho-Cliq-Signature");
// Validate signature using bot token
```

---

## Examples

### cURL Examples

**Start Standup:**
```bash
curl -X POST http://localhost:8080/api/standup/start \
  -H "Content-Type: application/json" \
  -d '{
    "zohoUserId": "12345",
    "userEmail": "john@example.com",
    "userName": "John Doe"
  }'
```

**Submit Response:**
```bash
curl -X POST http://localhost:8080/api/standup/submit \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "john@example.com",
    "response": "Worked on authentication module"
  }'
```

**Get User Standups:**
```bash
curl http://localhost:8080/api/standup/user/john@example.com?limit=5
```

### JavaScript/Axios Examples

```javascript
// Start standup
const response = await axios.post('http://localhost:8080/api/standup/start', {
  zohoUserId: '12345',
  userEmail: 'john@example.com',
  userName: 'John Doe'
});

// Submit response
await axios.post('http://localhost:8080/api/standup/submit', {
  userEmail: 'john@example.com',
  response: 'Worked on authentication module'
});

// Get user standups
const standups = await axios.get('http://localhost:8080/api/standup/user/john@example.com?limit=5');
```

---

## Postman Collection

Import this collection to test the API:

```json
{
  "info": {
    "name": "DevSync Standup Bot API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Start Standup",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/standup/start",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"zohoUserId\": \"12345\",\n  \"userEmail\": \"john@example.com\",\n  \"userName\": \"John Doe\"\n}"
        }
      }
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    }
  ]
}
```
