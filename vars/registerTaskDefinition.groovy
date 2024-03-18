def call(region, filename) {
	sh (
		returnStdout: true,
		script: "aws ecs register-task-definition --region ${region} --cli-input-json file://$filename | egrep 'taskDefinitionArn' | tr ',' ' ' | awk '{print \$2}'").trim()
}
