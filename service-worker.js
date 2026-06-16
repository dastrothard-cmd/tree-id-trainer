const CACHE_NAME="tree-id-trainer-hosted-v8";
const CORE=[
  "/",
  "/index.html",
  "/app.webmanifest",
  "/icons/icon-192.png",
  "/icons/icon-512.png","/icons/favicon-32.png","/icons/favicon-64.png","/icons/apple-touch-icon.png","/header-art.png",
  "/theme-assets/mystical-header.png","/theme-assets/mystical-logo.png",
  "/theme-assets/journal-header.png","/theme-assets/journal-logo.png",
  "/theme-assets/moonlit-header.png","/theme-assets/moonlit-logo.png",
  "/theme-assets/reddirt-header.png","/theme-assets/reddirt-logo.png"
];

self.addEventListener("install",event=>{
  event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.addAll(CORE)));
  self.skipWaiting();
});

self.addEventListener("activate",event=>{
  event.waitUntil(
    caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE_NAME).map(k=>caches.delete(k))))
  );
  self.clients.claim();
});

self.addEventListener("fetch",event=>{
  const request=event.request;
  const url=new URL(request.url);

  if(url.pathname.startsWith("/api/"))return;

  if(request.mode==="navigate"){
    event.respondWith(
      fetch(request)
        .then(response=>{
          const copy=response.clone();
          caches.open(CACHE_NAME).then(cache=>cache.put("/index.html",copy));
          return response;
        })
        .catch(()=>caches.match("/index.html"))
    );
    return;
  }

  event.respondWith(
    caches.match(request).then(cached=>{
      if(cached)return cached;
      return fetch(request).then(response=>{
        if(request.method==="GET" && response.ok){
          const copy=response.clone();
          caches.open(CACHE_NAME).then(cache=>cache.put(request,copy));
        }
        return response;
      });
    })
  );
});
