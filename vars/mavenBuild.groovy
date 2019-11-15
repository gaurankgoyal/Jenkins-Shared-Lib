def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    pomFile = config.get("pomFile", "pom.xml")
    
    stage 'checkout'
    node {
	stage('Pull Source Code') {
        	checkout scm

	}
	pom = readMavenPom file: 'pom.xml'
	pom.version
	//pomVersion = readMavenPom().getVersion()
        println(pom.version)
        stage 'Build'
        docker.image(config.environment).inside {
        	sh config.mainScript
        }
        stage 'UnitTest'
        sh config.postScript
    }
}
