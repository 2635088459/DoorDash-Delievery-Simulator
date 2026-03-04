# Deploy `livefood` to Bluehost

This guide deploys the Vite app under `https://haoyuanshan.com/livefood`.

## 1) Build the frontend

```bash
cd /Users/aaronshan2635088459/Desktop/DoorDash/frontend
npm run build
```

The production files are generated in `frontend/dist/`.

## 2) Upload to Bluehost

1. Open **Bluehost → cPanel → File Manager**.
2. Go to `public_html/`.
3. Create a folder named `livefood`.
4. Upload **all** files inside `frontend/dist/` into `public_html/livefood/`.

## 3) Add SPA routing (required)

Create a file named `.htaccess` inside `public_html/livefood/` with:

```
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /livefood/
  RewriteRule ^index\.html$ - [L]
  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /livefood/index.html [L]
</IfModule>
```

## 4) Confirm the site

Open `https://haoyuanshan.com/livefood` in your browser.

## 5) Configure the backend API base URL

The frontend calls `/api/*` by default, which will 404 unless you have a backend proxy on the same domain.

**Recommended:** set a production API base URL before building:

```bash
export VITE_API_BASE_URL="https://YOUR-BACKEND-DOMAIN"
npm run build
```

If you use a `.env.production` file, add:

```
VITE_API_BASE_URL=https://YOUR-BACKEND-DOMAIN
```

Then rebuild and re-upload the `dist/` folder.

## Notes

- If you later change the subpath, update `base` in `vite.config.js` and rebuild.
- The backend API base should be reachable from the public domain (e.g., via a reverse proxy or public API domain).
