# Deploying the DPLA API

The DPLA API is a Scala/JVM service running on AWS ECS Fargate (8 tasks, blue/green deployment via CodeDeploy). It powers all search and item retrieval for dp.la and all local hub sites.

**Production URL:** https://api.dp.la
**Internal URL:** https://api-internal.dp.la (used by dp.la frontends for SSR)

---

## Preferred: deploy both API services together

When deploying both the DPLA API and the thumbnail API in the same maintenance window, use the combined script to run everything in parallel. This produces one impact window instead of two:

```bash
~/bin/deploy-api-services          # deploy both
~/bin/deploy-api-services api      # api only
~/bin/deploy-api-services thumb    # thumbnail-api only
```

`deploy-api-services` is a local operator script at `~/bin/deploy-api-services` on the deploy machine — it is not part of this repository. It handles pre-flight checks, parallel ECR builds, ECR verification, and parallel pipeline execution with live monitoring. If you don't have it, follow the manual steps below for each service separately.

---

## Manual deploy (this service only)

Deployment is a **two-phase process**. Merging a PR to `main` does **not** automatically deploy — the pipeline webhook is intentionally disabled. Every deployment must be triggered manually after a PR is merged.

### Phase 1: Build the Docker image (GitHub Actions)

Dispatch the `deploy.yml` workflow ("Deploy to Amazon ECR") manually:

```bash
gh api --method POST \
  /repos/dpla/api/actions/workflows/deploy.yml/dispatches \
  -f ref=main
```

Or go to **Actions → Deploy to Amazon ECR → Run workflow** in the GitHub UI.

This runs `sbt assembly` to build the JAR, then builds a multi-arch (amd64 + arm64) Docker image and pushes it to ECR tagged as `latest`, the branch name (e.g. `main`), and the commit SHA. **This takes approximately 20–25 minutes.**

> ⚠️ **Do not skip this step.** The CodePipeline does not build Docker images — it only deploys whatever `api:latest` is currently in ECR. Starting the pipeline without first building a new image will re-deploy stale code silently.

After the action completes, verify the new image is in ECR:

```bash
aws ecr describe-images \
  --repository-name api \
  --image-ids imageTag=latest \
  --region us-east-1 \
  --query 'imageDetails[0].imagePushedAt' \
  --output text
```

The timestamp should match the current deployment time.

### Phase 2: Run the CodePipeline

Once the new image is confirmed in ECR:

```bash
aws codepipeline start-pipeline-execution \
  --name api-pipeline \
  --region us-east-1
```

The pipeline has three stages:

| Stage | What it does | Typical duration |
|---|---|---|
| **Source** | Pulls latest `main` from GitHub | ~10 seconds |
| **Build** | Generates `taskdef.json` (pointing at new `api:latest`) and `appspec.yaml` | ~1 minute |

> The `api-codebuild` buildspec is stored inline in the AWS CodeBuild project, not in this repository. It injects the current `api:latest` ECR digest into `taskdef.json` and renders `appspec_template.yaml` into `appspec.yaml` for CodeDeploy.
| **Production** | Blue/green ECS deploy (CodeDeploy `ECSAllAtOnce`) | ~8 minutes |

**Total typical duration: ~30–35 minutes** (dominated by Phase 1).

Monitor pipeline progress:

```bash
aws codepipeline get-pipeline-state \
  --name api-pipeline \
  --region us-east-1 \
  --query 'stageStates[*].{stage:stageName,status:latestExecution.status}'
```

---

## Important characteristics

### Blue/green deployment with automatic rollback

This service uses **blue/green deployment** via AWS CodeDeploy (`ECSAllAtOnce`). During a deploy:

1. A new "green" task set (8 tasks) is created alongside the live "blue" set.
2. Once all green tasks pass health checks, CodeDeploy shifts 100% of ALB traffic to green.
3. Blue tasks are terminated 5 minutes later.
4. Automatic rollback is enabled — a deployment failure will revert traffic to the blue task set. *(Verified in AWS console 2026-04-08; configured on the CodeDeploy deployment group, not in `appspec_template.yaml`.)*

### Slow-start on ALB target groups

Both `api-tg-blue` and `api-tg-green` have **90-second slow start** enabled. When the ALB registers new tasks after a traffic shift, it ramps each task's traffic share from near-zero to full over 90 seconds. This gives the JVM time to warm before absorbing real load — without it, cold tasks receive full traffic immediately and can return brief 503s. No action required; this is configured at the ALB level.

### Why the webhook is disabled

The CodePipeline webhook (auto-trigger on push to `main`) is intentionally disabled. The reason: the pipeline does not build a new Docker image — it only deploys whatever is currently in ECR. If the webhook were active, every PR merge would trigger a deploy of stale code with no pre-flight checks and no post-deploy health verification.

---

## Pre-deploy checklist

> **API key:** Use an existing operator key, or obtain one at https://pro.dp.la/developers/api-key. Required for all `/v2/items` requests.

- [ ] Confirm API is healthy: `curl -s "https://api.dp.la/v2/items?api_key=<YOUR_API_KEY>&page_size=1" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['count'], 'items')"`
- [ ] No in-flight pipeline executions: `aws codepipeline list-pipeline-executions --pipeline-name api-pipeline --query 'pipelineExecutionSummaries[?status==\`InProgress\`]'`
- [ ] No in-flight GH Action runs: `gh run list --repo dpla/api --workflow "Deploy to Amazon ECR" --limit 3`
- [ ] Confirm auto-rollback is still enabled: `aws deploy get-deployment-group --application-name api-deployment --deployment-group-name api-deployment-group --region us-east-1 --query 'deploymentGroupInfo.autoRollbackConfiguration' --output json` — verify `"enabled": true`

---

## Post-deploy health check

```bash
curl -s "https://api.dp.la/v2/items?api_key=<YOUR_API_KEY>&page_size=1" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('HTTP 200 —', d['count'], 'items in index')"
```

The API uses Elasticsearch as its primary data store. Expect 45–55 million items as of April 2026 (the count grows over time). A dramatically lower count (e.g. under 40 million) likely indicates an Elasticsearch connectivity or index issue.

---

## Troubleshooting and manual rollback

### If the GitHub Action fails

Check the action logs in the **Actions** tab in the GitHub UI. Common issues:

- **`sbt assembly` failure:** Check for compilation errors or dependency resolution failures in the build log. Fix the code, push a new commit, and re-dispatch.
- **ECR push failure:** Verify AWS credentials are valid and the ECR repo exists. Re-run the failed workflow from the GitHub Actions UI.
- **Build timeout:** Rare, but `sbt assembly` can time out on a cold runner. Re-run the workflow.

### If the CodePipeline fails

Check which stage failed:

```bash
aws codepipeline get-pipeline-state \
  --name api-pipeline \
  --region us-east-1 \
  --query 'stageStates[*].{stage:stageName,status:latestExecution.status}'
```

- **Source stage:** Usually a GitHub connectivity issue. Retry the pipeline: `aws codepipeline start-pipeline-execution --name api-pipeline --region us-east-1`
- **Build stage:** Check CodeBuild logs for errors generating `taskdef.json` or `appspec.yaml`.
- **Production stage:** Check the CodeDeploy deployment for health check failures. Automatic rollback should trigger; verify with `aws deploy list-deployments --application-name api-deployment --deployment-group-name api-deployment-group --region us-east-1`.

### Manual rollback

If automatic rollback doesn't trigger or issues are discovered post-deployment:

1. Find the previous successful deployment:

```bash
aws deploy list-deployments \
  --application-name api-deployment \
  --deployment-group-name api-deployment-group \
  --include-only-statuses Succeeded \
  --max-items 5 \
  --region us-east-1
```

2. Re-tag the previous stable ECR image as `latest` and re-run the pipeline. To identify the previous image, check ECR image history:

```bash
aws ecr describe-images \
  --repository-name api \
  --region us-east-1 \
  --query 'sort_by(imageDetails, &imagePushedAt)[-5:].{pushed:imagePushedAt,digest:imageDigest,tags:imageTags}'
```

Then re-tag the desired image and start the pipeline to deploy it.

---

## Infrastructure reference

| Resource | Value |
|---|---|
| GitHub repo | `dpla/api` (Scala — not `dpla/dpla-api`) |
| GH Actions workflow | `deploy.yml` — "Deploy to Amazon ECR" |
| ECR repo | `283408157088.dkr.ecr.us-east-1.amazonaws.com/api` |
| ECR image tag | `latest` (also tagged with branch name and commit SHA) |
| CodePipeline | `api-pipeline` |
| CodeBuild project | `api-codebuild` |
| CodeDeploy app / group | `api-deployment` / `api-deployment-group` |
| ECS cluster / service | `api` / `api` |
| Task count | 8 |
| ALB target groups | `api-tg-blue`, `api-tg-green` (90s slow start) |
| Deployment type | Blue/green (`ECSAllAtOnce`) — auto-rollback on failure |
| AWS region | `us-east-1` |
