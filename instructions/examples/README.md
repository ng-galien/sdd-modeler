# Exemples SDD — sdd-modeler

Ce dossier contient des exemples concrets de modèles SDD et du DDL SQL associé.
Ils servent de base pour tester et illustrer le comportement de sdd-modeler.

## 1. Modèle de commande e-commerce (State-Driven Design)

Cet exemple reprend le cas utilisé dans l'article du blog :
"SDD en SQL : modéliser les états plutôt que des statuts".

- **Modèle YAML** : `orders-sdd-model.yaml`
  - Décrit une entité `order` et ses états :
    - `pending` (initial)
    - `paid`
    - `cancelled` (depuis `pending` ou `paid`)
    - `refunded`
  - Déclare aussi des extensions (`order_paid_extensions`, `order_cancelled_extensions`) et
    des projections (`order_state_intervals`, `current_order_states`).

- **DDL SQL** : `orders-sdd-ddl.sql`
  - Contient les tables et vues correspondantes :
    - `orders` : entité neutre (id, client, montant initial, created_at).
    - `order_pending`, `order_paid`, `order_cancelled`, `order_refunded` : tables d'états.
    - `canceled_source` : table de mapping pour la transition "cancel" depuis `pending` ou `paid`.
    - `order_paid_extensions`, `order_cancelled_extensions` : tables d'extensions non décisionnelles.
    - `order_state_intervals`, `current_order_states` : vues pour l'historique et l'état courant.

- **Diagramme Mermaid** : `orders-sdd-diagram.mmd`
  - Visualisation du graphe d'états sous forme de diagramme Mermaid (stateDiagram-v2).
  - Généré avec : `./gradlew :state-modeler-cli:run --args="diagram orders-sdd-model.yaml -o orders-sdd-diagram.mmd"`

## Utilisation prévue

À terme, sdd-modeler devra être capable de :

1. Charger `orders-sdd-model.yaml` via l'API core (`ModelLoader`).
2. Valider le modèle (graphe d'états, projections, etc.).
3. Générer un plan SQL puis un DDL PostgreSQL équivalents à `orders-sdd-ddl.sql`.
4. Générer des diagrammes Mermaid pour visualiser les graphes d'états.

Ce couple YAML + SQL + Diagramme sert donc de **test d'acceptation** implicite pour le projet :

> À partir du YAML, le générateur doit produire un schéma SQL
> structurellement équivalent à `orders-sdd-ddl.sql` et un diagramme
> Mermaid visualisant le graphe d'états.

## Génération des exemples

```bash
# Générer le DDL SQL
./gradlew :state-modeler-cli:run --args="sql instructions/examples/orders-sdd-model.yaml -o instructions/examples/orders-sdd-ddl.sql"

# Générer le diagramme Mermaid
./gradlew :state-modeler-cli:run --args="diagram instructions/examples/orders-sdd-model.yaml -o instructions/examples/orders-sdd-diagram.mmd"
```

