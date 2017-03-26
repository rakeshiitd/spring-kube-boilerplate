# Spring boot kube boilerplate

# Initialize!

  ``` sh
  git clone https://github.com/rakeshiitd/spring-kube-boilerplate.git
  cd spring-boilerplate
  ```
# Setting up private keys for accessing cluster
  ``` sh
  cp .secrets/kube_aws_rsa ~/.ssh/
  ```
# Setting up kubectl to access remote cluster
  ``` sh
  cp .secrets/.kube_config ~/.kube/config
  ```
  - It will make your kubectl to point to remote cluster. Now you will be able to perform all sorts of operations on running cluster right from your local computer 
# Set up kube registry
  Open /etc/hosts file and append following line
  ``` sh
52.77.232.250 kube-registry.kube-system.svc.cluster.local #Added by secure-kube-registry script
  cp .secrets/ca.crt /etc/docker/certs.d/kube-registry.kube-system.svc.cluster.local:31000/ca.crt
  ```

# Docker image build
  - This repo uses ``docker-maven-plugin `` for building docker images 
  - Define docker image prefix-:
   ``` sh
   <properties>
        <java.version>1.8</java.version>
        <docker.image.prefix>kube-registry.kube-system.svc.cluster.local:31000/rak007007</docker.image.prefix>
    </properties>
   ```
   - Image name will be `${docker.image.prefix}/${project.artifactId}`
   - Command to build docker image -
     `sudo mvn package -e docker:build`
   - Command to push docker image with push
      `sudo mvn package -e docker:build -DpushImage`
   - Command to push docker image
       `sudo docker push <imageName>`

# Manging secrets 
 For management of secrets kubernetes provides Seccrets API. You can define secrets in following way-:
 ``` sh
 apiVersion: v1
kind: Secret
metadata:
  name: mysecret
type: Opaque
data:
  PPORT: '8899'
  password: MWYyZDFlMmU2N2Rm
 ```
 
 - creating the secrets
 ``` sh
 kubectl create -f secrets.yml
 ```
# Manging config files-:
For managing configs spring provides ConfigMaps. You can define ConfigMaps like this -: 
``` sh
kind: ConfigMap
apiVersion: v1
metadata:
  creationTimestamp: 2017-03-18T19:14:38Z
  name: example-config
  namespace: default
data:
  PPORT: '8899'
  example2: world
  example3: |-
    property.1=value-1
    property.2=value-2
    property.3=value-3
```
- creating the config map 
 ``` sh
 kubectl create -f config.yml
 ```

# Deployment
- Every application will be deployed to kube cluster using Deployment API of kubernetes and it will use replica set, replication controller strategy has been deprecated and higly not recommneded to use
 ``` sh
 apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: spring-bp
  labels:
    name: spring-bp-service
    version: latest
    visualize: "true"
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        name: spring-bp-service
        app: spring-bp-service
        version: latest
        visualize: "true"
    spec:
      containers:
       - name: spring-bp
         image: kube-registry.kube-system.svc.cluster.local:31000/rak007007/gs-spring-boot
         imagePullPolicy: Always
         ports:
          - name: http
            containerPort: 8899
         readinessProbe:
          failureThreshold: 3
          httpGet:
            path: /healthz
            port: 8899
            scheme: HTTP
          initialDelaySeconds: 5
          periodSeconds: 10
          successThreshold: 1
          timeoutSeconds: 1
        livenessProbe:
          failureThreshold: 3
          httpGet:
            path: /healthz
            port: 8899
            scheme: HTTP
          initialDelaySeconds: 5
          periodSeconds: 10
          successThreshold: 1
          timeoutSeconds: 1
         env:
          - name: PPORT2
            valueFrom:
              secretKeyRef:
                name: mysecret
                key: PPORT
          - name: PPORT
            valueFrom:
              configMapKeyRef:
                name: example-config
                key: PPORT
 ```
 - `name field in labels define the name of the pod and it will act as a selector label for the pod`
 - `replicas define how many load balanced copies you want to run minimum at a given time. According to provided value replica set will make sure that at any given point of time there are two copies of pod running`
 - `strategy: type  should always be RollingUpdate it will help us in maintaining blue green deploymenet without any downtime `
 - `image is the path of your docker image`
 - `imagePullPolicy should be set to always. It will ensure that latest image is being pulled whenever you update deployment`
 - `ports field defines what ports you want to use in container`
 - `readinessProbe and livenessProbe are health check endpoints, if these endpoints are not reachable, kube will kill the pod and run a new pod automatically `
 - env field exposes variables as ENVIRONMENT VARIABLES/CMD ARGUMENTS in container, You cab pull these env variables from Earlier defined secrets and config maps
 - creating the deployment 
 ``` sh
 kubectl create -f deployment.yml
 ```

# Exposing the running service to outside world

```
kind: Service
apiVersion: v1
metadata:
  name: spring-bp
  labels:
    name: spring-bp
    visualize: "true"
spec:
  ports:
    - port: 8899
      targetPort: 8899
  selector:
    name: spring-bp
```
- creating the service:-
``` sh
kubectl create -f service.yml
```

- after exposing the pod using service API other running container can call your service by calling http://<serviceName>:<servicePort>, every request will be load balanced by kube-proxy

# updating the deployment
update deployment.yaml file and run-:
`kubectl update -f deployment-v2.yaml`

# Log managment
- By default all the logs will be forwarded to elasticsearch using fluentdb. You can query over generated logs in kibana and view auto generated logs 
- Alternatively you can view all the logs from kubernetes dashboard and tail -f as well using kubectl

# Interaction with other services and Service Discovery, Service Registry
  Just call the service by it's name and port, request will be automatically load balanced and you dont have to worry about dynamic assignment and auto scaling of the services
  ex- http://<serviceName>:<servicePort>

# Demo of boilerplate
- Clone this repo
- Setup keys
- Setup kubectl
- setup kube registry
-  run `sudo mvn package -e docker:build`
-  sudo docker push `kube-registry.kube-system.svc.cluster.local:31000/rak007007/gs-spring-boot`
- create configMap `kubectl create -f src/main/.kube/config.yaml`
- create secrets   `kubectl create -f src/main/.kube/secrets.yaml`
- create service for exposing `kubectl create -f src/main/.kube/service.yaml`
- create deployment `kubectl create -f src/main/.kube/deployment.yaml`
- visit `https://52.74.51.169/api/v1/proxy/namespaces/default/services/spring-bp-service/` and verify the deployment of the service