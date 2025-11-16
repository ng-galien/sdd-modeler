-- Exemple de schéma SQL SDD pour une commande e-commerce
-- issu de l'article "SDD en SQL : modéliser les états plutôt que des statuts".

-- Table principale pour l'entité commande (attributs neutres, non porteurs d'état)
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,  -- Référence à un client (externe au modèle)
    total_amount DECIMAL(10, 2) NOT NULL,  -- Montant total initial (immuable)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()  -- Date de création de la commande
);

-- Table pour l'état initial : Pending (en attente)
-- Précédent : Aucun (état initial)
CREATE TABLE order_pending (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- Timestamp unique
    -- Attributs spécifiques à Pending (tous non nuls, immuables)
    pending_reason TEXT NOT NULL  -- Raison de l'attente (ex. : validation en cours)
);

-- Table pour l'état Paid (payée)
-- Précédent : Uniquement Pending (référence directe)
CREATE TABLE order_paid (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- Timestamp unique
    previous_pending_id INTEGER NOT NULL REFERENCES order_pending(id),  -- Référence à l'état précédent
    -- Attributs spécifiques à Paid (tous non nuls, immuables)
    payment_method TEXT NOT NULL,  -- Méthode de paiement
    paid_amount DECIMAL(10, 2) NOT NULL  -- Montant payé
);

-- Table pour l'état Cancelled (annulée)
-- Précédents possibles : Pending OU Paid (via table canceled_source pour le "OR")
CREATE TABLE order_cancelled (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- Timestamp unique
    previous_source_id INTEGER NOT NULL REFERENCES canceled_source(id),  -- Référence à la source précédente
    -- Attributs spécifiques à Cancelled (tous non nuls, immuables)
    cancel_reason TEXT NOT NULL  -- Raison de l'annulation
);

-- Table de mapping pour transitions vers Cancelled (représente le "OR" des précédents)
-- Restreint les transitions autorisées via structure déclarative
CREATE TABLE canceled_source (
    id SERIAL PRIMARY KEY,
    pending_state_id INTEGER REFERENCES order_pending(id),  -- Référence à Pending (nullable si non applicable)
    paid_state_id INTEGER REFERENCES order_paid(id),  -- Référence à Paid (nullable si non applicable)
    CHECK (
        (pending_state_id IS NOT NULL AND paid_state_id IS NULL) OR
        (pending_state_id IS NULL AND paid_state_id IS NOT NULL)
    )  -- Vérifie qu'exactement une des deux références est non nulle
    -- Note : Cette contrainte CHECK assure l'intégrité structurelle de la table de mapping, en complément de la logique applicative.
);

-- Table pour l'état Refunded (remboursée)
-- Précédent : Uniquement Paid (référence directe)
CREATE TABLE order_refunded (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- Timestamp unique
    previous_paid_id INTEGER NOT NULL REFERENCES order_paid(id),  -- Référence à l'état précédent
    -- Attributs spécifiques à Refunded (tous non nuls, immuables)
    refund_amount DECIMAL(10, 2) NOT NULL,  -- Montant remboursé
    refund_method TEXT NOT NULL  -- Méthode de remboursement
);

-- Tables d'extension pour enrichissements non décisionnels (optionnels, mutables)
-- Exemple pour état Paid : notes supplémentaires (ne modifient pas l'état)
CREATE TABLE order_paid_extensions (
    paid_id INTEGER PRIMARY KEY REFERENCES order_paid(id),
    additional_notes TEXT,  -- Optionnel, mutable
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()  -- Traçabilité des mises à jour
);

-- Exemple pour état Cancelled : détails administratifs (ne modifient pas l'état)
CREATE TABLE order_cancelled_extensions (
    cancelled_id INTEGER PRIMARY KEY REFERENCES order_cancelled(id),
    admin_comments TEXT,  -- Optionnel, mutable
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()  -- Traçabilité des mises à jour
);

-- Vue pour dériver les intervalles temporels (début/fin d'états)
-- Permet de calculer la fin d'un état comme le début du suivant
CREATE VIEW order_state_intervals AS
SELECT 
    o.id AS order_id,
    'PENDING' AS state_type,
    op.created_at AS start_at,
    COALESCE(
        (SELECT MIN(created_at) FROM order_paid WHERE previous_pending_id = op.id),
        (SELECT MIN(oc.created_at) FROM order_cancelled oc
         JOIN canceled_source cs ON cs.id = oc.previous_source_id
         WHERE cs.pending_state_id = op.id),
        NULL  -- Ouvert si pas de suivant
    ) AS end_at
FROM orders o
JOIN order_pending op ON op.order_id = o.id

UNION ALL

SELECT 
    o.id AS order_id,
    'PAID' AS state_type,
    opa.created_at AS start_at,
    COALESCE(
        (SELECT MIN(created_at) FROM order_refunded WHERE previous_paid_id = opa.id),
        (SELECT MIN(oc.created_at) FROM order_cancelled oc
         JOIN canceled_source cs ON cs.id = oc.previous_source_id
         WHERE cs.paid_state_id = opa.id),
        NULL  -- Ouvert si pas de suivant
    ) AS end_at
FROM orders o
JOIN order_paid opa ON opa.order_id = o.id

UNION ALL

SELECT 
    o.id AS order_id,
    'CANCELLED' AS state_type,
    oc.created_at AS start_at,
    NULL AS end_at  -- État final, scellé (pas de transitions sortantes)
FROM orders o
JOIN order_cancelled oc ON oc.order_id = o.id

UNION ALL

SELECT 
    o.id AS order_id,
    'REFUNDED' AS state_type,
    orf.created_at AS start_at,
    NULL AS end_at  -- État final, scellé (pas de transitions sortantes)
FROM orders o
JOIN order_refunded orf ON orf.order_id = o.id;

-- Vue pour l'état actif courant par commande
-- Basée sur l'absence de suivant (end_at IS NULL)
CREATE VIEW current_order_states AS
SELECT 
    order_id,
    state_type,
    start_at
FROM order_state_intervals
WHERE end_at IS NULL;
