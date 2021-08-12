## How to run the code
### Testing
```$ sbt test```
#### Environment
Note the embedded cassandra used in URLRoutesSpec requires Java8

### Running the App
#### Spin up cassandra (Required)
```$ docker-compose up```
#### Run the App
```$ sbt run```
#### Routes
Referer to [postman collection](/trex.postman_collection.json)

### Build App docker
```$ sbt docker:publishLocal```
