# SSO Integration Guide

This guide is for developers who want to add login to their app using this SSO service.  
You do **not** need to build your own login page. This SSO service handles it for you.

---

## How it works (big picture)

```
1. User opens your app
2. Your app checks: does the user have a valid JWT token?
   - NO  → redirect user to SSO login page
   - YES → let user in (verify token with /validate)
3. User logs in on SSO (one time)
4. SSO gives back a JWT token
5. Your app stores that token
6. Use the token for all future requests
```

---

## SSO Service Details

| Item             | Value                                      |
|------------------|--------------------------------------------|
| Login page       | `http://localhost:8080/login`              |
| Validate token   | `GET http://localhost:8080/validate`       |
| Logout           | `http://localhost:8080/logout`             |
| Token lifetime   | 1 hour                                     |
| Token location   | Returned as cookie `JWT-TOKEN` after login |

> In production, replace `http://localhost:8080` with the deployed SSO URL.

---

## Step-by-step for a React / JavaScript App

### Step 1 — Copy the helper file
Copy `sso-auth.js` from this folder into your project (e.g. `src/utils/sso-auth.js`).

### Step 2 — Protect your app at startup
In your main entry file (`App.jsx`, `index.js`, etc.):

```js
import { SSOAuth } from './utils/sso-auth';

const auth = new SSOAuth({ ssoBaseUrl: 'http://localhost:8080' });

// Call this before rendering anything
await auth.requireLogin();
// If the user is not logged in, they will be automatically sent to the SSO login page.
// This line only continues if the user IS logged in.

console.log('Logged in as:', auth.getUsername());
```

### Step 3 — Show username anywhere in your app

```js
const username = auth.getUsername(); // returns "naman", "john", etc.
```

### Step 4 — Add a logout button

```js
auth.logout(); // clears token and redirects to SSO logout
```

### Step 5 — Send the token with your API calls
If your app calls a backend API, include the JWT token in every request:

```js
const token = auth.getToken();

const res = await fetch('http://your-api.com/data', {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

---

## Step-by-step for a Spring Boot App

### Step 1 — Copy the filter file
Copy `SsoAuthFilter.java` from this folder into your project under:
```
src/main/java/com/yourapp/security/SsoAuthFilter.java
```
Update the package name at the top of the file to match your project.

### Step 2 — Wire the filter into Spring Security
In your `SecurityConfig.java`:

```java
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

http.addFilterBefore(new SsoAuthFilter(), UsernamePasswordAuthenticationFilter.class);
```

### Step 3 — Read the logged-in username in your controllers

```java
@GetMapping("/dashboard")
public String dashboard(HttpServletRequest request) {
    String username = (String) request.getAttribute("sso_username");
    return "Hello " + username;
}
```

### Step 4 — Your frontend must send the token in every API call
```
GET /dashboard
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

## Step-by-step for any other backend (Node.js, Python, etc.)

Your backend just needs to call the SSO `/validate` endpoint before processing any request.

### Node.js (Express) example

```js
async function ssoMiddleware(req, res, next) {
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing token' });
  }

  const token = authHeader.split(' ')[1];

  const ssoRes = await fetch('http://localhost:8080/validate', {
    headers: { Authorization: `Bearer ${token}` }
  });
  const data = await ssoRes.json();

  if (!data.valid) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }

  req.username = data.username; // available in all your route handlers
  next();
}

// Use it on any protected route
app.get('/dashboard', ssoMiddleware, (req, res) => {
  res.json({ message: `Hello ${req.username}` });
});
```

### Python (Flask) example

```python
import requests
from functools import wraps
from flask import request, jsonify

def sso_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.headers.get('Authorization', '')
        if not auth.startswith('Bearer '):
            return jsonify({'error': 'Missing token'}), 401

        token = auth.split(' ')[1]
        res = requests.get(
            'http://localhost:8080/validate',
            headers={'Authorization': f'Bearer {token}'}
        )
        data = res.json()

        if not data.get('valid'):
            return jsonify({'error': 'Invalid or expired token'}), 401

        request.username = data['username']
        return f(*args, **kwargs)
    return decorated

# Use it on any protected route
@app.route('/dashboard')
@sso_required
def dashboard():
    return jsonify({'message': f'Hello {request.username}'})
```

---

## How to get the JWT token (for testing with Postman or curl)

1. Open `http://localhost:8080/login` in your browser
2. Login with username `naman`, password `naman@sso123`
3. Open browser DevTools → Application → Cookies → copy `JWT-TOKEN`
4. Use it in Postman or curl:

```bash
curl -H "Authorization: Bearer <paste-token-here>" http://localhost:8080/validate
```

Expected response:
```json
{ "valid": true, "username": "naman" }
```

---

## Quick checklist before going live

- [ ] Replace `http://localhost:8080` with your production SSO URL everywhere
- [ ] Change the JWT secret in `application.properties` to a strong random string
- [ ] Add your username (or your team's usernames) to `UserDetailsServiceImpl.java`
- [ ] Enable HTTPS on the SSO service so tokens are not sent in plain text
- [ ] Set cookie `SameSite` and `Secure` flags if using cookie-based token passing
