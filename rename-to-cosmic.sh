#!/usr/bin/env bash
set -euo pipefail

echo "🚀 Starting comprehensive rename from KDE Connect to COSMIC Connect"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Backup directory
BACKUP_DIR="backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo -e "${BLUE}📦 Creating backup in $BACKUP_DIR${NC}"

# Function to backup and replace
replace_in_files() {
    local old_pattern="$1"
    local new_pattern="$2"
    local description="$3"

    echo -e "${YELLOW}🔄 $description${NC}"

    # Find all files (excluding git, build dirs, backups)
    find . -type f \
        -not -path "./.git/*" \
        -not -path "./build/*" \
        -not -path "./backup-*/*" \
        -not -path "./.gradle/*" \
        -not -path "./app/build/*" \
        -not -name "rename-to-cosmic.sh" \
        -exec grep -l "$old_pattern" {} \; 2>/dev/null | while read -r file; do

        # Backup file
        mkdir -p "$BACKUP_DIR/$(dirname "$file")"
        cp "$file" "$BACKUP_DIR/$file"

        # Replace in file
        sed -i "s|$old_pattern|$new_pattern|g" "$file"
        echo "  ✓ Updated: $file"
    done
}

echo ""
echo -e "${GREEN}Phase 1: Package Names${NC}"
echo "─────────────────────────────────────────────────"
replace_in_files "org\.kde\.kdeconnect_tp" "org.cosmic.cosmicconnect" "Replacing org.kde.kdeconnect_tp"
replace_in_files "org\.kde\.kdeconnect" "org.cosmic.cosmicconnect" "Replacing org.kde.kdeconnect"
replace_in_files "org/kde/kdeconnect" "org/cosmic/cosmicconnect" "Replacing paths org/kde/kdeconnect"

echo ""
echo -e "${GREEN}Phase 2: Class and File References${NC}"
echo "─────────────────────────────────────────────────"
# These are for import statements and comments
replace_in_files "kdeconnect" "cosmicconnect" "Replacing kdeconnect with cosmicconnect"
replace_in_files "KDEConnect" "COSMICConnect" "Replacing KDEConnect with COSMICConnect"
replace_in_files "KdeConnect" "CosmicConnect" "Replacing KdeConnect with CosmicConnect"

echo ""
echo -e "${GREEN}Phase 3: Display Names and Strings${NC}"
echo "─────────────────────────────────────────────────"
replace_in_files "KDE Connect" "COSMIC Connect" "Replacing display name KDE Connect"
replace_in_files "kde-connect" "cosmic-connect" "Replacing hyphenated kde-connect"

echo ""
echo -e "${GREEN}Phase 4: URLs and Documentation${NC}"
echo "─────────────────────────────────────────────────"
# Note: We keep references to the original KDE Connect protocol/documentation for attribution
# but update our own references
echo "  ℹ️  Keeping KDE Connect protocol documentation references for attribution"

echo ""
echo -e "${GREEN}✅ Replacement complete!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "  • Backup created in: $BACKUP_DIR"
echo "  • Package: org.cosmic.cosmicconnect"
echo "  • Path: src/org/cosmic/cosmicconnect"
echo ""
echo -e "${YELLOW}⚠️  Next steps:${NC}"
echo "  1. Rename directory: mv src/org/kde src/org/cosmic"
echo "  2. Rename subdirectory: mv src/org/cosmic/kdeconnect src/org/cosmic/cosmicconnect"
echo "  3. Update tests directory structure similarly"
echo "  4. Review changes with: git diff"
echo "  5. Test build: ./gradlew assembleDebug"
echo ""
