#!/bin/bash
# Create GitHub Labels for COSMIC Connect Android Project
# Run this script from the repository root

set -e

echo "Creating GitHub labels for COSMIC Connect Android..."

# Priority Labels
gh label create "P0-Critical" --description "Critical priority - must be completed" --color "d73a4a" --force
gh label create "P1-High" --description "High priority - important to complete" --color "ff6b6b" --force
gh label create "P2-Medium" --description "Medium priority - nice to have" --color "ffa500" --force
gh label create "P3-Low" --description "Low priority - future enhancement" --color "ffcc00" --force

# Category Labels
gh label create "setup" --description "Project setup and infrastructure" --color "0e8a16" --force
gh label create "infrastructure" --description "Build and deployment infrastructure" --color "1d76db" --force
gh label create "audit" --description "Codebase audit and analysis" --color "5319e7" --force
gh label create "android" --description "Android app development" --color "3ddc84" --force
gh label create "cosmic" --description "COSMIC Desktop integration" --color "ed8936" --force
gh label create "documentation" --description "Documentation improvements" --color "0075ca" --force
gh label create "testing" --description "Testing and quality assurance" --color "fbca04" --force
gh label create "protocol" --description "COSMIC Connect protocol implementation" --color "c5def5" --force
gh label create "gradle" --description "Gradle build system" --color "02303a" --force
gh label create "build-system" --description "Build configuration and tooling" --color "006b75" --force
gh label create "kotlin-conversion" --description "Java to Kotlin conversion" --color "7f52ff" --force
gh label create "architecture" --description "Architecture and design patterns" --color "d876e3" --force
gh label create "security" --description "Security and encryption" --color "b60205" --force
gh label create "tls" --description "TLS/SSL implementation" --color "d93f0b" --force
gh label create "networking" --description "Network communication" --color "0052cc" --force
gh label create "plugins" --description "Plugin system and implementations" --color "bfdadc" --force
gh label create "ui" --description "User interface" --color "c2e0c6" --force
gh label create "design" --description "Design system and styling" --color "e99695" --force
gh label create "compose" --description "Jetpack Compose UI" --color "7057ff" --force
gh label create "integration" --description "Integration testing" --color "5ebeff" --force
gh label create "e2e" --description "End-to-end testing" --color "1d76db" --force
gh label create "performance" --description "Performance optimization" --color "d4c5f9" --force
gh label create "release" --description "Release preparation and deployment" --color "0e8a16" --force
gh label create "project-management" --description "Project planning and tracking" --color "bfd4f2" --force

echo "✅ All labels created successfully!"
echo ""
echo "To verify labels, run: gh label list"
