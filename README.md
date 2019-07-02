# k3s-jenkins
Runs Jenkins on top of Kubernetes. 

## Getting Started
Use Vagrant to build and start the VM:
```
vagrant up
```
Log in to the VM:
```
vagrant ssh k3s
```
Use `sudo kubectl get pods` to follow the startup process of Jenkins. If the startup process is finished, the pod with the prefix _jenkins_ should be in status running.
```
NAME                       READY   STATUS    RESTARTS   AGE
jenkins-59657c8957-bhkd2   1/1     Running   0          71m
svclb-jenkins-zw8xq        1/1     Running   0          71m
```
Now you can logging to Jenkins: http://192.168.33.10:8080

In order to verfiy that Jenkins is configured correctly, you can add a Job with the following pipeline DSL:
```
def label = "jenkins-slave-${UUID.randomUUID().toString()}"
stage('Hello') {
    podTemplate(label: label, inheritFrom: 'jnlp-agent-maven') {
        node(label) {
            echo 'Hello from POD...'
        }
    }
}
```
Nexus is started automatically. However, initialization of Nexus might take some time. The Nexus UI can be accessed using the following URL:
http://192.168.33.10:8081/

Get Nexus Admin password:
```
kubectl exec -ti $(kubectl get pods -o=jsonpath="{$.items[?(@.metadata.labels.app=='nexus')].metadata.name}") -- cat /nexus-data/admin.password
```

Sonarqube URL: http://192.168.33.10:9000

## Known problems
* Changes to Jenkins configuration might be overwritten at restart.
* Vagrant cannot login to the VM on Windows. Setting the following environment variable might help:
```
set VAGRANT_PREFER_SYSTEM_BIN=0
```
* Vagrant box may run out of disk space. Box can be resized using the [vagrant-disksize plugin](https://github.com/sprotheroe/vagrant-disksize).
