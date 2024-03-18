#This script creates a TaskDefinition Json file that will be passed to cli-input-json argument when creating a TaskDefinition
#
#!/bin/bash
TASK_FAMILY="$1" 
image1="$2"
filename="$3"
region="$4"
image1tag="$5"

TASK_DEFINITION=$(aws ecs describe-task-definition --task-definition $TASK_FAMILY --region $region)
NEW_TASK_DEFINITION=$(echo $TASK_DEFINITION | jq --arg IMAGE1 $image1:$image1tag  '.taskDefinition | .containerDefinitions[0].image = $IMAGE1 | del(.taskDefinitionArn) | del(.revision) | del(.status) | del(.requiresAttributes) | del(.compatibilities) | del(.registeredAt) | del(.registeredBy)')
sudo echo $NEW_TASK_DEFINITION
sudo echo $NEW_TASK_DEFINITION>$filename