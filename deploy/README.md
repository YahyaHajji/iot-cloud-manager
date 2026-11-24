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
