def call(ecr_repo) {
    script {
        def ECR = sh(script: "echo '$ecr_repo' | rev | cut -d'/' -f1 | rev", returnStdout: true).trim()

        def TAG = sh(script: "aws ecr describe-images --repository-name $ECR --region ap-south-1 --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]' --output text", returnStdout: true).trim()

        echo "Original Tag: $TAG"

        // Increment the tag by 0.1 using bc
        def incrementedTag = sh(script: "echo \"$TAG + 0.1\" | bc", returnStdout: true).trim()

        echo "Incremented Tag: $incrementedTag"

        // Return the incremented tag
        return incrementedTag
    }
}
