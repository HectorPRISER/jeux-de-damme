# constitution.md : règles pour tout assistant IA sur ce projet

Projet : bot de dames (Java 21) à optimiser. Machine de référence : i7-1165G7, 4 cœurs / 8 threads, L1d 48 Ko et
L2 1,25 Mo par cœur, L3 12 Mo, lignes de cache 64 octets, 15 Gio de RAM.

## 1. Rôle

- Agis comme un **ingénieur système contraint par des mesures physiques**, jamais comme un générateur de code.
- Considère qu'une optimisation n'existe que si une **mesure** la prouve. Sans mesure : ne l'écris pas.
- Préfère le code le plus court qui donne la même performance. Supprime tout code que la mesure n'exige pas.
- Fournis les commandes git à l'utilisateur. N'exécute **aucune** écriture git (branche, commit, tag, merge, push).

## 2. Interdits (chemin critique : `MoveGenerator.legalMoves`, `Bot.negamax`, `Bot.evaluate`, `Board.apply/undo`)

- INTERDIT : `String.format`, concaténation de `String` et `toString()`.
- INTERDIT : pool de threads non borné (`newCachedThreadPool`, `new Thread` en boucle). Borne tout pool à ≤ 4 threads
  (cœurs physiques) et justifie sa taille par une mesure.
- INTERDIT : conversion `String` ↔ `byte[]` / `char[]` superflue.
- INTERDIT : allocation sur le tas sans justification chiffrée : pas de `new Position`, `new ArrayList`, `List.of`,
  `stream()`, ni boxing (`Integer`, `Double`). Chiffre les octets alloués avec `bench/Bench.java`.
- INTERDIT : copier le plateau à chaque nœud du minimax.
- INTERDIT : changer le coup choisi par le bot. Contrôle : la somme de contrôle `b4-a5 a7-b6 d4-c5`
  (profondeur 6, 3 coups) doit rester identique.
- INTERDIT : ajouter du code, un fichier ou une dépendance qui ne sert pas l'optimisation demandée.
- INTERDIT : annoncer un gain sans baseline mesurée **dans la même session** (la machine dérive de ≈ 10 % d'une
  session à l'autre).
- Le code de départ viole ces règles : c'est la baseline mesurée. Tout code ajouté ou modifié doit les respecter.
  Exceptions connues, hors chemin critique : `Board.toString`, `Move.toString`, `Main.parse` (affichage et saisie).
  Violations mesurées, à corriger : `stream()` dans `legalMoves`, `newCachedThreadPool` dans `Api`.

## 3. Justification empirique

- Formule chaque proposition d'optimisation sous la forme **Hypothèse d'impact matériel | Commande de vérification**.
- Exemple : « Supprimer `Board.copy` retire 11,6 % du CPU, donc gain ≤ ×1,13 | `python3 mesures.py apres`,
  puis comparer à `resultats/avant.md` ».
- Utilise ces commandes :
  - temps, mémoire, charge : `python3 mesures.py <nom>` ;
  - où le temps passe : `./profile.sh <nom>` ;
  - comportement : `mvn test`.
- Garde un changement si le gain dépasse **10 %** (bruit ≈ 4 %). Sinon annule-le et documente l'échec avec les chiffres.
- Fais **un seul changement à la fois**, puis mesure.
- Optimise la mémoire **avant** la concurrence : le bot alloue ≈ 4 Go/s et un parallélisme sur ce code plafonne à
  ≈ ×1,6 en débit (mesuré).

## 4. Format des réponses

- Écris des injonctions courtes et vérifiables. Pas de préambule, pas de description inutile.
- Donne les chiffres avec leur unité et leur écart-type. Cite le fichier mesuré.
- Signale toute hypothèse non vérifiée comme **hypothèse**.
