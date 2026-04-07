# Deploying the DPLA API

The DPLA API is a Scala/JVM service running on AWS ECS Fargate (13 tasks, rolling deployment). It powers all search and item retrieval for dp.la and all local hub sites.

**Production URL:** https://api.dp.la
**Internal URL:** https://api-internal.dp.la (used by dp.la frontends for SSR)

---

## How deployment works

Deployment is a **two-phase process**. Merging a PR to `main` does **not** automatically deploy — the pipeline webhook is intentionally disabled. Every deployment must be triggered manually after a PR is merged.

### Phase 1: Build the Docker image (GitHub Actions)

Dispatch the `deploy.yml` workflow ("Deploy to Amazon ECR") manually:

```bash
gh api --method POST \
  /repos/dpla/api/actions/workflows/deploy.yml/dispatches \
  -f ref=main
```

Or go to **Actions → Deploy to Amazon ECR → Run workflow** in the GitHub UI.

This runs `sbt assembly` to build the JAR, then builds a multi-arch (amd64 + arm64) Docker image and pushes it to ECR tagged as `latest`, `main`, and the commit SHA. **This takes approximately 20–25 minutes.**

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
| **Production** | Rolling ECS deploy across 13 tasks (50% minimum healthy) | ~10–15 minutes |

**Total typical duration: ~35–45 minutes** (dominated by Phase 1).

Monitor pipeline progress:

```bash
aws codepipeline get-pipeline-state \
  --name api-pipeline \
  --region us-east-1 \
  --query 'stageStates[*].{stage:stageName,status:latestExecution.status}'
```

---

## Important characteristics

### Rolling deployment — no auto-rollback

This service uses a **rolling deployment** strategy (not blue/green). Up to half the tasks (6–7 of 13) may be replaced simultaneously. There is **no automatic rollback** configured — if tasks fail to start, the service may be left in a degraded state until manual intervention. Monitor ECS events if the deploy hangs:

```bash
aws ecs describe-services \
  --cluster api --services api \
  --query 'services[0].events[0:5]'
```

### Brief 503s during deploy are expected

With 50% minimum healthy, some requests will hit tasks being replaced. Callers — including dp.la frontends via `api-internal.dp.la` — may see brief 503 errors during the ~10–15 minute deploy window. This is expected and not a sign of a failed deployment.

### Why the webhook is disabled

The CodePipeline webhook (auto-trigger on push to `main`) is intentionally disabled. The reason: the pipeline does not build a new Docker image — it only deploys whatever is currently in ECR. If the webhook were active, every PR merge would trigger a rolling restart of all 13 tasks deploying stale code, with no pre-flight checks and no post-deploy health verification.

---

## Pre-deploy checklist

- [ ] Confirm API is healthy: `curl -s "https://api.dp.la/v2/items?api_key=<KEY>&page_size=1" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['count'], 'items')"`
- [ ] No in-flight pipeline executions: `aws codepipeline list-pipeline-executions --pipeline-name api-pipeline --query 'pipelineExecutionSummaries[?status==\`InProgress\`]'`
- [ ] No in-flight GH Action runs: `gh run list --repo dpla/api --workflow "Deploy to Amazon ECR" --limit 3`

---

## Post-deploy health check

```bash
curl -s "https://api.dp.la/v2/items?api_key=<YOUR_API_KEY>&page_size=1" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('HTTP 200 —', d['count'], 'items in index')"
```

Expect ~50 million items. A dramatically lower count could indicate an Elasticsearch connectivity issue.

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
| ECS cluster / service | `api` / `api` |
| Task count | 13 (rolling, 50% min healthy, 200% max) |
| Deployment type | Rolling — **no automatic rollback** |
| AWS region | `us-east-1` |
