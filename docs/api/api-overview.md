# API Specification & Contracts Overview — CloudScale

## 1. Protocol & Conventions
- **Base URI**: `/api/v1`
- **Transport**: HTTPS (or HTTP in local development)
- **Data Format**: `application/json` (UTF-8)
- **Authentication**: `Authorization: Bearer <RS256-JWT>`
- **Date/Time Format**: ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ss.SSSZ`)

## 2. Standard Response Envelope

All API endpoints return a standardized envelope structure:

### Successful Response
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "timestamp": "2026-11-05T11:45:00.123Z",
    "requestId": "e4b9c1d2-7f3a-4a2b-9e1a-8c7d6e5f4a3b",
    "version": "v1"
  }
}
```

### Paginated Response
```json
{
  "success": true,
  "data": [ ... ],
  "error": null,
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "isFirst": true,
    "isLast": false
  },
  "meta": {
    "timestamp": "2026-11-05T11:45:00.123Z",
    "requestId": "e4b9c1d2-7f3a-4a2b-9e1a-8c7d6e5f4a3b",
    "version": "v1"
  }
}
```

### Error Response
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_NODE_CAPACITY",
    "message": "No eligible active nodes possess 4 CPU cores and 8192 MB memory.",
    "details": {
      "requestedCpu": 4,
      "requestedMemoryMb": 8192,
      "bestAvailableCpu": 2,
      "bestAvailableMemoryMb": 4096
    }
  },
  "meta": {
    "timestamp": "2026-11-05T11:45:00.123Z",
    "requestId": "e4b9c1d2-7f3a-4a2b-9e1a-8c7d6e5f4a3b",
    "version": "v1"
  }
}
```

## 3. Standard HTTP Status Codes
| Status | Name | Meaning |
|---|---|---|
| `200` | OK | Successful operation returning entity or list |
| `201` | Created | Resource successfully created |
| `204` | No Content | Successful operation with empty body (e.g. DELETE) |
| `400` | Bad Request | Validation failure or malformed payload |
| `401` | Unauthorized | Missing or expired JWT token |
| `403` | Forbidden | Insufficient role or cross-tenant access violation |
| `404` | Not Found | Entity not found within tenant boundary |
| `409` | Conflict | State transition conflict or optimistic locking failure |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Unhandled backend exception |
| `503` | Service Unavailable | Downstream dependency failure (e.g. AI Service) |
