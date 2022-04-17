# Test-tx

## คำสั่งในการสร้าง db server

```shell
docker run  --name test-tx-sql-8 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0.28

docker run  --name test-tx-maria-10 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mariadb:10.7

docker run --name test-tx-postgres-14 -p 5432:5432 -e POSTGRES_PASSWORD=root -d postgres:14

docker run -itd --name test-tx-db2 --privileged=true -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=P@ssw0rd -e DBNAME=testtx ibmcom/db2:11.5.7.0

docker run --name test-tx-sqlserver -e "ACCEPT_EULA=Y" -e "P@ssw0rd" -e "MSSQL_PID=Express" -p 1433:1433 -d mcr.microsoft.com/mssql/server:2019-latest 
```

## การสร้าง Table

สามารถสร้างโดยการแก้ค่า properties : spring.jpa.hibernate.ddl-auto เป็น update

## ตั้งค่าเริ่มต้น

ทำการ init ค่าให้กับ table account ด้วย sql จากไฟล์ : initscript.sql

## สิ่งที่ต้อง Config

ต้องไปแก้ไขค่าในไฟล์ application.properties ในส่วนที่เกี่ยวกับ db ทั้งหมด 