def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    configFolder = config.get("configFolder")
    
    node {
        stage("Checkout") {
            checkout scm
        }
        jsonFiles = findFiles glob: "${configFolder}/*.json"
        index = 0
        for (jsonfile in jsonFiles) {
                index = index + 1
                config = readJSON file: jsonfile.path
                connector = config["name"]
                changedFiles = sh(script: "git diff --name-only HEAD HEAD~1", returnStdout: true).trim()
                properties = readFile jsonfile.path
                print(properties)
                properties = properties.replace('${env}', env.environment)
                print(properties)
            }
        }
}


