kubectl create namespace jenkins
kubectl config set-context jenkins --namespace=jenkins --cluster=default --user=default
kubectl config use-context jenkins
kubectl apply -f ./jenkins/slave-maven/jenkins-slave-service.yml
USERNAME=`kubectl config view -o=jsonpath="{$.users[?(@.name=='default')].user.username}" | base64 -w0`
PASSWORD=`kubectl config view -o=jsonpath="{$.users[?(@.name=='default')].user.password}" | base64 -w0`
TUNNEL_IP=`kubectl get services -o=jsonpath="{$.items[?(@.metadata.name=='jenkins-jnlp')].spec.clusterIP}" | base64 -w0`
API_IP=`kubectl get services --all-namespaces -o=jsonpath="{$.items[?(@.metadata.name=='kubernetes')].spec.clusterIP}" | base64 -w0`

cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: k3s-access
type: Opaque
data:
  password: $PASSWORD
  username: $USERNAME
  tunnelIP: $TUNNEL_IP
  apiIP: $API_IP
EOF

# Add Jenkins (master)
kubectl apply -f ./jenkins/master/jenkins-master-deployment.yml
kubectl apply -f ./jenkins/master/jenkins-master-service.yml

# Add Nexus
kubectl apply -f nexus/nexus-deployment.yml
kubectl apply -f nexus/nexus-service.yml

# Add Sonarqube
kubectl apply -f sonar/sonarqube-deployment.yml
kubectl apply -f sonar/sonarqube-service.yml