.PHONY: help install install-node install-python build test test-unit test-int test-slow \
        test-api test-embed test-ingest test-ui smoke seed validate-fixture ollama-pull clean

help: ## Show available targets
	@grep -E '^[a-zA-Z0-9_-]+:.*?##' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

install: install-node install-python ## Install all dependencies (Node + Python)

install-node: ## Install Node dependencies and Turborepo
	npm install

install-python: ## Install Python runtime + dev/test dependencies
	python3 -m pip install -r packages/embed-sidecar/requirements.txt -q
	python3 -m pip install -r packages/embed-sidecar/requirements-dev.txt -q
	python3 -m pip install -r packages/ingest-cli/requirements.txt -q
	python3 -m pip install -r packages/ingest-cli/requirements-dev.txt -q

build: ## Build all packages via Turborepo
	npx turbo run build

test: ## Run all tests (unit + integration)
	npx turbo run test

test-unit: ## Run unit tests only (no Docker)
	npx turbo run test-unit

test-int: ## Run integration tests (requires Docker)
	npx turbo run test-int

test-slow: ## Run slow tests (requires Docker + Ollama with mistral model)
	cd packages/api && mvn test -q -Pslow

test-api: ## Run API package tests only
	cd packages/api && mvn test -q

test-embed: ## Run embed-sidecar tests only
	cd packages/embed-sidecar && python3 -m pytest -q

test-ingest: ## Run ingest-cli tests only
	cd packages/ingest-cli && python3 -m pytest -q

test-ui: ## Run UI package tests only
	cd packages/ui && npm run test-unit

smoke: ## Run E2E smoke test against full stack (requires docker compose)
	@echo "Smoke tests require full stack — see issue #28"
	@bash scripts/e2e-smoke.sh 2>/dev/null || \
		echo "scripts/e2e-smoke.sh not yet implemented (issue #28)"

validate-fixture: ## Validate demo fixture against SPEC §14 source fields
	python3 scripts/validate_fixture.py

seed: validate-fixture ## Seed demo data into news_radar collection
	bash scripts/seed-demo.sh

ollama-pull: ## Pull default Ollama model (mistral) into docker compose ollama service
	bash scripts/ollama-pull-model.sh

clean: ## Remove build artifacts from all packages
	npx turbo run clean
	rm -rf node_modules/.cache
