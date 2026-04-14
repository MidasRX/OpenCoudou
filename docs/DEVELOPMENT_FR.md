# OpenCoudou — Documentation Complète pour Développeurs & Contributeurs

> **OpenCoudou** est une réécriture complète de [OpenComputers](https://github.com/MightyPirates/OpenComputers) pour **Minecraft 1.21.4** sur **NeoForge 21.4.0-beta**, écrit en **Kotlin**.  
> Le mod ajoute des ordinateurs programmables en Lua, des écrans, claviers, robots et drones au jeu.

---

## Table des Matières

1. [C'est Quoi Ce Projet ?](#cest-quoi-ce-projet-)
2. [Version du Jeu & Plateforme](#version-du-jeu--plateforme)
3. [Prérequis & Installation des Dépendances](#prérequis--installation-des-dépendances)
4. [Compiler depuis les Sources](#compiler-depuis-les-sources)
5. [Lancer le Mod (Dev / Production)](#lancer-le-mod)
6. [Structure du Projet — Où Se Trouve Quoi](#structure-du-projet--où-se-trouve-quoi)
7. [Architecture — Comment Ça Marche](#architecture--comment-ça-marche)
8. [Flux de Données — Du GPU à l'Écran In-World](#flux-de-données--du-gpu-à-lécran-in-world)
9. [Bugs Connus & Problèmes Actuels](#bugs-connus--problèmes-actuels)
10. [TODOs Restants](#todos-restants)
11. [Guide de Débogage — LISEZ ÇA EN PREMIER](#guide-de-débogage--lisez-ça-en-premier)
12. [Ce Qui a Merdé & Leçons Apprises](#ce-qui-a-merdé--leçons-apprises)
13. [Contribuer](#contribuer)

---

## C'est Quoi Ce Projet ?

OpenCoudou est une **réécriture complète** du mod classique OpenComputers pour Minecraft. L'original était écrit en Scala pour Minecraft 1.7–1.12. Cette version cible le Minecraft moderne (1.21.4) avec NeoForge et Kotlin.

**Ce qui fonctionne actuellement (v3.0.0) :**
- Bloc Computer Case avec inventaire (slots CPU, RAM, GPU, HDD)
- Blocs Screen (Tier 1–3) avec rendu de texte dans le monde
- Entrée clavier transmise à la VM Lua
- Runtime Lua 5.4 via LuaJ (sandboxé, avec timeout)
- API GPU composant (set, fill, copy, setForeground, setBackground, getResolution)
- Composant Filesystem avec VFS (lecture/écriture de fichiers depuis les items HDD)
- BIOS intégré qui découvre automatiquement les médias bootables
- OpenOS complet inclus (60+ commandes shell, 20+ bibliothèques)
- Composant Internet (HTTP GET/POST)
- Composant Redstone (entrée/sortie par face)
- Synchro réseau : buffer écran synchronisé serveur→client via paquets custom
- LED d'alimentation sur le boîtier (vert = allumé, rouge = éteint)
- Son de fonctionnement de l'ordinateur
- Onglet créatif avec tous les items
- Événements touch/drag/scroll sur l'écran
- Support copier-coller clavier

**Ce qui NE fonctionne PAS encore :**
- Robots / Drones (les entités existent mais la logique est stubbed)
- Impression 3D
- Projecteur holographique
- Réseau par câbles entre ordinateurs
- Écrans multi-blocs
- Plusieurs GUIs (Printer, Server Rack n'envoient aucun paquet)
- Système d'énergie / consommation (pas de drain réel)
- Bus de composants / réseau de nœuds propre (approche directe simplifiée utilisée)
- MachineRegistry / NetworkHandler (commentés)

---

## Version du Jeu & Plateforme

| Propriété | Valeur |
|-----------|--------|
| Minecraft | **1.21.4** |
| Mod Loader | **NeoForge 21.4.0-beta** |
| Langage | **Kotlin 2.1.0** (cible JVM 21) |
| Java | **21** (requis) |
| Runtime Lua | **LuaJ 3.0.1** (inclus dans le JAR via jarJar) |
| Mod ID | `opencomputers` |
| Version du Mod | `3.0.0` |
| Gradle | Wrapper inclus, plugin NeoForge ModDev 2.0.28-beta |

---

## Prérequis & Installation des Dépendances

### Pour les Joueurs

1. Installer **Java 21** (ou supérieur) — [Adoptium](https://adoptium.net/)
2. Installer **Minecraft 1.21.4**
3. Installer le loader **NeoForge** pour 1.21.4 — [Téléchargements NeoForge](https://neoforged.net/)
4. Déposer le `opencomputers-3.0.0.jar` dans votre dossier `.minecraft/mods/`
5. Lancer Minecraft avec votre profil NeoForge

### Pour les Développeurs

1. **Java 21 JDK** — `java -version` doit afficher 21+
2. **Git** — pour cloner le dépôt
3. **IDE** — IntelliJ IDEA recommandé (support Kotlin intégré)

```bash
git clone https://github.com/MidasRX/OpenCoudou.git
cd OpenCoudou
```

Gradle télécharge automatiquement toutes les dépendances au premier build :
- **NeoForge 21.4.0-beta** (framework de modding Minecraft)
- **LuaJ 3.0.1** (VM Lua pour JVM — incluse dans le JAR du mod)
- **Kotlin stdlib 2.x** (incluse dans le JAR du mod)
- **Kotlin coroutines 1.8.0** (incluses dans le JAR du mod)
- **Kotlin reflect 2.x** (incluse dans le JAR du mod)
- **Gson** (JSON — déjà dans Minecraft, pas inclus)

Aucune installation manuelle de dépendance nécessaire. Gradle gère tout.

---

## Compiler depuis les Sources

### Compilation Rapide

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

JAR de sortie : `build/libs/opencomputers-3.0.0.jar`

### Déployer vers Minecraft

```powershell
# Windows — copier vers le dossier mods
copy build\libs\opencomputers-3.0.0.jar %APPDATA%\.minecraft\mods\
```

### Lancer en Environnement de Développement

```bash
# Client (ouvre Minecraft avec le mod chargé + hot reload)
gradlew.bat runClient

# Serveur
gradlew.bat runServer

# Génération de données (recettes, modèles, loot tables)
gradlew.bat runData
```

### Propriétés Gradle

Dans `gradle.properties` :
```properties
org.gradle.jvmargs=-Xmx4G    # 4 Go de heap pour Gradle
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

Si vous manquez de mémoire pendant le build, augmentez `-Xmx`.

---

## Lancer le Mod

### Configuration en Jeu (Ordinateur Minimum Viable)

1. Placez un **Computer Case (Tier 1)**
2. Clic droit dessus, insérez : **CPU**, **RAM**, **HDD** (ou EEPROM), **GPU**
3. Placez un bloc **Screen** adjacent au boîtier (dans un rayon de 8 blocs)
4. Placez un **Keyboard** adjacent à l'écran
5. Appuyez sur le **bouton power** dans le GUI du Case (ou envoyez un signal redstone)
6. Le BIOS intégré démarre → cherche `/init.lua` sur le HDD → lance OpenOS
7. Vous obtenez un prompt shell : `/ # _`

### Détection d'Écran

L'ordinateur scanne les écrans dans une boîte de **±8 X/Z, ±4 Y** autour du boîtier.
Il se connecte au **premier écran trouvé** (ordre de scan : X croissant → Y croissant → Z croissant).
Un seul écran est connecté à la fois.

---

## Structure du Projet — Où Se Trouve Quoi

```
OpenCoudou/
├── build.gradle.kts            # Config de build, dépendances, setup NeoForge
├── gradle.properties           # Args JVM, flags de cache
├── settings.gradle.kts         # Nom du projet, repos de plugins
│
├── src/main/kotlin/li/cil/oc/
│   ├── OpenComputers.kt        # Point d'entrée @Mod, câblage des registres
│   ├── Settings.kt             # Valeurs de configuration du mod
│   │
│   ├── api/                    # Interfaces API internes
│   │   ├── driver/Driver.kt
│   │   ├── fs/FileSystem.kt
│   │   ├── machine/            # Architecture, Machine, MachineHost, Context
│   │   └── network/            # Component, Node, Network interfaces
│   │
│   ├── client/                 # CÔTÉ CLIENT UNIQUEMENT
│   │   ├── ClientSetup.kt     # Enregistre renderers, screens, raccourcis clavier
│   │   ├── Sound.kt           # Boucle sonore de l'ordinateur en fonctionnement
│   │   ├── gui/               # 20+ écrans GUI (Case, Robot, Disk, etc.)
│   │   ├── input/             # KeyboardInputHandler (touche→paquet)
│   │   └── renderer/
│   │       ├── SimpleScreenRenderer.kt   # ★ Rendu du texte dans le monde
│   │       ├── CaseRenderer.kt           # LED d'alimentation sur la face du boîtier
│   │       ├── DroneRenderer.kt
│   │       ├── MicrocontrollerRenderer.kt
│   │       └── RobotRenderer.kt
│   │
│   ├── common/                 # PARTAGÉ (client + serveur)
│   │   ├── CommonSetup.kt
│   │   ├── Config.kt           # Chargement de la configuration
│   │   ├── Tier.kt             # Enum Tier 1–3
│   │   ├── block/              # Définitions de blocs (~20 blocs)
│   │   │   ├── CaseBlock.kt
│   │   │   ├── ScreenBlock.kt
│   │   │   ├── KeyboardBlock.kt
│   │   │   ├── CableBlock.kt
│   │   │   └── ...
│   │   ├── blockentity/        # Block entities (tile entities)
│   │   │   ├── CaseBlockEntity.kt    # ★ Hôte de l'ordinateur, exécute la machine
│   │   │   ├── ScreenBlockEntity.kt  # ★ TextBuffer, synchro réseau
│   │   │   └── ...
│   │   ├── init/               # Registres différés
│   │   │   ├── ModBlocks.kt
│   │   │   ├── ModItems.kt
│   │   │   ├── ModBlockEntities.kt
│   │   │   ├── ModMenus.kt
│   │   │   └── ...
│   │   ├── event/              # Gestionnaires d'événements du jeu
│   │   └── item/               # Définitions d'items (composants, matériaux, upgrades)
│   │
│   ├── server/                 # CÔTÉ SERVEUR UNIQUEMENT
│   │   ├── machine/
│   │   │   ├── SimpleLuaArchitecture.kt  # ★★ VM Lua de 3700 lignes (GPU, FS, net, etc.)
│   │   │   ├── SimpleMachine.kt          # Cycle de vie machine, file de signaux
│   │   │   └── InstalledComponents.kt    # Mapping inventaire→composants
│   │   ├── fs/
│   │   │   ├── VirtualFileSystem.kt      # VFS en mémoire pour les items HDD
│   │   │   └── OpenOSContent.kt          # Charge les fichiers OpenOS depuis les ressources
│   │   └── entity/
│   │       └── RobotAndDroneEntities.kt  # Stubbé
│   │
│   ├── network/
│   │   └── ModPackets.kt       # ★ Toutes les définitions de paquets & handlers
│   │
│   ├── util/
│   │   ├── TextBuffer.kt       # ★ Grille 2D char/fg/bg
│   │   ├── KeyboardKeys.kt     # Constantes de codes de touches
│   │   └── OCLogger.kt         # Utilitaire de logging
│   │
│   ├── datagen/
│   │   └── DataGenerators.kt   # Générateurs de données (recettes/modèles/loot)
│   │
│   └── integration/
│       ├── IntegrationManager.kt
│       └── SimpleComponent.kt
│
├── src/main/resources/
│   ├── META-INF/
│   │   └── neoforge.mods.toml  # Métadonnées du mod (id, version, dépendances)
│   ├── pack.mcmeta             # Métadonnées du resource pack
│   ├── assets/opencomputers/
│   │   ├── blockstates/        # Fichiers JSON de block states
│   │   ├── models/             # Modèles 3D (blocs & items)
│   │   ├── textures/           # Textures
│   │   ├── sounds/             # Fichiers sonores
│   │   └── lua/                # ★★ Programmes Lua
│   │       ├── bios.lua        # BIOS autonome
│   │       └── openos/         # OpenOS complet
│   │           ├── init.lua    # Point d'entrée du boot
│   │           ├── bin/        # 60+ commandes (ls, cat, edit, grep, ...)
│   │           ├── lib/        # 20+ bibliothèques (event, component, term, ...)
│   │           └── etc/        # Config (motd, profile)
│   ├── config/
│   │   └── opencomputers.toml  # Config par défaut
│   └── data/opencomputers/
│       ├── recipes/            # Recettes de craft
│       └── loot_tables/        # Tables de loot
│
├── docs/                       # Documentation (vous êtes ici)
│   ├── API_REFERENCE.md        # Référence des API composants Lua
│   ├── COMMANDS.md             # Commandes shell OpenOS
│   ├── GETTING_STARTED.md      # Guide de démarrage en jeu
│   ├── PROGRAMMING.md          # Guide de programmation Lua
│   ├── DEVELOPMENT.md          # Documentation dev (anglais)
│   └── DEVELOPMENT_FR.md       # ★ Ce fichier (français)
│
└── ~80+ fichiers .kt.disabled  # Fonctionnalités différées (code OC original, pas encore porté)
```

### Fichiers Clés que Vous Toucherez le Plus Souvent

| Fichier | Ce Qu'il Fait | Lignes |
|---------|--------------|--------|
| `SimpleLuaArchitecture.kt` | La VM Lua entière : API GPU, filesystem, internet, signaux, BIOS | ~3700 |
| `SimpleScreenRenderer.kt` | Rend le texte du TextBuffer sur les faces des blocs écran dans le monde | ~140 |
| `ScreenBlockEntity.kt` | État de l'écran, TextBuffer, paquets de synchro serveur→client | ~175 |
| `CaseBlockEntity.kt` | Boîtier : inventaire, cycle de vie machine, détection d'écran | ~285 |
| `ModPackets.kt` | Tous les paquets réseau (clavier, mises à jour écran, alimentation, touch) | ~600 |
| `TextBuffer.kt` | Stockage grille 2D caractères + couleurs | ~270 |
| `SimpleMachine.kt` | Machine à états (start/stop/pause), file de signaux | ~200 |

---

## Architecture — Comment Ça Marche

### La VM Lua

Le cœur du mod est `SimpleLuaArchitecture.kt`. Il crée un sandbox **LuaJ** par ordinateur :

```
LuaJ (org.luaj.vm2) Globals
├── Bibliothèques : Base, Package, Bit32, Table, String, Math, Coroutine, Debug
├── Timeout : 5 secondes max d'exécution, vérifié toutes les 10 000 instructions
├── APIs exposées au Lua :
│   ├── component.list()        — énumérer le matériel
│   ├── component.invoke()      — appeler les méthodes des composants
│   ├── computer.pushSignal()   — mettre des événements en file
│   ├── computer.pullSignal()   — attendre des événements (yield coroutine)
│   ├── gpu.set/fill/copy/...   — dessin GPU (modifie le TextBuffer)
│   ├── os.clock/date/time      — fonctions de temps
│   ├── internet.request()      — HTTP GET/POST
│   └── unicode.char/len/sub    — opérations sur chaînes Unicode
└── BIOS intégré : trouve GPU+écran, cherche un FS bootable, exécute /init.lua
```

### Le Système de Composants (Simplifié)

Contrairement à l'OpenComputers original qui avait un réseau complet de nœuds en graphe, cette réécriture utilise une **approche directe simplifiée** :

1. `CaseBlockEntity` scanne son inventaire pour trouver les items (CPU, RAM, GPU, HDD, etc.)
2. `SimpleMachine` les mappe à des adresses de composants virtuels (UUIDs)
3. `SimpleLuaArchitecture.registerVirtualComponents()` crée les composants appelables depuis Lua
4. Les opérations GPU modifient directement le `TextBuffer` du `ScreenBlockEntity` voisin

### Tiers

| Tier | Résolution Écran | Profondeur Couleur | RAM |
|------|-----------------|-------------------|-----|
| 1 | 50×16 | 16 couleurs | 192 Ko |
| 2 | 80×25 | 256 couleurs | 512 Ko |
| 3 | 160×50 | 16.7M couleurs | 1024 Ko |

---

## Flux de Données — Du GPU à l'Écran In-World

Voici le pipeline complet depuis un appel Lua `gpu.set(1, 1, "Hello")` jusqu'aux pixels à l'écran :

```
1. Le code Lua appelle gpu.set(x, y, text)
         │
         ▼
2. SimpleLuaArchitecture.kt
   Handler GPU "set" (s'exécute sur le thread serveur)
   Appelle findNearbyScreen() → obtient le ScreenBlockEntity
   Écrit dans screen.buffer.set(x-1, y-1, char, fg)
   Appelle screen.markForSync()
         │
         ▼
3. ScreenBlockEntity.serverTick()
   Vérifie le flag needsSync
   Appelle createFullSyncPacket()
   Sérialise charData + fgData + bgData → ByteBuffer
   Envoie ScreenUpdatePacket via PacketDistributor.sendToPlayersTrackingChunk()
   Log debug : "SCREEN TICK: sending packet for (x,y,z), nonSpaceChars=N"
         │
         ▼
4. ModPackets.handleScreenUpdate() [CÔTÉ CLIENT]
   Reçoit le paquet, trouve le ScreenBlockEntity à la position
   Appelle ScreenBlockEntity.applyScreenUpdate(data)
   Décode ByteBuffer → met à jour buffer.charData/fgData/bgData via setRawData()
   Log debug : "CLIENT applied screen update: nonSpaceChars=N"
         │
         ▼
5. SimpleScreenRenderer.render() [CÔTÉ CLIENT, chaque frame]
   Récupère le buffer depuis ScreenBlockEntity
   Détermine l'orientation du bloc (NORTH/SOUTH/EAST/WEST)
   Applique les transformations de pose (translate + rotate) pour positionner le texte
   Itère toutes les cellules : ignore char ≤ 32 (espace), dessine les autres via Font.drawInBatch()
   Utilise Font.DisplayMode.SEE_THROUGH + flush endBatch()
   Log debug (toutes les 200 frames) : "RENDERER: buffer WxH, nonSpaceChars=N"
```

### Résumé des Paquets Réseau

| Direction | Paquet | Fonction |
|-----------|--------|----------|
| Serveur→Client | `ScreenUpdatePacket` | Synchro complète du TextBuffer (chars+fg+bg) |
| Serveur→Client | `ComputerStatePacket` | État alimentation, énergie, nombre de composants |
| Client→Serveur | `ComputerPowerPacket` | Basculer le bouton power |
| Client→Serveur | `KeyboardInputPacket` | Appui/relâchement de touches + copier-coller |
| Client→Serveur | `ScreenTouchPacket` | Touch/drag/drop/scroll en coordonnées caractère |

---

## Bugs Connus & Problèmes Actuels

### CRITIQUE : Le Texte S'affiche du Mauvais Côté de l'Écran

**Statut :** Bug actif, pas encore corrigé.

**Symptôme :** Le texte est visible en regardant l'**arrière** du bloc écran, mais invisible depuis l'**avant**. C'est un problème classique de backface culling.

**Cause Racine :** Les transformations de pose dans `SimpleScreenRenderer.kt` positionnent le texte avec des normales orientées **vers l'intérieur** du bloc au lieu de **vers l'extérieur** en direction du joueur. La rotation `XP.rotationDegrees(180f)` inverse Y (correct — la police dessine vers le bas) mais inverse aussi la normale Z (incorrect — le texte pointe vers l'intérieur).

**Où :** `SimpleScreenRenderer.kt` lignes 66–100, le bloc `when(facing)`.

**Comment corriger :** Les transformations doivent produire des quads de texte avec des normales pointant **hors** de la face du bloc, vers le joueur. Il faut soit restructurer la chaîne de rotations, soit ajouter un scale/rotation compensatoire.

### Le Logging Debug Est Encore Actif

Des appels `LOGGER.info()` de diagnostic sont présents dans trois fichiers et vont spammer les logs :
- `ScreenBlockEntity.kt` — log chaque paquet de synchro envoyé
- `ModPackets.kt` — log chaque mise à jour d'écran reçue côté client
- `SimpleScreenRenderer.kt` — log toutes les 200 frames

**Ceux-ci doivent être supprimés ou changés en `LOGGER.debug()` avant la release.**

### La Détection d'Écran Est Simpliste

L'ordinateur se connecte au **premier** `ScreenBlockEntity` trouvé dans un scan ±8/±4. Si plusieurs écrans existent à proximité, un seul fonctionne. Les écrans multi-blocs ne sont pas supportés.

### Pas de Vrai Système d'Énergie

Les ordinateurs ne consomment pas d'énergie. La LED d'alimentation fonctionne mais est cosmétique. Les Capacitors et Power Converters existent comme blocs mais ne font rien de fonctionnel.

---

## TODOs Restants

Ce sont les commentaires `TODO` trouvés directement dans le code source :

| Fichier | TODO | Priorité |
|---------|------|----------|
| `OpenComputers.kt` | Réactiver `NetworkHandler` et `MachineRegistry` | Moyenne |
| `CommonSetup.kt` | Réactiver quand le système de config est prêt | Basse |
| `ClientSetup.kt` | Réactiver les renderers additionnels (hologramme, rack, etc.) | Basse |
| `PrinterBlock.kt` | Ouvrir le GUI imprimante au clic droit | Basse |
| `ServerRackBlock.kt` | Ouvrir le GUI server rack au clic droit | Basse |
| `RackScreen.kt` | Envoyer un paquet au serveur (GUI sans backend) | Basse |
| `DriveScreen.kt` | Envoyer un paquet au serveur (renommage label, 2 endroits) | Basse |
| `InputEventHandler.kt` | Envoyer des paquets réseau pour divers événements d'input (6 endroits) | Moyenne |
| `ModItems.kt` | Implémenter la classe DroneItem | Basse |
| `ModItems.kt` | Implémentation correcte des items upgrade | Basse |
| `RobotEventHandler.kt` | Implémenter quand l'entité robot existe | Basse |

### Fonctionnalités Pas Encore Portées (fichiers `.kt.disabled`)

Il y a **~80+ fichiers Kotlin désactivés** provenant du port OC original. Ils représentent des fonctionnalités différées :
- Réseau complet en graphe de nœuds (`network/`)
- Système de composants original avec visibilité/accessibilité
- Nombreux block entities (chargeur, assembleur, hologramme, etc.)
- Renderers originaux (hologramme, câble, rack, etc.)
- Fusion d'écrans multi-blocs
- IA et mouvement Robot/Drone
- Système de loot disks (partiellement porté)
- Intégration énergie Forge

---

## Guide de Débogage — LISEZ ÇA EN PREMIER

Si vous travaillez sur ce mod et que quelque chose ne marche pas, voici le guide de débogage développé à travers des essais-erreurs douloureux.

### 1. Vérifiez les Logs

Le log du jeu est votre meilleur ami. Regardez :
- **Dernier log :** `run/logs/latest.log` (environnement dev) ou `.minecraft/logs/latest.log` (production)
- **Cherchez :** `SCREEN TICK`, `CLIENT handleScreenUpdate`, `RENDERER:`

Si vous voyez `SCREEN TICK: nonSpaceChars=N` avec N > 0, le serveur a des données texte.  
Si vous voyez `CLIENT applied screen update: nonSpaceChars=N` avec N > 0, le client les a reçues.  
Si vous voyez `RENDERER: nonSpaceChars=N` avec N > 0, le renderer a les données — c'est un problème de rendu.

### 2. Tracez le Flux de Données

Si l'écran est noir, déterminez OÙ les données s'arrêtent :

```
Appel GPU Lua → TextBuffer sur serveur → Paquet envoyé ? → Paquet reçu ? → Buffer sur client → Le renderer le voit ?
```

Ajoutez des `println()` ou `LOGGER.info()` à chaque étape. Ne devinez pas — **vérifiez**.

### 3. Problèmes de Rendu Courants

| Symptôme | Cause Probable |
|----------|---------------|
| Texte complètement invisible | Z-fighting : texte à la même profondeur que la face du bloc. Augmentez `Z_OFFSET`. |
| Texte visible de derrière, pas de devant | Backface culling : les normales du quad sont dans le mauvais sens. Corrigez les transformations de pose. |
| Le texte clignote/scintille | Z-fighting ou ordre des batchs. Utilisez le mode `SEE_THROUGH` + `endBatch()`. |
| Texte mal positionné | Les maths de transformation sont fausses. Vérifiez translate/rotate par direction. |
| Texte sur la mauvaise face | La propriété `FACING` ne correspond pas à la direction attendue. Vérifiez le blockstate. |

### 4. Problèmes de Flux de Données Courants

| Symptôme | Cause Probable |
|----------|---------------|
| Le serveur a les données, pas le client | Le paquet n'est pas envoyé. Vérifiez le flag `needsSync` et `serverTick()`. |
| Le client reçoit le paquet, le buffer est vide | `applyScreenUpdate()` décode mal. Vérifiez l'ordre du ByteBuffer. |
| Les appels GPU ne font rien | `findNearbyScreen()` retourne null. Aucun écran connecté. Vérifiez la portée de scan. |
| Plusieurs écrans, le mauvais s'allume | La détection d'écran prend le PREMIER trouvé. Ordre de scan : X→Y→Z croissant. |
| L'ordinateur ne démarre pas | CPU ou RAM manquant dans l'inventaire du boîtier. Vérifiez la validation de `scanInventory()`. |

### 5. Techniques de Debug Utiles

**Ajouter du logging temporaire dans SimpleLuaArchitecture :**
```kotlin
// Dans le handler GPU "set" :
OpenComputers.LOGGER.info("GPU SET: x=$x, y=$y, text=$text, screen=${cachedScreen?.blockPos}")
```

**Vérifier quels composants sont enregistrés :**
```kotlin
// Dans SimpleLuaArchitecture.registerVirtualComponents() :
OpenComputers.LOGGER.info("Registered component: $type @ $address")
```

**Vérifier la connexion de l'écran :**
```kotlin
// Dans CaseBlockEntity.connectNearbyScreens() :
OpenComputers.LOGGER.info("Connected screen at ${screen.blockPos}, address=${screen.address}")
```

**Dumper le contenu du TextBuffer :**
```kotlin
// N'importe où vous avez accès à un TextBuffer :
val lines = buffer.getLines()
lines.forEachIndexed { i, line -> 
    if (line.isNotBlank()) OpenComputers.LOGGER.info("Buffer line $i: '$line'")
}
```

### 6. Cycle Build & Test

```powershell
# Compiler le JAR
.\gradlew.bat jar

# Copier vers le dossier mods
copy build\libs\opencomputers-3.0.0.jar $env:APPDATA\.minecraft\mods\ -Force

# Lancer Minecraft, vérifier les logs à :
# %APPDATA%\.minecraft\logs\latest.log

# OU utilisez directement le client dev :
.\gradlew.bat runClient
```

**Astuce pro :** Utilisez `runClient` pour itérer plus vite — pas besoin de copier des JARs.

### 7. Comprendre le Rendu Minecraft

Concepts clés pour le rendu de l'écran :

- **PoseStack :** Une pile de transformations (comme la pile de matrices OpenGL). `pushPose()` / `popPose()` pour sauvegarder/restaurer.
- **Origine du block entity :** (0,0,0) = coin inférieur-nord-ouest du bloc. (1,1,1) = coin supérieur-sud-est.
- **Font.drawInBatch() :** Dessine le texte dans le plan XY. +X = droite, +Y = bas. Le quad pointe vers +Z.
- **Directions des faces :**
  - Face NORTH : à z=0, le joueur est à z < 0 et regarde vers +Z
  - Face SOUTH : à z=1, le joueur est à z > 1 et regarde vers -Z
  - Face EAST : à x=1, le joueur est à x > 1 et regarde vers -X
  - Face WEST : à x=0, le joueur est à x < 0 et regarde vers +X
- **Mode SEE_THROUGH :** Ignore le depth buffer → le texte est toujours visible par-dessus les faces du bloc. Nécessite `endBatch()` pour forcer l'ordre de dessin correct.
- **Backface culling :** Si la normale d'un quad pointe en sens inverse de la caméra, il est invisible. C'est pourquoi la direction du texte est importante.

---

## Ce Qui a Merdé & Leçons Apprises

Cette section documente les bugs qui ont été trouvés et corrigés (ou partiellement corrigés) pendant le développement, pour que les futurs contributeurs ne refassent pas les mêmes erreurs.

### Bug 1 : Mauvais Écran Connecté

**Problème :** Le GPU écrivait sur le premier écran trouvé, mais `connectNearbyScreens()` marquait TOUS les écrans proches comme connectés. Le premier écran (par ordre de scan) recevait les écritures GPU, mais un écran différent pouvait être "le" connecté du point de vue du boîtier.

**Correction :** Changé `connectNearbyScreens()` pour ne connecter que l'écran **principal** (celui que `findAndCacheNearbyScreen()` retournerait) et déconnecter explicitement tous les autres.

**Leçon :** Quand deux systèmes scannent indépendamment des blocs (le GPU cherchant un écran + le Case connectant les écrans), ils DOIVENT utiliser le **même ordre de scan et la même logique de sélection** sinon ils choisiront des blocs différents.

### Bug 2 : LED HDD Clignotante

**Problème :** Le `CaseRenderer` avait une LED "activité disque dur" qui clignotait avec `((gameTime * 7) % 13) < 6`. Ça créait un effet stroboscopique rapide et distrayant.

**Correction :** Suppression complète de la LED d'activité HDD. Seule la LED d'alimentation reste.

**Leçon :** Ne simulez pas des LEDs d'activité matérielle avec des patterns pseudo-aléatoires — ça rend horrible en jeu.

### Bug 3 : Texte Invisible (Profondeur / Ordre de Dessin)

**Problème :** Le texte était dessiné à la même profondeur que la face du bloc, causant du Z-fighting. De plus, le système de batch rendering de Minecraft pouvait dessiner la face du bloc APRÈS le texte, l'écrasant complètement.

**Correction :** Trois changements combinés :
1. Augmenté `Z_OFFSET` de 0.005f à 0.02f (plus de séparation de profondeur)
2. Passé de `Font.DisplayMode.NORMAL` à `SEE_THROUGH` (ignore le test de profondeur)
3. Ajouté un appel `endBatch()` après le rendu du texte pour forcer le flush immédiat

**Leçon :** Dans le pipeline de rendu de Minecraft, on ne peut pas compter sur l'ordre des draw calls. `SEE_THROUGH` + `endBatch()` est le pattern fiable pour le texte superposé.

### Bug 4 : Le Texte Pointe vers l'Intérieur (ACTUEL)

**Problème :** Le texte est visible depuis l'arrière de l'écran mais pas de devant. La rotation `XP.rotationDegrees(180f)` inverse à la fois Y (voulu pour la police) et la normale Z (cause le backface culling vu de devant).

**Statut :** Pas encore corrigé. Les transformations de pose dans `SimpleScreenRenderer.kt` ont besoin d'être retravaillées.

**Leçon :** Quand vous appliquez des rotations pour le rendu de texte, n'oubliez pas que tourner autour de X de 180° inverse À LA FOIS Y et Z. Si vous voulez seulement inverser Y (pour que le texte descende), il faut une approche différente — soit utiliser `scale(1, -1, 1)` soit compenser l'inversion de Z avec une autre transformation.

---

## Contribuer

### Style de Code

- Kotlin avec cible JVM 21
- `freeCompilerArgs = -Xjvm-default=all` (méthodes par défaut des interfaces)
- Pas de formateur spécifique imposé — restez cohérent avec le code environnant
- Gardez `SimpleLuaArchitecture.kt` organisé par section d'API (GPU, FS, Internet, etc.)

### Avant de Soumettre une PR

1. Le build passe : `gradlew.bat build`
2. Testez en jeu : démarrez un ordinateur, lancez `edit test.lua`, vérifiez que l'écran fonctionne
3. Vérifiez le spam de logs : supprimez ou downgrader tout logging de debug que vous avez ajouté
4. Mettez à jour cette documentation si vous avez changé l'architecture ou corrigé un bug

### Git

```bash
git remote -v
# origin: https://github.com/MidasRX/OpenCoudou.git

git checkout main
git pull
# Faites vos modifications
git add -A
git commit -m "description du changement"
git push origin main
```

---

*Dernière mise à jour : Avril 2026 — OpenCoudou v3.0.0*
