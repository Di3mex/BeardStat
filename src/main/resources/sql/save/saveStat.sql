INSERT INTO `${PREFIX}_value` values(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `value`= VALUES(`value`);