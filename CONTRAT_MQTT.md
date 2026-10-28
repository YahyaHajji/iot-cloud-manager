# Contrat des messages MQTT – IoT Cloud Manager

**Version du contrat :** 1 (proposition du 28 octobre, à valider par l'équipe)
**Concerne :** Membre 1 (simulateurs Python), Membre 2 (backend Spring Boot), Membre 4 (configuration Mosquitto / ACL)
Le frontend React ne parle jamais MQTT : il passe uniquement par l'API REST du backend.

---

## 1. Règles générales

| Règle | Valeur |
|---|---|
| Broker | Mosquitto, port 1883 (TLS 8883 en bonus) |
| Format des messages | JSON, encodé en UTF-8 |
| Taille maximale d'un message | 1 Ko |
| Horodatage | ISO 8601 en UTC avec `Z` : `2026-10-28T10:15:30Z` |
| Nombres | Toujours des nombres JSON (`24.5`), jamais des chaînes (`"24.5"`) |
| Version | Chaque message contient `"schemaVersion": 1` |
| Champs inconnus | Ignorés par le récepteur (permet d'ajouter des champs sans tout casser) |
| Noms des champs | camelCase en anglais (`deviceId`, `intervalSec`) |

### Format du `deviceId`

- Expression régulière : `^[a-z0-9-]{3,50}$` (minuscules, chiffres, tirets)
- Exemples : `sensor-room-01`, `water-meter-02`
- Le `deviceId` dans le topic **doit être identique** à celui du message.
- Le `deviceId` sert aussi de **client ID MQTT** et de **nom d'utilisateur MQTT** (semaine 5).

---

## 2. Les topics

| Topic | Sens | QoS | Retain | Rôle |
|---|---|---|---|---|
| `iot/devices/{deviceId}/telemetry` | capteur → backend | 1 | non | Mesures |
| `iot/devices/{deviceId}/status` | capteur → backend | 1 | **oui** | État en ligne / hors ligne |
| `iot/devices/{deviceId}/commands` | backend → capteur | 1 | non | Commandes |
| `iot/devices/{deviceId}/commands/ack` | capteur → backend | 1 | non | Accusé de réception d'une commande |

**Abonnements du backend :**
`iot/devices/+/telemetry`, `iot/devices/+/status`, `iot/devices/+/commands/ack`

**Abonnement de chaque capteur :** `iot/devices/{sonDeviceId}/commands`

**Pourquoi QoS 1 :** chaque message arrive au moins une fois. Un message peut arriver en double ; le backend ignore les doublons grâce à `messageId` (voir section 7).

---

## 3. Télémétrie (mesures)

**Topic :** `iot/devices/sensor-room-01/telemetry`

```json
{
  "schemaVersion": 1,
  "messageId": "3f1c9a2e-8b7d-4c1a-9e2f-1a2b3c4d5e6f",
  "deviceId": "sensor-room-01",
  "timestamp": "2026-10-28T10:15:30Z",
  "measurements": [
    { "type": "temperature", "value": 24.5, "unit": "°C" },
    { "type": "humidity",    "value": 52.0, "unit": "%" }
  ]
}
```

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `schemaVersion` | entier | oui | Toujours `1` pour cette version |
| `messageId` | chaîne (UUID v4) | oui | Identifiant unique du message, pour détecter les doublons |
| `deviceId` | chaîne | oui | Identique au topic |
| `timestamp` | chaîne ISO 8601 UTC | oui | Moment de la mesure (côté capteur) |
| `measurements` | tableau (1 à 10 éléments) | oui | Une entrée par grandeur mesurée |
| `measurements[].type` | chaîne | oui | Voir le catalogue ci-dessous |
| `measurements[].value` | nombre | oui | Valeur mesurée |
| `measurements[].unit` | chaîne | oui | Doit correspondre à l'unité du catalogue |

**Pourquoi un tableau `measurements` plutôt que `"temperature": 24.5` ?**
Ajouter un nouveau type de capteur (compteur d'eau, CO2…) ne change ni le contrat, ni le code du backend, ni la base de données : il suffit d'ajouter une ligne au catalogue. Chaque entrée devient une ligne dans la table `measurement`.

### Catalogue des types de mesure

| `type` | `unit` | Plage valide | Exemple de capteur |
|---|---|---|---|
| `temperature` | `°C` | -40 à 85 | Thermomètre de salle |
| `humidity` | `%` | 0 à 100 | Hygromètre |
| `pressure` | `hPa` | 300 à 1100 | Baromètre |
| `co2` | `ppm` | 0 à 10000 | Qualité de l'air |
| `water_volume` | `L` | ≥ 0 | Compteur d'eau (valeur cumulée) |
| `presence` | `bool` | 0 ou 1 | Détecteur de présence |

Une valeur hors plage est considérée comme une **erreur de capteur** et rejetée. Pour tester les alertes (semaine 6), le simulateur envoie des valeurs **anormales mais dans la plage** (par ex. 31 °C pour un seuil d'alerte de 27 °C).

---

## 4. État du capteur (en ligne / hors ligne)

**Topic :** `iot/devices/sensor-room-01/status` — **retain = true**

Message publié par le capteur juste après sa connexion :

```json
{
  "schemaVersion": 1,
  "deviceId": "sensor-room-01",
  "status": "online",
  "timestamp": "2026-10-28T10:00:00Z",
  "intervalSec": 5,
  "enabled": true,
  "firmware": "sim-1.0.0"
}
```

Message publié avant une déconnexion volontaire :

```json
{ "schemaVersion": 1, "deviceId": "sensor-room-01", "status": "offline", "reason": "shutdown", "timestamp": "2026-10-28T12:00:00Z" }
```

**Last Will (LWT) :** à la connexion, chaque capteur déclare au broker ce message « testament », que Mosquitto publie automatiquement si la connexion est perdue (crash, coupure réseau) :

```json
{ "schemaVersion": 1, "deviceId": "sensor-room-01", "status": "offline", "reason": "connection_lost" }
```

Le LWT n'a pas de `timestamp` (il est écrit à l'avance) : le backend utilise alors l'heure de réception.

| Champ | Valeurs |
|---|---|
| `status` | `online` ou `offline` |
| `reason` | `shutdown`, `connection_lost` (seulement si `offline`) |
| `intervalSec` | Intervalle d'envoi actuel (1 à 3600) |
| `enabled` | `true` si le capteur envoie des mesures, `false` s'il a été désactivé |

**Double sécurité côté backend (semaine 6) :** si aucune télémétrie n'est reçue pendant **3 × `intervalSec`**, le backend marque le capteur comme hors ligne, même sans message LWT. Paramètres MQTT du capteur : `keepAlive = 30 s`.

---

## 5. Commandes (backend → capteur)

**Topic :** `iot/devices/sensor-room-01/commands`

```json
{
  "schemaVersion": 1,
  "commandId": "a7e4b2c1-5d6f-4e3a-8b9c-0d1e2f3a4b5c",
  "type": "set_interval",
  "params": { "intervalSec": 10 },
  "timestamp": "2026-10-28T10:20:00Z"
}
```

| `type` | `params` | Effet sur le capteur |
|---|---|---|
| `set_interval` | `{ "intervalSec": 1..3600 }` | Change la fréquence d'envoi des mesures |
| `disable` | `{}` | Arrête d'envoyer la télémétrie (reste connecté, publie un `status` avec `enabled: false`) |
| `enable` | `{}` | Reprend l'envoi de la télémétrie |
| `ping` | `{}` | Répond seulement par un accusé de réception (test de connexion) |

Après `set_interval`, `enable` ou `disable`, le capteur republie aussi son message `status` pour que l'état affiché reste à jour.

### Accusé de réception

**Topic :** `iot/devices/sensor-room-01/commands/ack`

```json
{
  "schemaVersion": 1,
  "commandId": "a7e4b2c1-5d6f-4e3a-8b9c-0d1e2f3a4b5c",
  "deviceId": "sensor-room-01",
  "result": "ok",
  "message": "intervalSec changé de 5 à 10",
  "timestamp": "2026-10-28T10:20:01Z"
}
```

- `result` : `ok` ou `error` (par ex. type de commande inconnu, paramètre invalide).
- Le backend stocke chaque commande avec un état : `PENDING` → `ACKNOWLEDGED` / `FAILED`, ou `TIMEOUT` si aucun ack n'arrive en 10 secondes.

---

## 6. Capteurs non déclarés

Un capteur **doit être créé dans l'application web** (CRUD) avant que ses messages soient acceptés.

| Cas | Comportement du backend |
|---|---|
| `deviceId` inconnu en base | Message rejeté, log `WARN`, compteur `mqtt_messages_rejected_total{reason="unknown_device"}` incrémenté |
| Capteur désactivé dans l'application | Télémétrie rejetée (`reason="device_disabled"`) |

À partir de la semaine 5, la sécurité MQTT bloque aussi ce cas en amont : sans identifiants Mosquitto, un capteur ne peut même pas se connecter.

---

## 7. Validation côté backend

Pour chaque message reçu, le backend vérifie dans l'ordre :

1. Le JSON est valide et fait moins de 1 Ko.
2. `schemaVersion` est supporté (`1`).
3. Le `deviceId` du message est égal à celui du topic.
4. Le capteur existe en base et est actif (section 6).
5. Les champs obligatoires sont présents et du bon type.
6. Chaque `type` de mesure est dans le catalogue, avec la bonne `unit` et une valeur dans la plage.
7. Le `messageId` n'a pas déjà été enregistré (colonne `UNIQUE` en base) ; sinon le doublon est ignoré sans erreur.

Tout message rejeté est logué avec sa raison et compté dans `mqtt_messages_rejected_total{reason=...}`, ce qui alimente directement Prometheus et Grafana (semaine 7).

---

## 8. Sécurité MQTT (semaine 5)

- Désactiver les connexions anonymes : `allow_anonymous false`.
- Un utilisateur Mosquitto par capteur (nom = `deviceId`) et un utilisateur `iot-backend`.
- Fichier ACL proposé (`%u` = nom de l'utilisateur connecté) :

```
# Capteurs : chacun ne peut écrire et lire que ses propres topics
pattern write iot/devices/%u/telemetry
pattern write iot/devices/%u/status
pattern write iot/devices/%u/commands/ack
pattern read  iot/devices/%u/commands

# Backend
user iot-backend
topic read  iot/devices/+/telemetry
topic read  iot/devices/+/status
topic read  iot/devices/+/commands/ack
topic write iot/devices/+/commands
```

Un capteur ne peut donc pas se faire passer pour un autre, ni envoyer de commandes.

---

## 9. Paramètres de connexion

| Client | Client ID | Clean session | Remarque |
|---|---|---|---|
| Capteur simulé | `{deviceId}` | true | LWT configuré, `keepAlive = 30` |
| Backend | `iot-backend-1` | **false** | Session persistante : le broker garde les messages QoS 1 pendant un redémarrage du backend |

---

## 10. Correspondance avec la base de données (indicatif)

| Message | Table(s) |
|---|---|
| `telemetry` | `measurement(id, device_id, message_id UNIQUE, type, value, unit, measured_at, received_at)` – une ligne par entrée de `measurements` |
| `status` | `device.status`, `device.last_seen_at`, `device.interval_sec` |
| `commands` / `ack` | `command(id, device_id, type, params, status, created_at, acknowledged_at)` |

---

## 11. Points à valider par l'équipe

| N° | Point | OK ? |
|---|---|---|
| 1 | Structure des topics `iot/devices/{deviceId}/...` | |
| 2 | Télémétrie avec un tableau `measurements` et catalogue des types | |
| 3 | QoS 1 partout, retain uniquement sur `status` | |
| 4 | LWT + délai de 3 × `intervalSec` pour l'état hors ligne | |
| 5 | Commandes `set_interval`, `enable`, `disable`, `ping` avec ack | |
| 6 | Rejet des capteurs non déclarés | |
| 7 | ACL Mosquitto proposée pour la semaine 5 | |
