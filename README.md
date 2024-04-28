### Apache Flink Examples
    
For userClickEvents:    
```bash
cd userClickEvents
```
Build:
```bash
mvn clean install
```
Submit a Flink job locally:
```bash
flink run target/task-1.2.2-SNAPSHOT.jar
```
![Running Jobs](./img/running_jobs.png)
    
![User Click Events](./img/user_click_events.png)


