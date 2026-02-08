# Release Checklist

Concise, actionable checklist for each COSMIC Connect Android release.

For detailed background, see [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md).

---

## Pre-Release

- [ ] All unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Lint check passes: `./gradlew lintDebug`
- [ ] Debug build compiles: `./gradlew assembleDebug`
- [ ] No uncommitted changes: `git status`
- [ ] CHANGELOG.md updated with release notes

## Version Bump

- [ ] Run version bump script: `./scripts/bump-version.sh X.Y.Z[-beta]`
- [ ] Verify versionName and versionCode updated in `app/build.gradle.kts`
- [ ] Verify CHANGELOG has new version section with correct date
- [ ] Push with tags: `git push origin master --tags`

## Build Release APK

### Local Build
- [ ] Set signing env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- [ ] Run: `./scripts/build-release.sh`
- [ ] Verify APK in `app/build/outputs/apk/release/`
- [ ] Verify SHA256 checksums generated

### CI Build (Automated)
- [ ] Tag push triggers `.github/workflows/release.yml`
- [ ] GitHub Release created automatically with APK + checksums
- [ ] Verify GitHub Release page has correct assets

## GitHub Release

- [ ] Release title: "COSMIC Connect X.Y.Z[-beta]"
- [ ] Auto-generated release notes are accurate
- [ ] APK and .sha256 files attached
- [ ] Pre-release flag set correctly (true for beta/alpha/rc)

## Post-Release Verification

- [ ] Download APK from GitHub Release
- [ ] Verify checksum: `sha256sum -c cosmic-connect-*.apk.sha256`
- [ ] Install on test device: `adb install cosmic-connect-*.apk`
- [ ] Basic smoke test: device discovery, pairing, ping

## Beta Testing

- [ ] Announce beta in GitHub Discussions
- [ ] Share APK download link
- [ ] Monitor GitHub Issues for bug reports
- [ ] Triage and fix critical issues
- [ ] Plan next iteration based on feedback

---

**Quick Reference:**

```bash
# Full release flow
./gradlew testDebugUnitTest                    # 1. Tests pass
./scripts/bump-version.sh 1.2.0-beta           # 2. Bump version
git push origin master --tags                  # 3. Push (triggers CI release)
```
