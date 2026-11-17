#!/usr/bin/env bash
# Builds the current checkout into a container image with Cloud Build and
# deploys it to the team's Cloud Run service. Usage:
#   ./deploy.sh
# Requires the gcloud CLI (signed in) and access to the GCP project below.
set -euo pipefail
cd "$(dirname "$0")"

PROJECT_ID="${PROJECT_ID:-jasaks-4156-9a0bbf}"
REGION="us-east1"
SERVICE="jasaks-api"          # Cloud Run service
SQL_INSTANCE="jasaks-pg"      # Cloud SQL (PostgreSQL 17) instance
DB_NAME="jasaks"
DB_USER="jasaks"
DB_SECRET="jasaks-db-password"  # Secret Manager secret holding the DB password
RUN_SA="jasaks-run@${PROJECT_ID}.iam.gserviceaccount.com"

# Every gcloud call passes the project explicitly, so this script never
# depends on (or changes) your default gcloud project.
g() { gcloud --project "$PROJECT_ID" "$@"; }

# Tag the image with the commit so every deployment is traceable to code.
TAG="$(git rev-parse --short HEAD)"
if [ -n "$(git status --porcelain -- src pom.xml Dockerfile)" ]; then
  TAG="${TAG}-dirty-$(date +%Y%m%d%H%M%S)"
  echo "Note: deploying uncommitted changes (image tag ${TAG})."
fi
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/jasaks/${SERVICE}:${TAG}"

echo "==> Building ${IMAGE}"
g builds submit . --tag "$IMAGE"

# The app reaches the database over its private network address.
DB_HOST="$(g sql instances describe "$SQL_INSTANCE" \
  --format='value(ipAddresses.filter("type:PRIVATE").extract(ipAddress).flatten())')"

echo "==> Deploying to Cloud Run"
g run deploy "$SERVICE" --region "$REGION" --image "$IMAGE" \
  --service-account "$RUN_SA" \
  --network default --subnet default --vpc-egress private-ranges-only \
  --set-env-vars "DB_URL=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME},DB_USER=${DB_USER}" \
  --set-secrets "DB_PASSWORD=${DB_SECRET}:latest" \
  --memory 1Gi --cpu 1 --cpu-boost \
  --min-instances 0 --max-instances 2 --quiet

URL="$(g run services describe "$SERVICE" --region "$REGION" --format='value(status.url)')"
echo "==> Checking ${URL}/api/spaces"
curl -fsS -o /dev/null -w "HTTP %{http_code}\n" "${URL}/api/spaces"
echo "Deployed: ${URL}"
