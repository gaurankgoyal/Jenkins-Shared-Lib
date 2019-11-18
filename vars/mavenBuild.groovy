def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    mavenTool = "M3"
    pomFile = config.get("pomFile", "pom.xml")
    commonFun.setJobProperties(env.NUM_BUILDS_KEPT, "H/10 * * * *")
    SERVER_URL = commonFun.artifactoryServerUrl()
    CREDENTIALS = "atrifactory"
    def artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: CREDENTIALS
    //print (artifactoryServer)
    def rtMaven = Artifactory.newMavenBuild()
    //print (rtMaven)
    def buildInfo
    //rtMaven.tool = mavenTool
    print (SERVER_URL) 
    print (CREDENTIALS)
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
