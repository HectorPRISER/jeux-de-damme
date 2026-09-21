# jeux-de-damme

Jeu de dames internationales (10x10) en Java 21, en mode console.

- Lancer : `mvn -q exec:java`
- Tests : `mvn test`
- Saisie : `b4-c5` (déplacement), `c3-e5-g7` (rafle) ; `coups` liste les coups légaux, `q` quitte.

Règles : prise obligatoire et majoritaire, prise arrière des pions, dames volantes, promotion uniquement si le coup s'achève sur la dernière ligne.
