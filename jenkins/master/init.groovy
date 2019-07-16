import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import org.csanchez.jenkins.plugins.kubernetes.*
import jenkins.model.*



/**
 A shared method to safely create a credential in the global domain.
 */
def addCredential(String credentialsId, def credential) {
    boolean credentialsModified = false
    Domain domain
    SystemCredentialsProvider systemCredentialsProvider = SystemCredentialsProvider.getInstance()
    Map systemCredentialMap = systemCredentialsProvider.getDomainCredentialsMap()
    domain = systemCredentialMap.keySet().find { it.getName() == null }
    if (!systemCredentialMap[domain] || (systemCredentialMap[domain].findAll { (credentialsId == it.id) }).size() < 1) {
        if (systemCredentialMap[domain] && systemCredentialMap[domain].size() > 0) {
            //other credentials exist so should only append
            systemCredentialMap[domain] << credential
        } else {
            systemCredentialMap[domain] = [credential]
        }
        credentialsModified = true
    }
    //save any modified credentials
    if (credentialsModified) {
        println "${credentialsId} credentials added to Jenkins."
        systemCredentialsProvider.setDomainCredentialsMap(systemCredentialMap)
        systemCredentialsProvider.save()
    } else {
        println "${credentialsId} credentials already configured."
    }
}

static KubernetesCloud createK8sClud(String apiIP, String tunnelIP, String credentialsId) {
    def kubernetesCloud = new KubernetesCloud(
            'kubernetes',
            null,
            "https://${apiIP}",
            'jenkins',
            null,
            '10', 5, 15, 5
    )
    kubernetesCloud.setSkipTlsVerify(true)
    kubernetesCloud.setCredentialsId(credentialsId)
    kubernetesCloud.setJenkinsTunnel("${tunnelIP}:50000")
    InetAddress dnsInetAddress = InetAddress.getByName 'jenkins'
    kubernetesCloud.setJenkinsUrl(dnsInetAddress.hostAddress)
    kubernetesCloud.setMaxRequestsPerHostStr('32')
    kubernetesCloud
}

static PodTemplate createPodTemplate() {
    def podTemplate = new PodTemplate()
    podTemplate.setName('jnlp-agent-maven')
    podTemplate.setLabel('jnlp-agent-maven')

    List<ContainerTemplate> containerList = []

    ContainerTemplate containerTemplate = new ContainerTemplate('jnlp', 'ungerts/jnlp-agent-maven')
    containerTemplate.setWorkingDir('/home/jenkins')
    containerTemplate.setCommand('')
    containerTemplate.setArgs('')
    containerTemplate.setTtyEnabled(true)

    containerList.add(containerTemplate)

    podTemplate.setContainers(containerList)
    podTemplate
}

def addCloud(String apiIP, String tunnelIP, String credentialsId) {
    def jenkins = Jenkins.get()
    def clouds = jenkins.clouds
    KubernetesCloud kubernetesCloud = createK8sClud(apiIP, tunnelIP, credentialsId)
    PodTemplate podTemplate = createPodTemplate()
    kubernetesCloud.addTemplate(podTemplate)
    jenkins.clouds.replace(kubernetesCloud)
    jenkins.save()
}


def password = (new File('/etc/k3s-access/password')).text
def username = (new File('/etc/k3s-access/username')).text
def tunnelIP = (new File('/etc/k3s-access/tunnelIP')).text
def apiIP = (new File('/etc/k3s-access/apiIP')).text
String credentialsId = 'k3s'

addCredential(
        'k3s',
        new UsernamePasswordCredentialsImpl(null, 'k3s', 'k3s', username, password)
)
addCloud(apiIP, tunnelIP, credentialsId)





