@echo off
docker run -p 3306:3306 --rm --name bbsql -d bbdata-mysql