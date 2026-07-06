-- Increase image_url column length to accommodate long URLs
ALTER TABLE equipment MODIFY COLUMN image_url VARCHAR(2048);
ALTER TABLE equipment_images MODIFY COLUMN image_url VARCHAR(2048);
