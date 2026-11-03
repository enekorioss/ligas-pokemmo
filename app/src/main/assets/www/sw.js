const CACHE = 'farmligas-v98';
const ASSETS = [
  './',
  './index.html',
  './assets/index-BmLHS4fQ.js',
  './assets/index-CjIbjzbu.css',
  './assets/anis-Dlo9nCVq.js',
  './images/lideres/agatha.png',
  './images/lideres/alecran.png',
  './images/lideres/anis.png',
  './images/lideres/aza.png',
  './images/lideres/blue.png',
  './images/lideres/bruno.png',
  './images/lideres/catleya.png',
  './images/lideres/cynthia.png',
  './images/lideres/delos.png',
  './images/lideres/dracon.png',
  './images/lideres/fatima.png',
  './images/lideres/fausto.png',
  './images/lideres/gaia.png',
  './images/lideres/karen.png',
  './images/lideres/koga.png',
  './images/lideres/lance.png',
  './images/lideres/lorelei.png',
  './images/lideres/lotto.png',
  './images/lideres/mirto.png',
  './images/lideres/nivea.png',
  './images/lideres/plubio.png',
  './images/lideres/sixto.png',
  './images/lideres/will.png'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (url.origin !== location.origin) return;
  if (e.request.method !== 'GET') return;

  // HTML: network first (para que los cambios se vean siempre)
  if (e.request.mode === 'navigate' || url.pathname === '/' || url.pathname.endsWith('.html')) {
    e.respondWith(
      fetch(e.request).then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(e.request, copy));
        return res;
      }).catch(() => caches.match(e.request))
    );
    return;
  }

  // Solo cachear assets estáticos propios
  if (/\.(js|css|png|webp|jpg|jpeg|ico|svg|woff2?)$/i.test(url.pathname)) {
    e.respondWith(
      caches.match(e.request).then((cached) => {
        if (cached) return cached;
        return fetch(e.request).then((res) => {
          if (res && res.status === 200) {
            const copy = res.clone();
            caches.open(CACHE).then((c) => c.put(e.request, copy));
          }
          return res;
        });
      })
    );
  }
  // el resto (Firebase Auth, Firestore, etc.) pasa sin cachear
});
