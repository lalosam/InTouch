# InTouch

#### Generic Rest API based on AKKA HTTP

The main idea of this project is to develop a generic API using conventions over custom developments to expose other services like DB with a simple configuration steps.


    http --verify=no -a MyName:p4ssw0rd GET https://localhost:8080/v1/servA/EntityID/A/B/C/D/E?a=123 InTouch-Debug:true

    curl --insecure --user MyName:p4ssw0rd --header "InTouch-Debug:true" https://127.0.0.1:8080/v1/servA/EntityID?a=123&a=345&b=333333&urlParam0=99125654645645
