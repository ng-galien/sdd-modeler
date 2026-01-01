DO $$
DECLARE
  l_lead uuid;
  l_new_id INTEGER;
  l_contacted_id INTEGER;
  l_qualified_id INTEGER;
BEGIN
  -- Ensure the uuid-ossp extension is available when using uuid_generate_v4
  PERFORM 1 FROM pg_extension WHERE extname = 'uuid-ossp';
  IF NOT FOUND THEN
    EXECUTE 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp"';
  END IF;

  -- Create a few leads and state rows while capturing IDs so foreign keys can be set properly
  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'Alice Martin', 'alice.martin@example.com', '+33 6 12 34 56 78', 'Website')
    RETURNING id INTO l_new_id;

  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'Bob Dupont', 'bob.dupont@example.com', '+33 6 23 45 67 89', 'LinkedIn')
    RETURNING id INTO l_new_id;

  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'Claire Bernard', 'claire.bernard@example.com', '+33 6 34 56 78 90', 'Referral')
    RETURNING id INTO l_new_id;

  -- Example with a contacted lead referencing previous_new_id
  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'David Rousseau', 'david.rousseau@example.com', '+33 6 45 67 89 01', 'Trade Show')
    RETURNING id INTO l_new_id;
  INSERT INTO public_states.leads_contacted (lead_id, previous_new_id, contacted_at, contacted_by, notes)
    VALUES (l_lead, l_new_id, '2025-11-20 10:30:00', 'Jean Vendeur', 'Premier contact téléphonique, intéressé par nos services')
    RETURNING id INTO l_contacted_id;

  -- Another example leading to qualification
  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'Emma Petit', 'emma.petit@example.com', '+33 6 56 78 90 12', 'Cold Call')
    RETURNING id INTO l_new_id;
  INSERT INTO public_states.leads_contacted (lead_id, previous_new_id, contacted_at, contacted_by, notes)
    VALUES (l_lead, l_new_id, '2025-11-21 14:15:00', 'Marie Commerce', 'Email de suivi envoyé, rendez-vous prévu la semaine prochaine')
    RETURNING id INTO l_contacted_id;
  INSERT INTO public_states.leads_qualified (lead_id, previous_contacted_id, budget, timeline, qualification_notes)
    VALUES (l_lead, l_contacted_id, 50000.00, 'Q1 2026', 'Budget confirmé, décision prévue en janvier')
    RETURNING id INTO l_qualified_id;

  -- converted example using previous qualified
  l_lead := uuid_generate_v4();
  INSERT INTO public.leads(id) VALUES (l_lead);
  INSERT INTO public_states.leads_new (lead_id, name, email, phone, source)
    VALUES (l_lead, 'Converted Inc', 'convert@example.com', '+33 6 66 66 66 66', 'Referral')
    RETURNING id INTO l_new_id;
  INSERT INTO public_states.leads_contacted (lead_id, previous_new_id, contacted_at, contacted_by, notes)
    VALUES (l_lead, l_new_id, '2025-11-22 09:00:00', 'Jean Vendeur', 'Démo du produit réalisée, feedback très positif')
    RETURNING id INTO l_contacted_id;
  INSERT INTO public_states.leads_qualified (lead_id, previous_contacted_id, budget, timeline, qualification_notes)
    VALUES (l_lead, l_contacted_id, 75000.00, 'Q2 2026', 'Besoin urgent, nécessite une proposition détaillée')
    RETURNING id INTO l_qualified_id;
  INSERT INTO public_states.leads_converted (lead_id, previous_qualified_id, converted_at, contract_value, sales_rep_id)
    VALUES (l_lead, l_qualified_id, '2025-11-15 16:45:00', 45000.00, 'SR-001');

END $$;

-- Quick summary
SELECT state_type, COUNT(*) as count FROM public_states.lead_state GROUP BY state_type ORDER BY state_type;
