# InTouch

#### Generic Rest API based on AKKA HTTP

The main idea of this project is to develop a generic API using conventions and configurtions over custom developments to expose with control and security other services like DB with a simple configuration steps.


    http --verify=no -a MyName:p4ssw0rd GET https://localhost:8080/v2/dbService1/test/17 v==value1 v==value2 v==value3 v==value4 InTouch-Debug:true
    http --verify=no -a MyName:p4ssw0rd GET https://localhost:8080/v2/dbService1/test/17 v==value1 v==value2 v==value3 v==value4

    curl --insecure --user MyName:p4ssw0rd --header "InTouch-Debug:true" https://127.0.0.1:8080/v2/dbService1/test/17?v=value1&v=value2&v=value3&v=value4


##### URL Anatomy

> https://localhost:8080/v2/dbService1/test/17

| VALUE            | DESCRIPTION                                                                                                                 |
|-----------------:|-----------------------------------------------------------------------------------------------------------------------------|
| https            | Protocol, always HTTP over SSL to protect your data                                                                         |
| localhost        | The API host                                                                                                                |
| 8080             | The port used to service the API                                                                                            |
| v2               | The version of the API for an easy upgrade without brake current implementation. A "v" (lower case) followed by an Integer  |
| dbService1       | The service that should respond the request. Like the DB connection (configurable)                                          |
| test             | The Entity ID, the name of the object requested.The object to retrieve, insert, update, etc . . .  (configurable)           |
| 17/. . .         | The remaining URL segments will be converted to parameters with the names: urlParam0, urlParam1, . . . (by convention)      |
| Query Parameters | The parameters specified in the query could be replaced in the services requests. Like named parameters in the query string |


### Included example

To show the use of this framework it is included a database schema, test data and the API documentation.

The database scripts, JDBC driver and the service configuration is based on a MySQL 8.0.12 instance.

You could change the database engine making the necessary adjustments.



##### API documentation

https://documenter.getpostman.com/view/457563/RWaKTTx3

Since current project is ussing a self-signed SSL certificate, it is necesary to allow NOT secure connections to reach the API.

In a browser you just need to add a security exception to allow the current certificate.

If you are using cURL please add the __--insecure__ flag to the command.

For a different client please review its documentation.

##### Database schema and data

To generate the example schema and populate it with test data you need to execute the follow scripts (in the root folder of the repository) in order:

 1. vs_challenge_schema.sql
 2. populate_data.sql
 
##### Build and execute

Before build the project be sure that the file **InTouch/src/main/resources/application.conf** has the correct configuration: port, database connections and queries mainly.

To build the project, on the root folder run:

    sbt assembly
 
This command should create a fat-jar with the follow name:

    InTouch/target/scala-2.11/InTouch-1.0-0000.jar
    
(In environments, like Jenkins, where there is a **BUILD_NUMBER** environment variable, the last four numbers could change)

Execute the API services with this command:

    java -jar InTouch-1.0-0000.jar

You can test if the service is running opening this page in a web browser:

    https://localhost:9090/v1/vsChallenge/ticketsByEvent/162

(change the host and port properly)

If you are requested for an user and a password type:

**USER:** nomatter

**PASSWORD:** p4ssw0rd

You should add an exception in your browser to allow use SSL layer with a self-signed certificate.

#### Database Schema

![Database Schema](https://raw.githubusercontent.com/lalosam/InTouch/master/vs_challenge_diagram.png)

  
