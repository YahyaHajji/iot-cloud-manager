# IoT Cloud Manager

Système de gestion des objets IoT basé sur MQTT et le Cloud.

Des capteurs simulés publient leurs mesures sur un broker MQTT. Le backend les reçoit, les enregistre dans une base de données et les expose à un dashboard web. L'ensemble tourne dans des conteneurs Docker sur une machine virtuelle cloud.

## Structure du dépôt

| Dossier | Contenu | Technologies |
|---|---|---|
| `simulator/` | Capteurs simulés qui publient sur MQTT | Python |
| `backend/` | API REST, réception MQTT, base de données | Spring Boot, PostgreSQL |
| `frontend/` | Dashboard web | Angular |
| `deploy/` | Docker Compose, configuration des services, déploiement | Docker, Mosquitto |

## Démarrage rapide (Mosquitto et PostgreSQL en local)

```bash
cd deploy
cp .env.example .env      # puis modifier les valeurs
docker compose up -d
```

Test du broker (dans deux terminaux) :

```bash
mosquitto_sub -h localhost -t "iot/devices/+/telemetry"
mosquitto_pub -h localhost -t "iot/devices/sensor-room-01/telemetry" -m '{"deviceId":"sensor-room-01","temperature":24.5,"humidity":52.0}'
```

## Règles de travail

- Une branche par fonctionnalité (`feature/nom-court`), jamais de push direct sur `master`.
- Une pull request relue par au moins un autre membre avant la fusion.
- Ne jamais déposer de mots de passe, clés SSH ou fichiers `.env` dans le dépôt.
- Le suivi des tâches se fait sur le tableau Trello de l'équipe.

## Équipe

| Membre | Rôle |
|---|---|
| Membre 1 | IoT / MQTT |
| Membre 2 | Backend |
| Membre 3 | Frontend |
| Membre 4 | Cloud / DevOps |
