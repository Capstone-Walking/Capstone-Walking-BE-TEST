#!/bin/bash

 until docker exec walking-mysql8 sh -c 'export MYSQL_PWD=root; mysql -u root -e ";"'
 do
     echo "Waiting for mysql_master database connection..."
     sleep 4
 done


 until docker exec walking-mysql8-read sh -c 'export MYSQL_PWD=root; mysql -u root -e ";"'
 do
     echo "Waiting for mysql_slave database connection..."
     sleep 4
 done

 MS_STATUS=`docker exec walking-mysql8 sh -c 'export MYSQL_PWD=root; mysql -u root -e "SHOW MASTER STATUS"'`
 CURRENT_LOG=`echo $MS_STATUS | awk '{print $6}'`
 CURRENT_POS=`echo $MS_STATUS | awk '{print $7}'`

 start_slave_stmt="CHANGE MASTER TO MASTER_HOST='walking-mysql8',MASTER_USER='walking-repl',MASTER_PASSWORD='walking-repl',MASTER_LOG_FILE='$CURRENT_LOG',MASTER_LOG_POS=$CURRENT_POS; START SLAVE;"
 start_slave_cmd='export MYSQL_PWD=root; mysql -u root -e "'
 start_slave_cmd+="$start_slave_stmt"
 start_slave_cmd+='"'
 docker exec walking-mysql8-read sh -c "$start_slave_cmd"

 docker exec walking-mysql8-read sh -c "export MYSQL_PWD=root; mysql -u root -e 'SHOW SLAVE STATUS \G'"