# Audit de performance : pourquoi on optimise, et ce qu'on a fait

Ce document explique la démarche : le problème mesuré sur la version de départ, puis chaque solution testée et
pourquoi. Les chiffres viennent de mesures réelles (`python3 mesures.py` et `./profile.sh`), pas d'estimations.

État actuel : toutes les optimisations ci-dessous sont fusionnées sur `main`, sauf la concurrence (2.3), documentée
comme un échec et jamais fusionnée. La section 4 compare la version de départ à la version finale (mémoire +
alpha-bêta + cache + listes réutilisées).

## 1. Le problème : pourquoi optimiser

Le jeu et le bot sont volontairement écrits sans souci de performance, pour servir de point de départ mesurable
(c'est l'objet du TP). Trois mesures montrent où ça coince — détails complets dans [`resultats/avant.md`](resultats/avant.md)
et [`resultats/profil-avant.md`](resultats/profil-avant.md).

**Le bot est lent.** Une partie de 3 coups à profondeur 6 prend **1,355 s** en moyenne (écart-type 0,043 s, soit 3,2 %,
donc une mesure fiable). Une seule recherche à cette profondeur dure 403 ms.

**Il alloue énormément de mémoire pour ça.** Une recherche à profondeur 6 alloue **1 969,8 Mo**. Un seul appel à
`legalMoves` (qui liste les coups possibles) alloue déjà 4 992 octets. Pourtant, les pauses du ramasse-miettes ne
totalisent que 9 ms sur toute la mesure (10 pauses) : **ce n'est pas le nettoyage qui coûte cher, c'est la création**
de tous ces petits objets.

**Le profil CPU dit où.** Sur les 62,8 % du temps passés dans `Bot.chooseMove` :
- `MoveGenerator.legalMoves` à lui seul pèse 47,2 % du CPU total, soit **75 % du temps du bot** ;
- `Board.copy` pèse 11,6 % : le plateau entier (un tableau `Piece[10][10]`) est recopié à chaque position explorée,
  alors que le minimax en explore des dizaines de milliers.

**Le profil d'allocations confirme.** Sur les octets alloués : 51,5 % sont des objets `Position`, créés à la volée
(un nouveau `Position` à chaque case regardée), et 88,6 % du total est alloué sous `legalMoves`.

**Conclusion du diagnostic** : le goulet n'est pas le calcul lui-même (l'évaluation d'une position, `Bot.evaluate`, ne
pèse que 2,5 %), c'est la façon dont le code représente une position et génère les coups : trop de petits objets,
trop de copies.

**Le serveur a la même limite, sous un autre angle.** `Api` répond à `GET /api/move`, qui lance une recherche du bot.
Testé avec Vegeta à débit croissant : jusqu'à 30 requêtes/s, tout est servi (p99 ≤ 170 ms). À 40 requêtes/s, le serveur
plafonne à ≈ 28 requêtes servies par seconde et la latence médiane grimpe à 6,8 s : chaque requête coûte cher (la même
recherche lente), donc le serveur sature vite.

## 2. Les solutions mises en place

**Méthode** : un changement à la fois. Après chacun, on vérifie que le bot **joue les mêmes coups** qu'avant (le
comportement ne doit pas changer), puis on mesure avec le même protocole que la baseline.

### 2.1 Mémoire : ne plus copier le plateau

**Pourquoi celui-là en premier** : le profil désignait exactement deux lignes — `Board.copy` (11,6 % du CPU) et les
objets `Position` créés par `legalMoves` (51,5 % des octets alloués). C'est le changement le plus sûr : il ne touche
pas à la stratégie du bot, seulement à la façon dont une position est représentée et manipulée.

**Ce qui a changé** : `Board` ne copie plus le plateau entier à chaque coup exploré. Un coup est joué en place
(`Board.apply`) puis annulé (`Board.undo`) une fois la branche explorée — le plateau lui-même passe d'un tableau à
deux dimensions à un tableau plat. `legalMoves` ne crée plus d'objet `Position` pour les cases qui ne contiennent pas
une pièce du joueur en train de jouer.

**Résultat mesuré** (même session que la baseline, secteur branché, Firefox fermé) :

| Mesure | Avant | Après | Gain |
|---|---|---|---|
| Temps complet (3 coups, profondeur 6) | 1,508 s ± 0,050 | **1,284 s ± 0,040** | ×1,17 |
| Une recherche (profondeur 6) | 425 ms | 352 ms | ×1,21 |
| Mémoire allouée par recherche | 1 971,6 Mo | **1 379,3 Mo** | −30 % |
| Mémoire par appel de `legalMoves` | 4 992 octets | 3 072 octets | −38 % |

Le bot joue exactement les mêmes coups (vérifié sur 9 000 positions tirées au hasard, profondeurs 1 à 5, et sur la
somme de contrôle `b4-a5 a7-b6 d4-c5`). Le gain correspond à ce que le profil annonçait : supprimer `Board.copy`
(11,6 % du CPU) ne pouvait pas donner plus de ×1,13 environ ; on mesure ×1,17, un peu plus grâce aux `Position` en
moins. Ce qui reste alloué (1 379 Mo) vient des objets `Position`, `Piece`, `Move` et `ArrayList` toujours créés à
chaque coup généré : c'est la suite logique de cet axe, pas encore faite.

### 2.2 Élagage alpha-bêta

**Pourquoi** : le bot explore *toutes* les branches, même celles qui ne peuvent plus changer le résultat (si
l'adversaire a déjà un meilleur coup ailleurs, inutile de creuser). L'alpha-bêta coupe ces branches sans jamais
changer le coup choisi — contrairement à l'optimisation mémoire, celle-ci réduit le *nombre* de positions explorées,
pas leur coût unitaire. C'est la deuxième étape naturelle pour un minimax.

**Ce qui a changé** : `Bot.negamax` reçoit deux bornes, `alpha` (le meilleur score déjà garanti) et `beta` (le seuil
au-delà duquel l'adversaire évitera cette position). Dès que `alpha >= beta`, les coups restants de cette branche ne
sont plus explorés.

**Résultat mesuré** (`python3 mesures.py`, secteur branché, Firefox fermé) :

| Mesure | Avant (baseline) | Après | Gain |
|---|---|---|---|
| Temps complet (3 coups, profondeur 6) | 1,355 s ± 0,043 | **0,137 s ± 0,015** | ×9,9 |
| Une recherche, JVM chauffée | 403 ms | **11 ms** | ×37 |
| Mémoire allouée par recherche | 1 969,8 Mo | **27,6 Mo** | ÷71 |
| Serveur à 40 req/s : requêtes servies | 27,7/s (98 % de succès) | **40,0/s (100 %)** | plus de saturation |

Mêmes coups vérifiés de la même façon (9 000 positions, même somme de contrôle). L'écart entre ×9,9 (temps complet) et
×37 (une recherche) s'explique : le démarrage de la JVM à lui seul coûte ≈ 35 ms, incompressible, qui pèse
proportionnellement plus sur un temps devenu très court. La mémoire par appel de `legalMoves` (4 992 octets) n'a pas
changé : l'alpha-bêta réduit le nombre de positions visitées, pas le coût de chacune — les deux optimisations sont
complémentaires, pas redondantes.

### 2.3 Échec constructif : paralléliser les coups de départ (testé, non retenu)

**Hypothèse** : le bot explore 9 coups de départ indépendants les uns des autres. Avec un pool de threads (un par
cœur), on espérait diviser le temps par ~4 (le nombre de cœurs physiques de la machine).

**Ce qui a été fait** : `Bot.chooseMove` lance un pool de threads fixe (`availableProcessors()`, soit 8 threads
logiques) et confie un coup de départ à chaque thread.

**Résultat mesuré** (baseline et version parallèle mesurées à la suite, même session) :

| Mesure | Avant (séquentiel) | Après (8 threads) | Effet |
|---|---|---|---|
| Temps complet | 1,514 s ± 0,055 | 1,305 s ± 0,072 | ×1,16 (×4 espéré) |
| CPU total consommé (user + system) | 2,46 s | **8,53 s** | 3,5 fois plus |
| Mémoire totale allouée (tous threads) | ≈ 5 050 Mo | ≈ 5 062 Mo | inchangée |
| Serveur à 30 req/s : requêtes servies | 27,3/s | 26,0/s | pas mieux |

**Pourquoi ça n'a pas marché, avec preuves** :
1. La mémoire totale allouée est identique avant/après : on répartit le même travail sur plusieurs threads, on ne le
   réduit pas.
2. Le vrai plafond est la mémoire, pas le calcul. En lançant plusieurs copies **indépendantes** du bot séquentiel en
   même temps (aucun partage entre elles), le débit total ne dépasse jamais ×1,65 (testé à 2, 4 et 8 copies) : la
   machine ne peut pas faire mieux, avec ou sans parallélisme dans le code. Espérer ×4 était donc impossible.
3. Sous charge (serveur à 30-40 requêtes/s), chaque requête lance son propre pool de 8 threads : quand les cœurs sont
   déjà occupés par d'autres requêtes, il n'y a rien à gagner.

Deux hypothèses initiales ont été écartées par la mesure : ce n'est pas le ramasse-miettes (9 ms de pause avant, 13 ms
après, négligeable dans les deux cas), et ce n'est pas un déséquilibre entre les 9 coups de départ (ils prennent
chacun entre 36 et 73 ms, ce qui autoriserait au moins ×4,7).

**Décision : non retenu.** Ce qui fonctionne quand même — la latence d'une seule recherche baisse (×1,9 à chaud) — ne
compense pas le coût en CPU pour un gain de capacité nul.

**Leçon retenue** : paralléliser un code qui alloue beaucoup ne sert à rien tant que les allocations elles-mêmes ne
sont pas réduites (axe Mémoire, 2.1). L'ordre des optimisations compte : réduire le travail avant de le répartir. Les
deux optimisations suivantes (2.4 et 2.5) appliquent cette leçon : elles portent sur ce qui reste après l'alpha-bêta,
pas sur la répartition du travail entre threads.

### 2.4 Cache : table de transposition

**Pourquoi** : plusieurs suites de coups différentes peuvent mener à la même position (une prise dans un ordre ou
dans l'autre, par exemple). L'alpha-bêta ne le sait pas et la recalcule à chaque fois. Un cache qui identifie une
position déjà vue évite ce recalcul, sans changer le résultat.

**Ce qui a été fait** : chaque position est identifiée par un hachage de Zobrist (une clé aléatoire par case et par
type de pièce, combinées par XOR). Une table (tableaux primitifs `long[]`/`double[]`/`short[]`, taille fixée à
l'ouverture) retient, pour une position et une profondeur restante données, sa valeur exacte ou une borne
(inférieure/supérieure) selon la fenêtre alpha-bêta utilisée au moment du calcul.

**Résultat mesuré** (`python3 mesures.py`, secteur branché, Firefox fermé, profondeur 10 — à cette profondeur les
positions répétées sont plus nombreuses et le gain se voit mieux qu'à profondeur 6) :

| Mesure | Avant (mémoire + alpha-bêta) | Après (+ cache) | Gain |
|---|---|---|---|
| Temps complet (3 coups, profondeur 10) | 1,099 s ± 0,052 | **0,589 s ± 0,017** | ×1,87 |
| Une recherche, JVM chauffée | 282 ms | **99 ms** | ×2,85 |
| Mémoire allouée par recherche | 1 460,8 Mo | **392,7 Mo** | ÷3,7 |
| Serveur à 40 req/s : p50 | 46 ms | **24 ms** | ×1,9 |

Mêmes coups vérifiés (9 000 positions, même somme de contrôle). Une variante plus simple, qui ne retient que les
valeurs exactes (sans les bornes inférieure/supérieure), a aussi été mesurée : elle ne donne que ×1,27, contre ×1,8
pour la version complète — gardée malgré ses 10 lignes de plus, l'écart de gain le justifie.

### 2.5 Mémoire (suite) : listes de travail réutilisées

**Pourquoi** : `legalMoves` créait deux nouvelles listes (`ArrayList`) à chaque pièce examinée, pour explorer ses
prises possibles. Ces listes sont vidées puis reconstruites à chaque appel : les réutiliser d'une pièce à l'autre
évite l'allocation sans changer la logique.

**Ce qui a été fait** : les listes de travail (`path`, `captured`) sont créées une seule fois par appel à
`legalMoves`, puis vidées (`clear()`) et réutilisées pour chaque pièce du joueur, au lieu d'en créer une nouvelle
paire par pièce.

**Résultat mesuré** (`python3 mesures.py`, secteur branché, Firefox fermé, profondeur 10) :

| Mesure | Avant (+ cache) | Après (+ listes réutilisées) | Gain |
|---|---|---|---|
| Temps complet (3 coups, profondeur 10) | 0,579 s ± 0,020 | **0,508 s ± 0,017** | ×1,14 |
| Mémoire par appel de `legalMoves` | 3 072 octets | **2 008 octets** | −35 % |
| Une recherche, JVM chauffée | 87 ms | 90 ms | aucun gain mesurable |

Gain modeste (12 %, juste au-dessus du seuil de bruit de 10 %) et honnêtement signalé comme tel : sur une recherche
déjà chaude, le temps ne bouge pas du tout — le gain ne se voit que sur la commande complète (démarrage JVM + JIT
inclus), et la mémoire baisse bien comme prévu. Mêmes coups vérifiés (9 000 positions, même somme de contrôle).

## 3. Ce qui reste ouvert

- **Mémoire (fin de l'axe)** : les objets `Position`/`Piece`/`Move` restants pourraient être remplacés par des
  entiers primitifs. Non fait : le gain n'a pas été mesuré et le code perdrait en lisibilité pour un bénéfice
  incertain.
- **Concurrence** : documentée comme un échec (2.3), jamais reprise. La leçon (réduire les allocations d'abord) est
  maintenant appliquée, mais reprendre l'essai avec le code actuel n'a pas été fait.

## 4. Synthèse finale : la version de départ contre la version finale

Toutes les optimisations gardées (mémoire, alpha-bêta, cache, listes réutilisées) mesurées ensemble contre la version
de départ, dans la même session, mêmes conditions (secteur branché, Firefox fermé) : détails dans
[`resultats/final.md`](resultats/final.md).

| Mesure | Avant (version de départ) | Après (version finale) | Gain |
|---|---|---|---|
| Temps complet, profondeur 6 | 1,317 s ± 0,042 | **0,108 s ± 0,006** | ×12,2 |
| Temps complet, profondeur 8 | 37,8 s (1 mesure) | **0,174 s ± 0,012** | ×217 |
| Mémoire allouée par recherche (profondeur 6) | 1 969,8 Mo | **6,9 Mo** | ÷285 |
| Serveur à 40 req/s : requêtes servies | 26,7/s (95 % de succès) | **39,9/s (100 %)** | plus de saturation |
| Serveur à 40 req/s : latence médiane | 7 088 ms | **23 ms** | ×308 |

Mêmes coups joués dans les deux versions : `b4-a5 a7-b6 d4-c5`.

**Pourquoi ×12 à profondeur 6 mais ×217 à profondeur 8** : à profondeur 6, la version finale est si rapide (≈ 108 ms)
que le démarrage de la JVM (≈ 35 ms, incompressible) en représente une part importante et écrase une partie du gain
mesuré. À profondeur 8, le calcul domine largement ce coût fixe, et l'écart algorithmique (moins de positions
explorées, chacune moins coûteuse) apparaît en entier.

**Pourquoi le gain n'est pas le produit des gains individuels** (×1,17 × ×9,9 × ×1,87 × ×1,14 ≈ ×25, très supérieur à
×12,2 mesuré à profondeur 6) : les optimisations ne sont pas indépendantes. L'alpha-bêta réduit déjà tellement le
nombre de positions explorées que le cache et les listes réutilisées, mesurés ensuite sur un code qui alloue déjà
beaucoup moins, ont moins de travail à économiser que si on les avait mesurés seuls sur la version de départ.

**Ce qui a le plus compté** : les deux optimisations qui réduisent le *nombre* de positions calculées (alpha-bêta
×9,9, cache ×1,87) dominent largement celles qui réduisent le *coût* de chaque position (mémoire ×1,17, listes
×1,14). Pour un minimax, couper des branches rapporte plus que rendre chaque nœud un peu moins cher.

**Limites de cette mesure** : la baseline à profondeur 8 n'a été mesurée qu'une seule fois (37,8 s par mesure rend
15 répétitions coûteuses en temps de session ; l'écart avec la version finale est de toute façon bien supérieur au
bruit habituel de la machine, ≈ 10 %). La concurrence (2.3) est exclue de la version finale : c'est un échec
documenté, pas une optimisation gardée.
