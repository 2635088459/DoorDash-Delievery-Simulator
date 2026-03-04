# Root .htaccess for /livefood bypass

Place this in `public_html/.htaccess` (root), above any existing rewrite rules (WordPress, etc.).

```
<IfModule mod_rewrite.c>
  RewriteEngine On
  # Allow /livefood/* to pass through without being rewritten by the root site
  RewriteRule ^livefood/ - [L]

  # Keep your existing root rewrite rules below this line
</IfModule>
```

If you already have a root `.htaccess`, add only the `RewriteRule ^livefood/ - [L]` line near the top (before other RewriteRule entries).
