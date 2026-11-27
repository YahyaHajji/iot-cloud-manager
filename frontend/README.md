# IoT Cloud Manager — frontend

Interface React/Vite en français pour superviser un parc de capteurs IoT.

## Installation

```sh
cd frontend
npm install
```

`frontend/.env.example` garde `VITE_DATA_MODE=api` et `VITE_API_BASE_URL=http://localhost:8080`. Copiez-le vers `.env`, puis choisissez le mode voulu avant de démarrer Vite.

### Mode API (par défaut)

```env
VITE_DATA_MODE=api
VITE_API_BASE_URL=http://localhost:8080
```

Démarrez le Backend Spring Boot, puis lancez le frontend :

```sh
npm run dev
```

Connectez-vous avec un compte enregistré dans le Backend. Les appels API utilisent le jeton JWT de cette session.

### Mode démonstration sans Backend

Dans `.env`, définissez :

```env
VITE_DATA_MODE=mock
```

Puis démarrez `npm run dev`. À l’écran de connexion, `demo` comme nom d’utilisateur et `demo` comme mot de passe fonctionnent ; en mode mock, n’importe quelles valeurs non vides conviennent. Les capteurs, mesures et alertes sont simulés localement, et les opérations de capteurs modifient l’état en mémoire. Ce mode n’envoie aucune requête au Backend.

Le mode est choisi au démarrage/à la compilation : après toute modification de `VITE_DATA_MODE`, redémarrez Vite. Pour une version compilée, relancez `npm run build` et redéployez les fichiers générés.

Le mode API est la valeur par défaut. Une erreur réseau ou Backend ne bascule jamais automatiquement l’application en mode mock.

## Routes

- `/dashboard` — statistiques, historique des mesures, état des capteurs, alertes et dernières mesures.
- `/capteurs` — inventaire, recherche et filtres, ajout, modification, suppression et activation.
- `/appareils` — informations techniques des capteurs.
- `/alertes` — historique des alertes.
- `/donnees` — historique filtrable et graphiques par grandeur.
- Toute route inconnue affiche la page 404.

## Scripts

```sh
npm run dev
npm run lint
npm run build
npm test
npm run preview
```
