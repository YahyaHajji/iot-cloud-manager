# IoT Cloud Manager — frontend

Interface React/Vite en français pour superviser le parc de capteurs IoT. Le frontend utilise les types, unités, plages, statuts et la forme de télémétrie de `../CONTRAT_MQTT.md`. Le navigateur communique uniquement avec Spring Boot en mode API; il ne se connecte ni à Mosquitto ni à PostgreSQL.

## Installation et lancement

```sh
cd frontend
npm install
Copy-Item .env.example .env
npm run dev
```

Scripts disponibles : `npm run dev`, `npm run build`, `npm run lint`, `npm test`, `npm run preview`.

Le mode par défaut est `mock`. Les 24 appareils, mesures, alertes et CRUD simulé sont chargés en mémoire pour la session navigateur. Pour utiliser le backend, configurez `VITE_DATA_MODE=api` et `VITE_API_BASE_URL=http://localhost:8080` dans `.env`, puis redémarrez Vite. En mode API, une erreur réseau reste une erreur; l'application ne revient pas silencieusement aux mocks.

## Routes

- `/dashboard` — indicateurs calculés, historique, états, alertes et mesures récentes.
- `/capteurs` — inventaire paginé, recherche et filtres, ajout et modification.
- `/appareils` — informations techniques des mêmes appareils.
- `/alertes` — alertes d’exemple valides selon le catalogue MQTT.
- `/donnees` — historique filtrable et graphique par grandeur (une unité par axe).
- Toute route inconnue affiche la page 404.

## Contrat REST attendu

Aucun contrôleur Spring Boot ni document OpenAPI n’est présent dans le workspace. Les routes suivantes sont donc une proposition frontend à confirmer avec l’équipe backend, pas un contrat serveur vérifié :

```text
GET    /api/devices
GET    /api/devices/{deviceId}
POST   /api/devices
PUT    /api/devices/{deviceId}
```

Le client accepte une liste de DTO sous forme de tableau; `deviceId` (ou `id`) sert d’identifiant. L’adaptateur normalise les champs UI optionnels `name`, `location`, `description` et `measurementTypes` (ou `types`). Le POST envoie `deviceId` et les champs du formulaire; le PUT envoie les champs modifiables et conserve l’identifiant dans le chemin. Les formes exactes de requête et de réponse, ainsi que la persistance de ces métadonnées, nécessitent confirmation du backend. Les erreurs HTTP usuelles et les erreurs réseau/timeouts sont présentées dans l’interface.

## Jeux de données

Les 24 identifiants sont uniques et respectent `^[a-z0-9-]{3,50}$`. Les mesures suivent le catalogue MQTT et sont émises dans `measurements[]` avec UUID v4 par message. `enabled` et `status` sont affichés séparément; l’état d’avertissement est calculé côté UI à partir des alertes, jamais utilisé comme statut MQTT.

## Vérifications

`npm test` couvre la validation du formulaire et les opérations mock/API du service (5 tests). `npm run lint` et `npm run build` passent. Le build signale un bundle JavaScript supérieur à 500 kB, principalement dû aux graphiques Recharts. Les cinq chemins applicatifs et une route inconnue ont répondu via le serveur Vite. L’API Spring Boot et la persistance PostgreSQL n’ont pas été vérifiées, car aucun backend n’est présent dans le workspace.
