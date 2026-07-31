#!/usr/bin/env bash

warning_count=0
error_count=0

log() {
    printf '[INFO] %s\n' "$*" >&2
}

warn() {
    printf '[WARN] %s\n' "$*" >&2
    warning_count=$((warning_count + 1))
}

error() {
    printf '[ERROR] %s\n' "$*" >&2
    error_count=$((error_count + 1))
}

print_summary() {
    local result=$1
    printf '\n========================================\n' >&2
    printf 'RESULT:   %s\n' "$result" >&2
    printf 'WARNINGS: %d\n' "$warning_count" >&2
    printf 'ERRORS:   %d\n' "$error_count" >&2
    printf '========================================\n' >&2
}

require_command() {
    local name=$1
    if ! command -v "$name" >/dev/null 2>&1; then
        error "Required command not found: $name"
        return 1
    fi
    return 0
}

find_build_tool() {
    local name=$1
    local sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
    local candidate=''

    if [[ -n $sdk_root && -d $sdk_root/build-tools ]]; then
        candidate=$(find "$sdk_root/build-tools" -mindepth 2 -maxdepth 2 -type f -name "$name" -print \
            | sort -V \
            | tail -n 1)
    fi

    if [[ -n $candidate && -x $candidate ]]; then
        printf '%s\n' "$candidate"
        return 0
    fi

    if command -v "$name" >/dev/null 2>&1; then
        command -v "$name"
        return 0
    fi

    return 1
}

require_archive_entry() {
    local inventory=$1
    local entry=$2
    if ! grep -Fxq "$entry" "$inventory"; then
        error "APK is missing required entry: $entry"
        return 1
    fi
    return 0
}

main() {
    local tag=${1:-}
    local expected_tag='v0.1.5'
    local apk=''
    local asset_dir=''
    local apksigner=''
    local aapt=''
    local zipalign=''
    local tmp_dir=''
    local badging=''
    local cert_digest=''
    local expected_digest=''
    local failed=0

    if [[ $tag != "$expected_tag" ]]; then
        error "Expected release tag $expected_tag, received ${tag:-<empty>}"
        print_summary 'FAILED'
        return 2
    fi

    asset_dir="release-assets/$tag"
    apk="$asset_dir/NoFlyers-$tag.apk"

    log '[1/5] Validating required files and tools'
    for file in "$apk" "$asset_dir/SHA256SUMS" "$asset_dir/CERTIFICATE-SHA256.txt" "release-notes/$tag.md"; do
        if [[ ! -s $file ]]; then
            error "Required release file is missing or empty: $file"
            failed=1
        fi
    done

    require_command sha256sum || failed=1
    require_command unzip || failed=1

    apksigner=$(find_build_tool apksigner) || {
        error 'Android apksigner was not found.'
        failed=1
    }
    aapt=$(find_build_tool aapt) || {
        error 'Android aapt was not found.'
        failed=1
    }
    zipalign=$(find_build_tool zipalign) || {
        error 'Android zipalign was not found.'
        failed=1
    }

    if ((failed != 0)); then
        print_summary 'FAILED'
        return 1
    fi

    tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/noflyers-release.XXXXXXXX") || {
        error 'Could not create a temporary validation directory.'
        print_summary 'FAILED'
        return 1
    }

    log '[2/5] Verifying checksum and alignment'
    if ! (cd "$asset_dir" && sha256sum -c SHA256SUMS); then
        error 'Release checksum verification failed.'
        failed=1
    fi
    if ! "$zipalign" -c -P 16 4 "$apk"; then
        error 'APK zip alignment verification failed.'
        failed=1
    fi

    log '[3/5] Verifying APK signature and certificate'
    if ! "$apksigner" verify --verbose --print-certs "$apk" >"$tmp_dir/apksigner.txt" 2>&1; then
        cat "$tmp_dir/apksigner.txt" >&2
        error 'APK signature verification failed.'
        failed=1
    else
        cat "$tmp_dir/apksigner.txt" >&2
        if ! grep -Fq 'Verified using v2 scheme (APK Signature Scheme v2): true' "$tmp_dir/apksigner.txt"; then
            error 'APK is not protected by APK Signature Scheme v2.'
            failed=1
        fi
        cert_digest=$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$tmp_dir/apksigner.txt" | head -n 1 | tr '[:upper:]' '[:lower:]')
        expected_digest=$(tr -d '[:space:]' < "$asset_dir/CERTIFICATE-SHA256.txt" | tr '[:upper:]' '[:lower:]')
        if [[ -z $cert_digest || $cert_digest != "$expected_digest" ]]; then
            error "Signing certificate mismatch: expected $expected_digest, found ${cert_digest:-<none>}"
            failed=1
        fi
    fi

    log '[4/5] Verifying package identity and embedded version'
    if ! badging=$("$aapt" dump badging "$apk" 2>"$tmp_dir/aapt.err"); then
        cat "$tmp_dir/aapt.err" >&2
        error 'Could not read APK package metadata.'
        failed=1
    else
        if ! grep -Fq "package: name='io.github.cbkii.noflyers' versionCode='15' versionName='0.1.5'" <<<"$badging"; then
            error 'APK package name or version does not match NoFlyers v0.1.5.'
            failed=1
        fi
        if grep -Fq 'application-debuggable' <<<"$badging"; then
            error 'Release APK is marked debuggable.'
            failed=1
        fi
    fi

    log '[5/5] Verifying Xposed module payload'
    if ! unzip -Z1 "$apk" > "$tmp_dir/inventory.txt"; then
        error 'Could not inspect APK inventory.'
        failed=1
    else
        require_archive_entry "$tmp_dir/inventory.txt" 'assets/xposed_init' || failed=1
        require_archive_entry "$tmp_dir/inventory.txt" 'META-INF/xposed/scope.list' || failed=1
        require_archive_entry "$tmp_dir/inventory.txt" 'classes.dex' || failed=1
    fi

    rm -rf -- "$tmp_dir" 2>/dev/null || warn 'Could not remove the temporary validation directory.'

    if ((failed != 0 || error_count != 0)); then
        print_summary 'FAILED'
        return 1
    fi

    print_summary 'SUCCESS'
    return 0
}

main "$@"
