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
    artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: CREDENTIALS
    print (artifactoryServer)
    rtMaven = Artifactory.newMavenBuild()
    print (rtMaven)
    rtMaven.tool = mavenTool
    print (SERVER_URL) 
    print (CREDENTIALS)
    stage 'checkout'
    node {
	stage('Pull Source Code') {
        	checkout scm

	}
	rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artifactoryServer
        rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artifactoryServer
        buildInfo = Artifactory.newBuildInfo()
	pomVersion = readMavenPom().getVersion()
	println(pomVersion)
        stage 'Build'
        //docker.image(config.environment).inside {
        	//sh config.mainScrit
		rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
        //}
        stage 'UnitTest'
	stage ('Publish build info') {
        artifcatoryServer.publishBuildInfo buildInfo
    }
        sh config.postScript
    }
}
