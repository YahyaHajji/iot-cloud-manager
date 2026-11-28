# Simulateur MQTT

Le simulateur publie la télémétrie et le statut de chaque capteur sur
`iot/devices/<deviceId>/...`. Le broker doit avoir l'authentification activée.
Chaque capteur utilise son `deviceId` comme nom d'utilisateur MQTT; son mot de
passe doit être fourni dans `SIMULATOR_SENSORS`. Il n'y a pas de mot de passe
par défaut.

## Variables d'environnement

| Variable | Obligatoire | Valeur par défaut | Description |
| --- | --- | --- | --- |
| `MQTT_HOST` | non | `localhost` | Hôte du broker MQTT |
| `MQTT_PORT` | non | `1883` | Port du broker MQTT |
| `MQTT_TLS` | non | `false` | Active TLS avec les certificats CA de confiance du système (`true` ou `false`) |
| `SIMULATOR_SENSORS` | oui | aucune | Tableau JSON de capteurs avec `deviceId`, `intervalSec` et `password` |
| `HEARTBEAT_INTERVAL_SEC` | non | `30` | Intervalle des messages de statut online, de 1 à 3600 secondes |
| `ANOMALY_EVERY_N` | non | `0` (désactivé) | Une température de 45 °C est envoyée à chaque N-ième mesure; `0` désactive le mode |

Exemple de structure, avec un mot de passe factice à remplacer localement :

```json
[
  {
    "deviceId": "sensor-room-01",
    "intervalSec": 5,
    "password": "CHANGE_ME"
  }
]
```

Les identifiants doivent correspondre aux utilisateurs du passwordfile Mosquitto.
Les ACL autorisent un capteur à publier uniquement dans ses propres topics.
N'ajoutez jamais de vrais mots de passe à Git ou à un fichier d'exemple versionné.

## Lancement local

Installez les dépendances, définissez les variables dans votre environnement
local, puis démarrez le simulateur :

```sh
python -m pip install -r simulator/requirements.txt
python simulator/simulateur.py
```

Exemple de configuration pour un broker local. Remplacez `CHANGE_ME` par un
mot de passe de test local qui correspond au compte créé avec `mosquitto_passwd` :

```sh
export MQTT_HOST=localhost
export MQTT_PORT=1883
export MQTT_TLS=false
export SIMULATOR_SENSORS='[{"deviceId":"sensor-room-01","intervalSec":5,"password":"CHANGE_ME"}]'
export HEARTBEAT_INTERVAL_SEC=30
export ANOMALY_EVERY_N=5
python simulator/simulateur.py
```

Pour un broker TLS, utilisez son nom DNS correspondant au certificat, le port
TLS (généralement `8883`) et `MQTT_TLS=true`. Le simulateur vérifie le certificat
du broker avec les autorités de certification du système.

Le mode anomalie est facultatif; supprimez `ANOMALY_EVERY_N` ou mettez-le à `0`
pour le désactiver.

## Lancement avec Docker

Construisez l'image depuis la racine du dépôt :

```sh
docker build -f simulator/Dockerfile -t iot-simulator .
```

Placez les variables dans un fichier local non versionné, par exemple
`simulator/.env` (avec `MQTT_HOST=mosquitto` si le conteneur rejoint le réseau
Docker du broker). Puis lancez-le sur le même réseau Docker que Mosquitto :

```sh
docker run --rm --network iot-cloud-manager_default --env-file simulator/.env iot-simulator
```

Adaptez le nom du réseau au nom du projet Compose de votre environnement. Ne
placez aucun vrai mot de passe dans un exemple, une commande enregistrée ou Git.

## Tests

```sh
python -m pip install -r simulator/requirements-dev.txt
python -m pytest simulator/test_simulateur.py
```
