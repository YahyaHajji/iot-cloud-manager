# Backend : étapes pour s'aligner sur le contrat MQTT v1

Pour Nisrine. Référence : `CONTRAT_MQTT.md` (sections 3, 4, 6, 7, 10).

1. **Mettre à jour le dépôt.** `git pull` sur `master`, lire les sections 3, 4, 6, 7 et 10 du contrat.
2. **Corriger l'entité Device.**
   - Champs : `id`, `deviceId` (unique), `name`, `status` (ONLINE, OFFLINE, UNKNOWN), `enabled`, `intervalSec` (5 par défaut), `lastSeenAt`.
   - Renommer `active` en `enabled` ; retirer `type`, `unit`, `minValue`, `maxValue`.
   - Valider `deviceId` avec `^[a-z0-9-]{3,50}$`.
3. **Ajouter le catalogue des types de mesure.** Table `measurement_type` (`type`, `unit`, `min`, `max`) remplie avec les 6 types du contrat : temperature, humidity, pressure, co2, water_volume, presence. Ajouter un type = ajouter une ligne.
4. **Ajouter l'entité Measurement.** `id`, `device` (clé étrangère), `messageId`, `type`, `value`, `unit`, `measuredAt`, `receivedAt`, avec la contrainte `UNIQUE (message_id, type)`.
5. **Adapter l'API REST.** `GET` et `POST /api/devices`, puis `GET /{id}`, `PUT`, `DELETE`. Réponse 400 si `deviceId` invalide, 409 si doublon. JSON : `deviceId`, `name`, `enabled`, `status`, `intervalSec`, `lastSeenAt`. Format à valider avec Hamza.
6. **Configurer MQTT.** Client ID `iot-backend-1`, `cleanSession=false`, reconnexion automatique. Abonnements QoS 1 : `iot/devices/+/telemetry` et `iot/devices/+/status` (plus tard `.../commands/ack`). Se réabonner après chaque reconnexion.
7. **Écrire le traitement des messages.**
   - Le `deviceId` vient du topic ; ignorer les champs inconnus (`FAIL_ON_UNKNOWN_PROPERTIES=false`).
   - Les 7 contrôles du contrat, dans l'ordre : JSON et taille, `schemaVersion`, `deviceId` égal au topic, capteur existant et actif, champs obligatoires, type, unité et plage, `messageId` déjà vu.
   - Un rejet : log `WARN` et compteur `mqtt_messages_rejected_total{reason=...}`. Un doublon est ignoré sans erreur.
8. **Traiter les messages d'état.** Mettre à jour `status`, `intervalSec`, `enabled`, `lastSeenAt`. Le Last Will n'a pas de `timestamp` : utiliser l'heure de réception. Chaque télémétrie rafraîchit aussi `lastSeenAt`.
9. **Configuration par variables d'environnement.** URL, utilisateur et mot de passe de la base, `MQTT_BROKER_URL`, `MQTT_CLIENT_ID`, avec des valeurs par défaut locales, par exemple `mqtt.broker-url=${MQTT_BROKER_URL:tcp://localhost:1883}`. Le fichier `deploy/docker-compose.yml` envoie exactement ces noms.
10. **Tester.** Tests unitaires avec les messages d'exemple du contrat : valide, doublon, capteur inconnu, valeur hors plage, `deviceId` différent du topic. Puis un `mosquitto_pub` réel et vérification dans `GET /api/devices` et la base.
11. **Pousser sur une branche de fonctionnalité.** `mvnw`, `.mvn` et `pom.xml` doivent être dans `backend/`, et `./mvnw package` doit produire un jar. Prévenir Yahya pour fusionner le Docker Compose et tester sur la VM.
12. **Mettre à jour Trello** au fur et à mesure. La carte « réception MQTT » est terminée quand l'étape 10 passe.
