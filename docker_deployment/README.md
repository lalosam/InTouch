## Run InTouch in docker containers

### Prerequsites

Docker engine must be installed in the machine

Clone InTouch github repository.

    git clone
    cd InTouch 


### Create local docker image with InTouch code using SBT

    sbt docker:publishLocal
    
One image with two different tags should be created:

1. lalosam/intouch:1.0
2. lalosam/intouch:latest

Validate they are there

    docker images

### Create local docker image with MYSQL test database

    cd docker_deployment
    docker build -f mysql.df -t lalosam/mysql-vs:1.0 .
    
### Run a container with the sample database

    docker run -d --name mysqlvs -e MYSQL_ROOT_PASSWORD=rootroot -p 3306:3306 -p 33060:33060 lalosam/mysql-vs:1.0

### Create sample schema an load sample data

Wait until command

    docker container ls
    
show a __healthy__ status on mysqlvs container

> 486cc606758c        lalosam/mysql-vs:1.0     "docker-entrypoint.s…"   3 minutes ago       Up 3 minutes __(healthy)__   0.0.0.0:3306->3306/tcp, 0.0.0.0:33060->33060/tcp   mysqlvs

then execute 

    docker exec mysqlvs sh -c "/scripts/create_data.sh"
    
You could get warning messages complaining with the use of the password in the command line, just ignore them.

### Run a container with the InTouch API

    docker run -d -p 9090:9090 --link mysqlvs:mysqlvs --rm --name intouch lalosam/intouch:latest

### Try the API

Open a browser and visit 

    https://localhost:9090/v1/vsChallenge/events
    
 Your browser should complain the certificate wasn sign by a trusted entity, you will require to create an exception to allow SSL connection to the API.
 
 
 ## Run InTouch in docker containers using docker-compose
 
docker-compose must be installed

    cd docker_deployment
    docker-compose up -d --build

 ### Create sample schema an load sample data
 
 Wait until command
 
     docker container ls
     
 show a __healthy__ status on mysqlvs container
 
 > 486cc606758c        lalosam/mysql-vs:1.0     "docker-entrypoint.s…"   3 minutes ago       Up 3 minutes __(healthy)__   0.0.0.0:3306->3306/tcp, 0.0.0.0:33060->33060/tcp   mysqlvs
 
 then execute 
 
     docker exec mysqlvs sh -c "/scripts/create_data.sh"
     
 You could get warning messages complaining with the use of the password in the command line, just ignore them.