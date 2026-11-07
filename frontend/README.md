# IoT Cloud Manager — frontend

Interface React/Vite en français pour superviser le parc de capteurs IoT
.Le frontend utilise les types, unités, plages, statuts et la forme de télémétrie de `../CONTRAT_MQTT.md`.

## Installation et lancement

```sh
cd frontend
npm install
Copy-Item .env.example .env
npm run dev
```
Scripts disponibles : `npm run dev`, `npm run build`, `npm run lint`, `npm test`, `npm run preview`.

## Routes

- `/dashboard` — indicateurs calculés, historique, états, alertes et mesures récentes.
- `/capteurs` — inventaire paginé, recherche et filtres, ajout et modification.
- `/appareils` — informations techniques des mêmes appareils.
- `/alertes` — alertes d’exemple valides selon le catalogue MQTT.
- `/donnees` — historique filtrable et graphique par grandeur (une unité par axe).
- Toute route inconnue affiche la page 404.

## Contrat REST attendu

```text
GET    /api/devices
GET    /api/devices/{deviceId}
POST   /api/devices
PUT    /api/devices/{deviceId}
```

## Vérifications

`npm test` couvre la validation du formulaire et les opérations mock/API du service (5 tests). `npm run lint` et `npm run build` passent. Le build signale un bundle JavaScript supérieur à 500 kB, principalement dû aux graphiques Recharts. Les cinq chemins applicatifs et une route inconnue ont répondu via le serveur Vite. L’API Spring Boot et la persistance PostgreSQL n’ont pas été vérifiées, car aucun backend n’est présent dans le workspace.
