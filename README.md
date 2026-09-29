# AttendanceApp

A native Android app for marking staff attendance via on-device face recognition, with
GPS/network location captured alongside each attendance record.

## Technology / Architecture

- **Language / UI**: Kotlin, Jetpack Compose (Material 3), single-activity app with
  Navigation Compose (`AppNavGraph.kt`) driving all screens.
- **Pattern**: MVVM — each screen has a `ViewModel` exposing a `StateFlow<UiState>`; no DI
  framework is used, a hand-written `AppContainer` wires up repositories/DAOs manually and is
  handed down through a small `ViewModelFactory`.
- **Persistence**: Room (`AppDatabase`) with three tables — `staff`, `enrolled_face` (one row per
  staff member holding their averaged face embedding), and `attendance_record` (one row per
  staff per day, with selfie path, match confidence, and captured location).
- **Face recognition pipeline** (`data/face/`):
  1. **ML Kit Face Detection** (`FaceEmbeddingExtractor`) locates the face in the captured
     selfie and gates on basic quality/liveness checks (eyes open, head roughly frontal).
  2. **MobileFaceNet**, a bundled TensorFlow Lite model (`assets/mobilefacenet.tflite`), turns
     the cropped, aligned face into a 192-dimensional L2-normalized embedding (`FaceEmbedder`).
  3. **`FaceMatcher`** compares the live embedding to the staff member's enrolled embedding via
     cosine similarity against a fixed threshold (0.65) to decide match/no-match. During
     enrolment, 2 shots are captured and their embeddings averaged for a more robust reference
     vector.
- **Camera**: CameraX (front camera preview + still capture), shared between the enrolment and
  attendance-marking flows via one reusable `CameraCaptureScreen` composable.
- **Location**: `LocationProvider` acquires a fix in the background as soon as the camera screen
  opens (so the wait overlaps with face matching instead of following it), racing a cached
  last-known location against a fresh `FusedLocationProviderClient` request
  (`PRIORITY_BALANCED_POWER_ACCURACY`) plus a raw `LocationManager` fallback, bounded to a 10s
  total budget. A fix is only used if it's ≤2 minutes old and ≤200m accuracy; otherwise the
  attendance record is saved with a null location rather than blocking or failing the capture.
  Reverse geocoding (`Geocoder`) resolves the fix to a human-readable address for display.
- **Async**: Kotlin Coroutines throughout (`viewModelScope`, `Dispatchers.IO` for
  disk/network/model work).

## How to run

**Requirements**: Android Studio (a recent version supporting AGP 9.x / Kotlin 2.2), JDK 11+,
a device or emulator running **Android 7.0 (API 24) or higher** with Google Play Services
(needed for the fused location provider) and a front-facing camera.

1. Open the project root in Android Studio and let it sync Gradle.
2. Select a device/emulator and run the `app` configuration (or, from the CLI:
   `./gradlew installDebug`).
3. On first launch you'll be prompted for **Camera** and **Location** permissions — both are
   required to mark attendance (see Limitations below); grant "Precise" location for the most
   reliable fix.

No backend/server setup is needed — everything runs locally against the on-device Room database.

## Demo credentials

Authentication is intentionally a stand-in (see Limitations) — there's one hardcoded admin
account and a passwordless staff sign-in:

| Role  | Username                          | Password   |
|-------|------------------------------------|------------|
| Admin | `admin`                            | `admin123` |
| Staff | `staff` (or any enrolled employee ID) | `staff123` |

Signing in as `staff` with no staff records yet will auto-create a dummy staff member
(`Staff User`, employee ID `emp1`) so the staff flow can be explored immediately. As an admin
you can add real staff members and enrol their faces from **Staff List → Add Staff → Enrol Face**.

## Typical demo flow

1. Sign in as `admin` / `admin123` → **Add Staff** → enter name + employee ID.
2. Enrol the new staff member's face (2 selfie shots, front camera).
3. Log out, sign in as that staff member (employee ID / `staff123`).
4. **Mark Attendance** → grant camera + location permission → capture a selfie.
5. On a match, the record (timestamp, confidence, address) appears in attendance history; a
   second attempt the same day is blocked with "already marked today."

## Assumptions & limitations

- **Authentication is a placeholder**, not production auth: one hardcoded admin
  credential and a shared staff password, no hashing/backend — acceptable for this
  assignment's scope but not something to ship as-is.
- **One attendance record per staff per calendar day.** There's no clock-in/clock-out concept,
  no editing/deleting past records from the UI, and no admin override for a missed capture.
- **Face matching is single-reference, on-device, and threshold-based** — not a production-grade
  biometric system. It uses one averaged embedding per staff member (from 2 enrolment shots) and
  a fixed cosine-similarity threshold; it hasn't been tuned against a large real-world dataset,
  and there's no liveness/anti-spoofing beyond basic eyes-open/head-angle checks (a printed
  photo could plausibly pass).
- **Location is best-effort, not enforced geofencing.** The app does not restrict *where*
  attendance can be marked from — it only records the location for reference, and will still
  save an attendance record with a null location if no sufficiently fresh/accurate fix is
  available within the acquisition budget (e.g., indoors with poor GPS/network signal). Camera
  and location *permission* are hard requirements to attempt a capture; getting an actual fix is
  not.
- **No backend/sync** — all data (staff, enrolled faces, attendance, selfie images) is local to
  the device via Room + internal storage. Uninstalling the app or clearing its data deletes
  everything; there's no multi-device or admin-web view.
- **No automated test suite** covering the face-matching or location logic; verification during
  development was done via manual device/emulator testing and targeted logcat inspection.
- Tested primarily on Android 13 (a physical OnePlus 8) and an Android emulator; other OEM
  skins may behave differently around background location/battery restrictions.
