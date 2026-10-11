# ProjetForum — Application de Forum en Java

Application de chat en temps réel avec interface graphique, basée sur une architecture **client-serveur TCP** et une sérialisation Java.

---

## Fonctionnalités

- **Connexion** : saisie d'un pseudo unique, adresse IP du serveur et port
- **Lobby** : liste des salons disponibles, création et rejoint de salons
- **Chat** : envoi et réception de messages en temps réel dans un salon
- **Administration** : le créateur d'un salon peut expulser (*kick*) ou bannir (*ban*) des membres
- **Interface sombre** : thème graphique Swing moderne (dark mode)

---

## Structure du projet

```
ProjetForum/
└── src/
    └── forum/
        ├── client/
        │   ├── ForumClient.java       # Gestion de la connexion TCP côté client
        │   └── gui/
        │       ├── LoginFrame.java    # Fenêtre de connexion
        │       ├── LobbyFrame.java    # Fenêtre du lobby (liste des salons)
        │       └── ChatFrame.java     # Fenêtre de chat d'un salon
        ├── common/
        │   ├── Message.java           # Objet sérialisable échangé entre client et serveur
        │   ├── ChatRoom.java          # Représentation d'un salon (thread-safe)
        │   └── ClientHandler.java     # Thread dédié à chaque client connecté
        └── server/
            ├── ForumServer.java       # Point d'entrée du serveur
            └── RoomManager.java      # Gestion des salons et des clients connectés
```

---

## Prérequis

- **Java 8** ou version supérieure
- IDE compatible (Eclipse recommandé — fichiers `.project` et `.classpath` inclus)

---

## Lancement

### 1. Démarrer le serveur

Exécuter la classe `forum.server.ForumServer` :

```bash
java forum.server.ForumServer
```

Le serveur écoute par défaut sur le **port 5000**.

### 2. Démarrer le client

Exécuter la classe `forum.client.gui.LoginFrame` (ou le point d'entrée de l'interface graphique).

Dans la fenêtre de connexion :
- **Pseudo** : nom d'utilisateur (1 à 20 caractères)
- **IP serveur** : adresse IP de la machine hébergeant le serveur (ex. `127.0.0.1` en local)
- **Port** : `5000` par défaut

---

## Architecture

```
Client  ──TCP──►  Serveur
        ◄──TCP──
```

- La communication repose sur la **sérialisation Java** (`ObjectInputStream` / `ObjectOutputStream`).
- Chaque client est géré par un `ClientHandler` dédié dans un thread séparé.
- Le `RoomManager` centralise la gestion des salons et des utilisateurs connectés (`ConcurrentHashMap`).

### Types de messages (`Message.Type`)

| Direction | Types |
|-----------|-------|
| Client → Serveur | `LOGIN`, `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `CHAT`, `LIST_ROOMS`, `LOGOUT`, `KICK_USER`, `BAN_USER` |
| Serveur → Client | `LOGIN_OK`, `LOGIN_ERROR`, `ROOM_CREATED`, `ROOM_JOINED`, `ROOM_LEFT`, `ROOM_LIST`, `CHAT_BROADCAST`, `USER_JOINED`, `USER_LEFT`, `MEMBER_LIST`, `ERROR`, `SERVER_INFO`, `KICKED`, `BANNED` |

---

## Comportements notables

- Un salon est **automatiquement supprimé** lorsqu'il ne contient plus aucun membre.
- Un utilisateur banni ne peut pas rejoindre le salon à nouveau.
- Seul le **créateur** d'un salon peut expulser ou bannir des membres.
- Les pseudos sont **uniques** : deux clients ne peuvent pas utiliser le même nom simultanément.
