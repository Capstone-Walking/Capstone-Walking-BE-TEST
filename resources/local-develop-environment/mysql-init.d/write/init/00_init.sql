CREATE
    USER 'walking-local'@'localhost' IDENTIFIED BY 'walking-local';
CREATE
    USER 'walking-local'@'%' IDENTIFIED BY 'walking-local';

GRANT ALL PRIVILEGES ON *.* TO
    'walking-local'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO
    'walking-local'@'%';

CREATE USER 'walking-repl'@'%' IDENTIFIED WITH mysql_native_password  BY 'walking-repl';
GRANT REPLICATION SLAVE ON *.* TO 'walking-repl'@'%';
FLUSH PRIVILEGES;

CREATE
    DATABASE api DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;