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

## Démarrer les services

```sh
docker compose up -d --build
```

Les ports MQTT et API sont liés à `127.0.0.1`; utilisez un tunnel SSH pour y
accéder depuis une autre machine.

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
