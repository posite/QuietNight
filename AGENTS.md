# QuietNight Codex Instructions

QuietNight is an Android sleep-monitoring app. The repository's detailed architecture, domain invariants, commands, permissions, persistence rules, and UI conventions are documented in `CLAUDE.md`.

## Required context

- Read `CLAUDE.md` before changing application code. Treat it as the detailed project guide and preserve its invariants.
- Use the existing Kotlin, Jetpack Compose Material 3, Hilt, Room/KSP, and Navigation Compose patterns.
- Keep user-facing strings and code comments in Korean when matching the surrounding code.
- On Windows, use `gradlew.bat`; on other platforms, use `./gradlew`.

## Change discipline

- Inspect the relevant existing code and tests before editing.
- Keep changes focused and avoid unrelated refactors.
- Preserve the service as the source of truth for monitoring-session accumulation and persistence.
- Route new ViewModel behavior through `SnoreIntent` and its public wrapper; keep `handleIntent` private.
- Preserve Room migration history and exported schemas when changing persistence.
- 모듈은 `:app`, `:data`, `:ui`, `:domain`, `:core` 다섯 개를 유지한다. 의존성은 `app → ui, data, domain`, `ui → domain`, `data → domain, core`만 사용한다. domain과 core는 Android·Room·Hilt에 의존하지 않는 Kotlin/JVM 모듈이며, 현재 서로를 포함한 프로젝트 의존성이 없다.
- `:app`과 `:ui`의 `device` dimension에 `phone`·`wear` product flavor를 사용한다. Activity·기기별 화면은 `ui/src/phone`과 `ui/src/wear`, 패키징 리소스·매니페스트는 `app/src/phone`과 `app/src/wear`, 공통 Application·구현 바인딩은 `app/src/main`에 둔다. 별도 `:wear` 모듈을 다시 만들지 않는다.
- 두 flavor는 측정 서비스·DB를 `:data`, ViewModel·SnoreIntent를 `:ui`, 도메인 모델·인터페이스·유스케이스·감지 규칙을 `:domain`에서 공유한다. UI는 DAO/Room 엔티티/서비스 구현을 참조하지 않는다. 측정/저장 로직을 기기별로 복제하지 않는다. 기기별 UI 의존성은 ui의 `phoneImplementation` 또는 `wearImplementation`에 선언한다.
- `:core`는 PCM RMS처럼 도메인과 무관한 순수 공통 계산만 담당한다. 수면 모델·코골이 판정·세션 누적·Android 입출력을 core에 넣지 않는다. 서비스의 PCM 진폭 기반 dB 변환은 data의 `calculateDecibels`에 유지하며, 실제 읽은 샘플만 사용하고 기존 감도·반올림 방식을 변경하지 않는다.
- 워치는 독립 측정 앱이다. 기기 간 동기화와 원격 제어는 별도 요구사항 없이는 추가하지 않는다. 워치의 `supports_snore_clips=false` 정책을 유지한다.
- Treat microphone access, foreground-service behavior, wake locks, clip recording, and notification actions as sensitive behavior. Test the narrowest relevant path after changes.

## Verification

### TDD (필수)

- 기능 추가와 버그 수정은 기대 동작을 검증하는 테스트부터 작성한다.
- 관련 테스트를 실행해 의도한 이유로 실패하는지 확인(Red)한 뒤, 통과에 필요한 최소 코드를 구현한다(Green).
- 테스트가 통과한 상태에서만 구조를 정리하고 다시 실행한다(Refactor). 기존 동작을 분리할 때는 먼저 회귀 테스트를 작성한다.
- 환경 오류나 컴파일 오류만으로 버그 재현을 완료했다고 판단하지 않는다. 가능한 경우 실제 동작의 assertion 실패를 확인한다.
- 시간·날짜 계산은 고정된 Clock과 명시적 시간대로 검증하고, Android 입출력과 순수 계산을 분리한다. 세션 누적·저장 책임은 서비스에 유지한다.
- DB 변경은 기존 데이터 보존 및 스키마 검증 계측 테스트를 작성하고, 마이그레이션과 내보낸 스키마 이력을 보존한다.
- 마이크·서비스·알림·진동·녹음 변경은 관련 단위 테스트와 기기 검증을 구분한다. 기기가 없어 실행하지 못한 항목은 결과에 명시한다.
- 검증 명령과 테스트 범위는 `README.md`를 참고한다. 테스트 실패를 숨기기 위해 테스트를 삭제하거나 비활성화하지 않는다.

For normal Kotlin/Android changes, run the relevant Gradle checks, usually:

```powershell
gradlew.bat :app:testPhoneDebugUnitTest :app:testWearDebugUnitTest
gradlew.bat :ui:testPhoneDebugUnitTest :ui:testWearDebugUnitTest
gradlew.bat :ui:lintPhoneDebug :ui:lintWearDebug
gradlew.bat :app:lintPhoneDebug :app:lintWearDebug
gradlew.bat :app:assemblePhoneDebug :app:assembleWearDebug
```

공통 모듈 또는 빌드 구성을 변경할 때는 다음 검증도 실행한다.

```powershell
gradlew.bat :core:test :domain:test :data:testDebugUnitTest :data:lintDebug
gradlew.bat :app:assemblePhoneRelease :app:assembleWearRelease
gradlew.bat :app:assemblePhoneDebugAndroidTest :app:assembleWearDebugAndroidTest
gradlew.bat :ui:assembleWearDebugAndroidTest
```

Instrumented tests require a connected device or emulator with the needed permissions and hardware capabilities.
