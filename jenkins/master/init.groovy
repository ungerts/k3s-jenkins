import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import java.io.File
import org.csanchez.jenkins.plugins.kubernetes.*
import jenkins.model.*

/**
  Resolves a credential scope if given a string.
  */
def resolveScope(String scope) {
    scope = scope.toString().toUpperCase()
    if(!(scope in ['GLOBAL', 'SYSTEM'])) {
        scope = 'GLOBAL'
    }
    CredentialsScope."${scope}"
}

/**
  A shared method used by other "setCredential" methods to safely create a
  credential in the global domain.
  */
def addCredential(String credentials_id, def credential) {
    boolean modified_creds = false
    Domain domain
    SystemCredentialsProvider system_creds = SystemCredentialsProvider.getInstance()
    Map system_creds_map = system_creds.getDomainCredentialsMap()
    domain = (system_creds_map.keySet() as List).find { it.getName() == null }
    if(!system_creds_map[domain] || (system_creds_map[domain].findAll {credentials_id.equals(it.id)}).size() < 1) {
        if(system_creds_map[domain] && system_creds_map[domain].size() > 0) {
            //other credentials exist so should only append
            system_creds_map[domain] << credential
        }
        else {
            system_creds_map[domain] = [credential]
        }
        modified_creds = true
    }
    //save any modified credentials
    if(modified_creds) {
        println "${credentials_id} credentials added to Jenkins."
        system_creds.setDomainCredentialsMap(system_creds_map)
        system_creds.save()
    }
    else {
        println "Nothing changed.  ${credentials_id} credentials already configured."
    }
}

/**
  Supports SSH username and private key (directly entered private key)
  credential provided by BasicSSHUserPrivateKey class.
  Example:
    [
        'credential_type': 'BasicSSHUserPrivateKey',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'user': 'some user',
        'key_passwd': 'secret phrase',
        'key': '''
private key contents (do not indent it)
        '''.trim()
    ]
  */
def setBasicSSHUserPrivateKey(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String user = ((settings['user'])?:'').toString()
    String key = ((settings['key'])?:'').toString()
    String key_passwd = ((settings['key_passwd'])?:'').toString()
    String description = ((settings['description'])?:'').toString()

    addCredential(
            credentials_id,
            new BasicSSHUserPrivateKey(
                resolveScope(settings['scope']),
                credentials_id,
                user,
                new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(key),
                key_passwd,
                description)
            )
}

/**
  Supports String credential provided by StringCredentialsImpl class.
  Example:
    [
        'credential_type': 'StringCredentialsImpl',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'secret': 'super secret text'
    ]
  */
def setStringCredentialsImpl(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String description = ((settings['description'])?:'').toString()
    String secret = ((settings['secret'])?:'').toString()
    addCredential(
            credentials_id,
            new StringCredentialsImpl(
                resolveScope(settings['scope']),
                credentials_id,
                description,
                Secret.fromString(secret))
            )
}

/**
  Supports username and password credential provided by
  UsernamePasswordCredentialsImpl class.
  Example:
    [
        'credential_type': 'UsernamePasswordCredentialsImpl',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'user': 'some user',
        'password': 'secret phrase'
    ]
  */
def setUsernamePasswordCredentialsImpl(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String user = ((settings['user'])?:'').toString()
    String password = ((settings['password'])?:'').toString()
    String description = ((settings['description'])?:'').toString()

    addCredential(
            credentials_id,
            new UsernamePasswordCredentialsImpl(
                resolveScope(settings['scope']),
                credentials_id,
                description,
                user,
                password)
            )
}

def password = (new File('/etc/k3s-access/password')).text
def username = (new File('/etc/k3s-access/username')).text
def tunnelIP = (new File('/etc/k3s-access/tunnelIP')).text
def apiIP = (new File('/etc/k3s-access/apiIP')).text

addCredential(
            'k3s',
            new UsernamePasswordCredentialsImpl(
                null,
                'k3s',
                'k3s',
                username,
                password)
            )


def j = Jenkins.getInstance()
def k = new KubernetesCloud(
  'kubernetes',
  null,
  "https://${apiIP}",
  'jenkins',
  null,
  '10', 5, 15, 5
)
k.setSkipTlsVerify(true)
k.setCredentialsId('k3s')
k.setJenkinsTunnel("${tunnelIP}:50000")
k.setMaxRequestsPerHostStr('32')

def p = new PodTemplate()
p.setName('jnlp-agent-maven')
p.setLabel('jnlp-agent-maven')

List<ContainerTemplate> containerList = []

ContainerTemplate ct = new ContainerTemplate('jnlp', 'ungerts/jnlp-agent-maven')
ct.setWorkingDir('/home/jenkins')
ct.setCommand('')
ct.setArgs('')
ct.setTtyEnabled(true)

containerList.add(ct)

p.setContainers(containerList)

k.addTemplate(p)


j.clouds.replace(k)
j.save()
