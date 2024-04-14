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