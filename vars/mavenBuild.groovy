//import com.cloudbees.plugins.credentials.*
//import com.cloudbees.plugins.credentials.domains.Domain
//import com.cloudbees.plugins.credentials.impl.*
//import hudson.util.Secret
//import jenkins.model.Jenkins

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

//    withVault(configuration: [timeout: 60, vaultCredentialId: 'vault-token', vaultUrl: 'http://e746f51dee0e:8200'], vaultSecrets: [[path: 'secret/testing', secretValues: [[envVar: 'test_one', vaultKey: 'value_one']]]])
//print (test_one)
// parameters

//Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(
//CredentialsScope.GLOBAL, // Scope
//"my-id", // id
//"My description", // description
//"my-username", // username
//"password" // password
//)
//SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)



    stage 'checkout'
    node {
	stage('Get Secret From Vault'){
	withVault(configuration: [timeout: 60, vaultCredentialId: 'vault-token', vaultUrl: 'http://e746f51dee0e:8200'], vaultSecrets: [[path: 'secret/testing', secretValues: [[envVar: 'test_one', vaultKey: 'value_one']]]])
	print (test_one)
	commonFun.addCredential()
	}
	
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
