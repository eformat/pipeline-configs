def devProjectName = "development-"
def testProjectName = "testing-"
def prodProjectName = "production-"

stage("Set Project Names") {
  def userInput = input(
  id: 'userInput', message: 'Please Set Project Names', parameters: [
      [$class: 'TextParameterDefinition', defaultValue: 'username', description: 'Username', name: 'USERNAME']
  ])

  devProjectName += userInput
  testProjectName += userInput
  prodProjectName += userInput

  echo ("Development Project: "+devProjectName)
  echo ("Testing Project: "+testProjectName)
  echo ("Production Project: "+prodProjectName)
}

node('maven') {

  stage ('build & deploy in dev') {
    openshiftBuild(namespace: devProjectName,
          buildConfig: 'myapp',
          showBuildLogs: 'true',
          waitTime: '3000000')
  }

  stage ('verify deploy in dev') {
    openshiftVerifyDeployment(namespace: devProjectName,
          depCfg: 'myapp',
          replicaCount:'1',
          verifyReplicaCount: 'true',
          waitTime: '300000')
  }

  stage ('deploy in test') {
    openshiftTag(namespace: devProjectName,
          sourceStream: 'myapp',
          sourceTag: 'latest',
          destinationStream: 'myapp',
          destinationTag: 'promoteQA')

    openshiftDeploy(namespace: testProjectName,
          deploymentConfig: 'myapp',
          waitTime: '300000')

    openshiftScale(namespace: testProjectName,
          deploymentConfig: 'myapp',
          waitTime: '300000',
          replicaCount: '2')
  }

  stage ('verify deploy in test') {
    openshiftVerifyDeployment(namespace: testProjectName,
          depCfg: 'myapp',
          replicaCount:'2',
          verifyReplicaCount: 'true',
          waitTime: '300000')
  }

  stage ('Deploy to production') {
    timeout(time: 2, unit: 'DAYS') {
          input message: 'Approve to production?'
    }
  

    openshiftTag(namespace: devProjectName,
          sourceStream: 'myapp',
          sourceTag: 'promoteQA',
          destinationStream: 'myapp',
          destinationTag: 'promotePRD')

    openshiftDeploy(namespace: prodProjectName,
          deploymentConfig: 'myapp',
          waitTime: '300000')

    openshiftScale(namespace: prodProjectName,
          deploymentConfig: 'myapp',
          waitTime: '300000',
          replicaCount: '2')
  }

  stage ('verify deploy in production') {
    openshiftVerifyDeployment(namespace: prodProjectName,
          depCfg: 'myapp',
          replicaCount:'2',
          verifyReplicaCount: 'true',
          waitTime: '300000')
  }
}
