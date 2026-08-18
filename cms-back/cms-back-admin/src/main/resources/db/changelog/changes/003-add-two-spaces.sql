-- 003-add-two-spaces.sql

-- 添加Shopchup空间
INSERT INTO spaces (name, slug, description, sort, status)
SELECT 'Shopchup', 'shopchup', 'Shopchup空间', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM spaces WHERE slug = 'shopchup');

-- 添加物联网空间
INSERT INTO spaces (name, slug, description, sort, status)
SELECT 'Iot', 'iot', '物联网空间', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM spaces WHERE slug = 'iot');