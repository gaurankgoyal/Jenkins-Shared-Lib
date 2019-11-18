import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import jenkins.model.Jenkins

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    def mavenTool = "M3"
    def pomFile = config.get("pomFile", "pom.xml")
    commonFun.setJobProperties(env.NUM_BUILDS_KEPT, "H/10 * * * *")
    def SERVER_URL = commonFun.artifactoryServerUrl()
    def CREDENTIALS = "atrifactory"
    def artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: CREDENTIALS
    //print (artifactoryServer)
    def rtMaven = Artifactory.newMavenBuild()
    //print (rtMaven)
    def buildInfo
    //rtMaven.tool = mavenTool
    print (SERVER_URL) 
    print (CREDENTIALS)


// parameters
def jenkinsKeyUsernameWithPasswordParameters = [
  description:  'Description here',
  id:           'key-id-here',
  secret:       '12345678901234567890',
  userName:     'your-username-here'
]

// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()

// get credentials domain
def domain = Domain.global()

// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

// define Bitbucket secret
def jenkinsKeyUsernameWithPassword = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  jenkinsKeyUsernameWithPasswordParameters.id,
  jenkinsKeyUsernameWithPasswordParameters.description,
  jenkinsKeyUsernameWithPasswordParameters.userName,
  jenkinsKeyUsernameWithPasswordParameters.secret
)

// add credential to store
store.addCredentials(domain, jenkinsKeyUsernameWithPassword)

// save to disk
jenkins.save()




    stage 'checkout'
    node {
	stage('Pull Source Code') {
        	checkout scm

	}
	stage ('Artifactory configuration') {
		rtMaven.tool = mavenTool
		rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artifactoryServer
        	rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artifactoryServer
        	buildInfo = Artifactory.newBuildInfo()
		//pomVersion = readMavenPom().getVersion()
		//println(pomVersion)
	}
        stage 'Build'
		rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
        stage 'UnitTest'
	stage ('Publish build info') {
        artifactoryServer.publishBuildInfo buildInfo
    }
        sh config.postScript
    }
}
