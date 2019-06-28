# k3s-jenkins
## Starting
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

## Known problems
* Changes to Jenkins configuration might be overwritten at restart.
* Vagrant cannot login to the VM on Windows. Setting the following environment variable might help:
```
set VAGRANT_PREFER_SYSTEM_BIN=0
```
