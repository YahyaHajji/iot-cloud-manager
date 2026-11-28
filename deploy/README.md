# Déploiement

Depuis `deploy`, copiez `.env.example` vers `.env` et remplacez les valeurs
factices. Le broker n'accepte pas les connexions anonymes. Créez le fichier de
mots de passe avant de démarrer Compose.

```sh
cp .env.example .env
```

## Créer les identifiants Mosquitto

Avec `mosquitto_passwd` installé sur la machine hôte :

```sh
mosquitto_passwd -c ./mosquitto/passwordfile iot-backend
mosquitto_passwd ./mosquitto/passwordfile sensor-room-01
```

La première commande crée le fichier et l'utilisateur backend; la seconde
ajoute un utilisateur pour un capteur. Répétez la seconde commande pour chaque
capteur en utilisant son `deviceId` comme nom d'utilisateur. Chaque commande
vous demande le mot de passe de façon interactive.

Ne versionnez jamais `deploy/.env` ni `deploy/mosquitto/passwordfile`.

## Notifications par e-mail

Configurez `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` et
`ALERT_EMAIL_TO` dans `deploy/.env`. Les notifications sont envoyées après
l'enregistrement de l'alerte, en arrière-plan; un même capteur et un même type
de mesure ne déclenchent au plus qu'un e-mail toutes les dix minutes. Sans hôte
SMTP ou destinataire configuré, les notifications restent désactivées.

## Démarrer les services

```sh
docker compose up -d --build
```

L'API et le listener MQTT TCP local (1883) sont liés à `127.0.0.1`; utilisez
un tunnel SSH pour y accéder depuis une autre machine. Le backend et le
simulateur de cette configuration Compose utilisent le broker TLS à
`iot-manager-yahya.duckdns.org:8883`. Avant de les démarrer, ce nom doit pointer
vers un listener Mosquitto TLS accessible depuis les conteneurs, avec un
certificat valide pour ce nom. La configuration du listener et l'ouverture du
port 8883 sur le serveur restent des prérequis d'infrastructure.

## Frontend, Nginx et HTTPS

Le conteneur `frontend` contient le frontend React compilé et Nginx. Nginx
écoute sur les ports 80 et 443, redirige HTTP vers HTTPS, sert le frontend,
relaie `/api/` vers le backend et refuse `/actuator/` depuis Internet. Le
backend, MQTT et PostgreSQL restent liés à `127.0.0.1`.

### Prérequis sur la VM

1. Un nom de domaine DuckDNS qui pointe vers l'adresse IP publique de la VM.
2. Les ports 80 et 443 ouverts dans la liste de sécurité Oracle et dans
   `iptables`, puis `sudo netfilter-persistent save`.
3. Dans `deploy/.env`, `CORS_ALLOWED_ORIGINS` et `VITE_API_BASE_URL` égaux à
   l'adresse HTTPS du site.

### Obtenir le certificat Let's Encrypt

Avec le conteneur `frontend` arrêté (le port 80 doit être libre) :

```sh
sudo apt install -y certbot
sudo certbot certonly --standalone -d iot-manager-yahya.duckdns.org
```

Le certificat est écrit dans `/etc/letsencrypt`, que Compose monte en lecture
seule dans le conteneur. Ne versionnez jamais ce dossier.

### Renouvellement

Certbot renouvelle automatiquement. Pour vérifier :

```sh
sudo certbot renew --dry-run
```

Le renouvellement en mode `standalone` a besoin du port 80. Ajoutez dans
`/etc/letsencrypt/renewal/<domaine>.conf` les commandes
`pre_hook = docker stop iot-frontend` et `post_hook = docker start iot-frontend`.

### Mettre à jour le site

```sh
git pull
docker compose up -d --build frontend
```

Si le nom de domaine change, modifiez `frontend/nginx.conf` (`server_name` et
chemins des certificats) puis reconstruisez le conteneur.

## Supervision : Prometheus et Grafana

Prometheus lit les métriques du backend toutes les 15 secondes
(`backend:8080/actuator/prometheus`, sur le réseau Docker interne) et les garde
15 jours. Grafana affiche le tableau de bord « IoT Cloud Manager - Backend »
(état du backend, mémoire, CPU, requêtes HTTP, connexions à la base, messages
MQTT rejetés), chargé automatiquement depuis `deploy/grafana/`.

Les deux services sont liés à `127.0.0.1` : ils ne sont pas accessibles depuis
Internet, et Nginx bloque `/actuator/`. Définissez `GRAFANA_ADMIN_PASSWORD`
dans `deploy/.env`.

### Accéder à Grafana depuis votre ordinateur

```sh
ssh -i <cle>.key -L 3000:127.0.0.1:3000 -L 9090:127.0.0.1:9090 ubuntu@<adresse-ip-de-la-vm>
```

Puis ouvrez http://localhost:3000 (utilisateur `admin`) pour Grafana et
http://localhost:9090/targets pour vérifier que la cible `iot-backend` est `UP`.

### Démarrer

```sh
docker compose up -d prometheus grafana
```

## Simulateur de capteurs

Le service `simulator` publie la télémétrie des capteurs fictifs sur le broker
TLS défini dans `deploy/docker-compose.yml`. Chaque capteur se connecte avec
son `deviceId` comme nom d'utilisateur MQTT. Le simulateur vérifie le
certificat du broker avec les autorités système.

### Avant de le démarrer

1. Créez chaque capteur dans le `passwordfile` de Mosquitto, avec un mot de
   passe différent :

   ```sh
   mosquitto_passwd -b ./mosquitto/passwordfile sensor-room-01 <mot-de-passe-1>
   mosquitto_passwd -b ./mosquitto/passwordfile sensor-room-02 <mot-de-passe-2>
   docker compose restart mosquitto
   ```

2. Enregistrez les mêmes capteurs dans le backend (`POST /api/devices` ou
   l'écran Capteurs). Le backend rejette les mesures d'un capteur inconnu.
3. Dans `deploy/.env`, renseignez `SIMULATOR_SENSORS` avec les mêmes mots de
   passe, sur une seule ligne.

### Démarrer et vérifier

```sh
docker compose up -d --build simulator
docker compose logs -f simulator
```

Chaque capteur doit afficher `connected`. Si le journal indique
`Connection rejected`, le mot de passe du `passwordfile` et celui de
`SIMULATOR_SENSORS` ne correspondent pas.

### Démonstration des alertes

Pour provoquer des alertes, définissez `ANOMALY_EVERY_N=5` dans `deploy/.env`
puis `docker compose up -d simulator`. Une température de 45 °C est envoyée
toutes les 5 mesures. Créez d'abord un seuil sur `temperature` pour le capteur
(`POST /api/alerts/thresholds`).
