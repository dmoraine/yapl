# F-Droid submission

[`be.moraine.yapl.yml`](be.moraine.yapl.yml) is the F-Droid build recipe for YAPL.
It is **not** read from this repository — F-Droid builds from a metadata file kept
in the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository. This copy
lives here for reference and to make submission a copy-paste.

## How to submit

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Copy `be.moraine.yapl.yml` to `metadata/be.moraine.yapl.yml` in your fork.
3. Run `fdroid lint be.moraine.yapl` and `fdroid build be.moraine.yapl:1` to check
   the recipe builds (see the F-Droid
   [Inclusion How-To](https://f-droid.org/docs/Inclusion_How-To/)).
4. Open a merge request against fdroiddata.

App descriptions, the icon and screenshots are picked up automatically from the
[`fastlane/metadata/`](../fastlane/metadata) tree in this repository, so they do
not need to be duplicated in the recipe.

Each new release: push a `vX.Y.Z` tag with a matching `versionName`/`versionCode`
bump in `app/build.gradle.kts`. `UpdateCheckMode: Tags` + `AutoUpdateMode: Version v%v`
let F-Droid pick it up automatically.
