CREATE
    USER 'walking-local'@'localhost' IDENTIFIED BY 'walking-local';
CREATE
    USER 'walking-local'@'%' IDENTIFIED BY 'walking-local';

GRANT ALL PRIVILEGES ON *.* TO
    'walking-local'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO
    'walking-local'@'%';

CREATE
    DATABASE api DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CHANGE MASTER TO MASTER_HOST='walking-mysql', MASTER_PORT=3306, MASTER_USER='repluser', MASTER_PASSWORD='replpw', MASTER_LOG_FILE='walking_log.000003', MASTER_LOG_POS=752; START SLAVE;

--