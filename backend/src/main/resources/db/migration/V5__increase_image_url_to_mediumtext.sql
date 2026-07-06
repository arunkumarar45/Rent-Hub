-- Modify image_url columns to MEDIUMTEXT to support long URLs or base64 encoded data URIs
ALTER TABLE equipment MODIFY COLUMN image_url MEDIUMTEXT;
ALTER TABLE equipment_images MODIFY COLUMN image_url MEDIUMTEXT;
