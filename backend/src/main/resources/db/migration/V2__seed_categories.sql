-- Seed Categories for RentHub Marketplace

-- Seed Root Categories
INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
VALUES ('Cameras & Photography', 'cameras-photography', 'Professional cameras, DSLRs, mirrorless, and photography accessories.', 1, TRUE, NULL);

INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
VALUES ('Power Tools', 'power-tools', 'Drills, saws, sanders, and other heavy-duty construction tools.', 2, TRUE, NULL);

INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
VALUES ('Outdoor & Camping', 'outdoor-camping', 'Tents, backpacks, sleeping bags, stoves, and outdoor gear.', 3, TRUE, NULL);

INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
VALUES ('Audio & Video', 'audio-video', 'Microphones, speakers, mixers, projectors, and audio production accessories.', 4, TRUE, NULL);

-- Seed Subcategories under Cameras & Photography
INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
SELECT 'DSLR & Mirrorless Cameras', 'dslr-mirrorless', 'High-end DSLR and mirrorless camera bodies.', 1, TRUE, id
FROM equipment_categories WHERE slug = 'cameras-photography';

INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
SELECT 'Lenses & Optics', 'lenses-optics', 'Prime lenses, zoom lenses, telephotos, and filters.', 2, TRUE, id
FROM equipment_categories WHERE slug = 'cameras-photography';

-- Seed Subcategories under Power Tools
INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
SELECT 'Drills & Drivers', 'drills-drivers', 'Cordless drills, impact drivers, hammer drills.', 1, TRUE, id
FROM equipment_categories WHERE slug = 'power-tools';

INSERT INTO equipment_categories (name, slug, description, display_order, is_active, parent_id)
SELECT 'Saws & Cutters', 'saws-cutters', 'Circular saws, miter saws, jigsaws, reciprocating saws.', 2, TRUE, id
FROM equipment_categories WHERE slug = 'power-tools';
