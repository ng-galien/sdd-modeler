-- Sample Data for Lead CRM
-- This script populates the database with example leads in various states
-- Important: This must be run AFTER the schema has been created

-- First, insert the main Lead entities
INSERT INTO public.leads (id) VALUES
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222'),
    ('33333333-3333-3333-3333-333333333333'),
    ('44444444-4444-4444-4444-444444444444'),
    ('55555555-5555-5555-5555-555555555555'),
    ('66666666-6666-6666-6666-666666666666'),
    ('77777777-7777-7777-7777-777777777777'),
    ('88888888-8888-8888-8888-888888888888'),
    ('99999999-9999-9999-9999-999999999999'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

-- Leads in "NEW" state (5 leads)
INSERT INTO public_states.leads_new (Lead_id, name, email, phone, source) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Alice Martin', 'alice.martin@example.com', '+33 6 12 34 56 78', 'Website'),
    ('22222222-2222-2222-2222-222222222222', 'Bob Dupont', 'bob.dupont@example.com', '+33 6 23 45 67 89', 'LinkedIn'),
    ('33333333-3333-3333-3333-333333333333', 'Claire Bernard', 'claire.bernard@example.com', '+33 6 34 56 78 90', 'Referral'),
    ('44444444-4444-4444-4444-444444444444', 'David Rousseau', 'david.rousseau@example.com', '+33 6 45 67 89 01', 'Trade Show'),
    ('55555555-5555-5555-5555-555555555555', 'Emma Petit', 'emma.petit@example.com', '+33 6 56 78 90 12', 'Cold Call');

-- Leads in "CONTACTED" state (3 leads)
-- Note: These need the previous_new_id from leads that were in NEW state first
-- For simplicity, we'll use sequential IDs assuming the previous inserts generated IDs 1, 2, 3...
-- BUT since we can't easily know the generated IDs without returning them, 
-- and we are just inserting sample data, we will assume the previous_new_id are 1, 2, 3.
-- Ideally we would select them, but for a sample script this is acceptable if run on fresh DB.
INSERT INTO public_states.leads_contacted (Lead_id, previous_new_id, contactedAt, contactedBy, notes) VALUES
    ('66666666-6666-6666-6666-666666666666', 1, '2025-11-20 10:30:00', 'Jean Vendeur', 'Premier contact téléphonique, intéressé par nos services'),
    ('77777777-7777-7777-7777-777777777777', 2, '2025-11-21 14:15:00', 'Marie Commerce', 'Email de suivi envoyé, rendez-vous prévu la semaine prochaine'),
    ('88888888-8888-8888-8888-888888888888', 3, '2025-11-22 09:00:00', 'Jean Vendeur', 'Démo du produit réalisée, feedback très positif');

-- Leads in "QUALIFIED" state (2 leads)
INSERT INTO public_states.leads_qualified (Lead_id, previous_contacted_id, budget, timeline, qualificationNotes) VALUES
    ('99999999-9999-9999-9999-999999999999', 1, 50000.00, 'Q1 2026', 'Budget confirmé, décision prévue en janvier'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 2, 75000.00, 'Q2 2026', 'Besoin urgent, nécessite une proposition détaillée');

-- Leads in "CONVERTED" state (1 lead)
INSERT INTO public_states.leads_converted (Lead_id, previous_qualified_id, convertedAt, contractValue, salesRepId) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, '2025-11-15 16:45:00', 45000.00, 'SR-001');

-- Display summary
SELECT 
    state_type,
    COUNT(*) as count
FROM public_states.Lead_state
GROUP BY state_type
ORDER BY 
    CASE state_type
        WHEN 'NEW' THEN 1
        WHEN 'CONTACTED' THEN 2
        WHEN 'QUALIFIED' THEN 3
        WHEN 'CONVERTED' THEN 4
    END;
