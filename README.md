# QuietNight

Kotlin, Jetpack Compose Material 3, Hilt, Room 기반 Android 휴대폰·Wear OS 코골이 모니터링 앱입니다.

마이크 소리를 분석해 코골이를 감지하고 진동 알림을 제공합니다. 휴대폰에서는 선택적으로 짧은 WAV 녹음도 가능합니다. 측정 시간·코골이 시간·점수·분 단위 소리 추이를 날짜별로 저장합니다.

휴대폰의 설정 → 화면 테마에서 시스템·라이트·다크를 선택할 수 있습니다. 기존 기본값은 다크이며, 시스템을 선택하면 기기의 화면 설정을 따릅니다. 선택은 즉시 모든 화면과 시스템 바에 반영되고 앱을 다시 실행해도 유지됩니다.

하단 탐색은 선택한 탭을 배경색과 굵은 글씨로 강조합니다. 바 배경은 테두리 없이 화면 아래까지 이어지고, 내부 여백과 가변 높이로 시스템 내비게이션 영역과 큰 글자에 대응합니다. 버튼을 위로 띄우는 추가 제스처 여백은 두지 않습니다. 기록이 없는 분석 화면은 첫 밤 엽서 안내와 수면 탭 이동 버튼을 보여 주며, 버튼은 측정 화면만 엽니다.

홈은 기록이 없으면 첫 밤 안내와 측정 준비 팁을 보여 줍니다. 기록이 있으면 가장 최근 날짜의 밤 엽서·측정 시간·코골이 시간·진동 요청을 요약하고 분석으로 이동할 수 있습니다. 소리 추이가 없는 기록은 엽서를 만들어내지 않습니다. 측정 중에는 현재 소음·경과 시간·최근 감지 로그를 표시하며, 본문이 스크롤되어도 시작·종료 버튼은 하단에 유지됩니다.

수면 방향/자세는 측정하거나 저장하지 않습니다. v4의 방향 통계 컬럼을 제거하는 기존 마이그레이션은 수면 기록 보존을 위해 유지합니다. 녹음 파일과 수면 DB는 클라우드 백업 및 기기 이전 대상에서 제외합니다. 앱 삭제나 기기 변경 시 이 기록은 복원되지 않습니다.

## 밤 엽서와 진동 성적표

- **측정 확인**: 매 측정 시작 시 실제 오디오 20초를 모아 침실 소음을 확인합니다. 이때는 진동하지 않습니다. 읽은 샘플 길이를 넘어선 지연은 코골이 시간이 아니라 누락 시간으로 기록하고, 읽기 실패가 5초 이어지면 측정을 종료합니다.
- **진동 성적표**: 기본은 최소 60초 간격, 달력 날짜당 최대 30회입니다. 지원 기기에 진동 요청을 보낸 경우만 셉니다. 직전 5초 평균과 진동 직후 1초를 제외한 다음 5초 평균을 비교합니다. 도중 누락·종료·자정 이월로 비교를 마치지 못하면 ‘비교 불가’로 남깁니다. 3 이상 감소를 ‘소리 감소’로 표시하는 앱 내부 규칙이며 의학적 효과 판정 기준이 아닙니다. 진동을 실제로 느꼈는지는 확인할 수 없습니다.
- **밤 엽서**: 분석의 엽서 목록과 워치 기록 상세에서 확인합니다. 분 단위 최대 소리값을 최대 60개 구간으로 묶어 별과 선으로 그립니다. 소리가 클수록 별이 높아지며, 같은 기록은 같은 그림이 됩니다. 1분 기록이 없으면 임의의 그림을 만들지 않습니다. 원음·AI 서버·외부 전송·자동 공유는 사용하지 않습니다.
- **개인화**: 종료 후 날짜별로 ‘괜찮았어요 / 진동이 방해됐어요’를 남기거나 지울 수 있습니다. 최근 7일의 진동 요청이 있었던 날짜 중 최근 답변 3개에 방해됐다는 답변이 2개 이상이면 ‘여유 모드’를 제안합니다. 직접 적용할 때만 최소 120초 간격·하루 최대 10회로 바뀝니다. 설정에서 기본/여유 모드를 직접 선택할 수도 있습니다. 진동 세기를 자동으로 높이지 않습니다.

측정 정보는 오디오 읽기 확인·지속 소음 주의·누락 구간 주의·측정 정보 부족으로 구분합니다. 이것은 코골이 분류 정확도나 수면의 질 점수가 아닙니다. 현재 센서는 소리의 출처와 실제 수면 여부를 구분하지 않으며, 소리 감소는 진동의 효과·숙면·호흡 상태를 증명하지 않습니다. 마이크 위치, 주변 소음, 기기별 입력 특성이 결과에 영향을 줍니다.

기존 모델과 동일하게 자정에 날짜가 나뉘므로 ‘밤 엽서’도 달력 날짜별 기록입니다. 진동 횟수와 휴식은 저장된 요청에서 이어가며, 재시작 후 진행 중이던 전후 비교를 추정해서 완성하지 않습니다. 측정 중에는 피드백 입력·전체 삭제·개인화 제안 적용을 차단합니다. 서비스의 저장과 사용자 피드백 갱신은 Room 트랜잭션으로 분리하여 서로 덮어쓰지 않습니다.

DB는 v7이며 v6→v7에서 관찰 시간·진동 전후 기록·피드백 컬럼만 추가합니다. 과거 기록·클립·v5/v6 내보낸 스키마는 보존합니다. 과거 데이터의 새 관찰 정보는 빈 값으로 남아 ‘측정 정보 부족’으로 표시됩니다. 워치 독립 측정과 녹음 금지 정책은 그대로입니다.

## 구조

| 모듈 | 책임 | 프로젝트 의존성 |
| --- | --- | --- |
| `:app` | 공통 Application, Hilt 구현 바인딩, phone/wear APK 패키징, 런처 매니페스트·아이콘·기기 정책, 통합 테스트 | domain, data, ui |
| `:data` | Room 엔티티·DAO·마이그레이션, 도메인 모델 변환, SharedPreferences 저장소, Android 서비스·측정 제어 구현 | domain, core |
| `:ui` | 공통 ViewModel·SnoreIntent, phone/wear Activity·Compose 화면·탐색·권한 처리 | domain |
| `:domain` | 순수 Kotlin/JVM 수면 모델, 저장소·측정 제어 인터페이스, 날짜별 기록 조회 유스케이스, 코골이 지속 시간 판정 | 없음 |
| `:core` | 순수 Kotlin/JVM 공통 기술 기능: PCM 16-bit 샘플의 RMS 계산 | 없음 |

의존성은 `app → ui → domain`, `app → data → domain` 방향이며, `data`는 공통 오디오 계산을 위해 `core`도 사용합니다. `app`은 의존성 조립을 위해 `domain`을 직접 참조합니다. `ui`는 `data`나 `SnoreService`를 직접 참조하지 않습니다. `ArchitectureModule`이 도메인 인터페이스와 실제 구현을 조립합니다. `DBModule`은 data 내부의 Room 생성 설정을 담당합니다.

`domain/src/main/kotlin/com/example/quietnight/domain`은 Android·Room·Hilt에 의존하지 않습니다. 기존 `com.example.quietnight.domain.*` 패키지는 유지합니다. `ObserveSleepHistoryUseCase`는 주입된 `Clock`으로 오늘/어제 기록을 계산합니다. ViewModel은 `SleepRepository`와 `MonitoringController`를 사용하고, DB·파일·서비스 시작/종료 구현을 알지 못합니다. [Android 아키텍처 지침](https://developer.android.com/topic/architecture/data-layer)의 UI/데이터 소스 분리 원칙을 참고하되, 저장소 인터페이스는 클린 아키텍처의 안쪽 계층인 domain에 둡니다.

`core/src/main/kotlin/com/example/quietnight/core/audio/Pcm16Audio.kt`는 실제 읽은 샘플 구간의 RMS만 계산합니다. 코골이 임계값·400ms 판정은 domain, PCM 진폭 기준 dB 변환과 0~100 범위 처리는 data의 `calculateDecibels`, 오디오 읽기·세션 누적·저장은 서비스에 유지합니다. 현재의 dB 값은 기존 PCM 진폭 기반 수치이며 물리적 음압(dB SPL)으로 보정한 값은 아닙니다. core는 다른 프로젝트 모듈이나 코루틴 라이브러리에 의존하지 않습니다.

`ui/src/main`은 공통 ViewModel, `ui/src/phone`과 `ui/src/wear`는 기기별 UI를 담습니다. `app/src/main`은 공통 Application/DI/매니페스트, `app/src/phone`과 `app/src/wear`는 기기별 패키징 리소스/매니페스트를 담습니다. `:app`과 `:ui`에 같은 `device` dimension과 `phone`·`wear` flavor가 있어 Gradle이 대응하는 UI를 선택합니다. 별도 워치 앱 모듈이나 측정 로직 복제는 없습니다.

서비스가 측정 누적·자정 이월·자동 저장·진동을 담당하며, 400ms 감지 판정은 두 기기에서 같습니다. 알림은 해당 APK의 런처를 엽니다. 기존 Room 엔티티 이름·DB 이름·스키마 경로 `app/schemas`를 유지하며, 버전 7로 마이그레이션합니다. 스키마 생성은 `:data`의 KSP가 담당합니다.

상세 개발 지침은 `CLAUDE.md`, 작업 규칙은 `AGENTS.md`를 참고합니다.

## Build Variants

Gradle Sync 후 Android Studio의 **Build Variants** 창에서 `:app`의 Active Build Variant를 선택합니다. 별도 `:wear` 모듈은 사용하지 않습니다.

`domain`과 `core`는 순수 Kotlin/JVM 모듈이므로 Android Build Variants에 표시되지 않습니다. 앱을 빌드할 때 공통 JAR로 자동 포함되며, 단독 검증은 `:domain:test`와 `:core:test`로 실행합니다. `:ui`는 `:app`과 같은 phone/wear variant를 선택합니다.

| 대상 | Debug | Release | minSdk | versionCode |
| --- | --- | --- | --- | --- |
| 휴대폰 | `phoneDebug` | `phoneRelease` | 28 | 1 |
| Wear OS | `wearDebug` | `wearRelease` | 30 | 1000001 |

두 flavor의 applicationId는 기존과 같은 `com.example.quietnight`입니다. 설치된 앱의 기록을 유지할 수 있도록 suffix를 추가하지 않습니다. 각 APK는 해당 기기에 설치합니다. Release 빌드는 기존처럼 배포 서명을 자동 적용하지 않으므로 실제 배포 전 서명 설정이 필요합니다.

휴대폰 전용 Material 3·Navigation·아이콘 의존성은 `:ui`의 `phoneImplementation`, Wear Compose 의존성은 `wearImplementation`에 선언합니다. Compose UI 테스트도 `:ui`에 있어 `:app`은 Compose 컴파일러에 의존하지 않습니다. 기기별 코드는 런타임 분기 없이 소스 세트에서 선택됩니다. 자세한 규칙은 [Android build variants 문서](https://developer.android.com/build/build-variants)를 참고합니다.

## 워치 지원

Wear OS 3 이상(Android API 30 이상), 마이크가 있는 워치를 대상으로 합니다. Apple Watch 및 Tizen 워치는 이 Android APK로 지원하지 않습니다.

워치에서 독립적으로 측정을 시작·종료하고 현재 dB/경과 시간, 날짜별 점수·측정 시간·코골이 시간을 확인할 수 있습니다. 민감도 3단계와 진동 켜기/끄기를 제공합니다. 앱이 화면에 보일 때 시작하며, 마이크 권한이 없으면 허용을 요청합니다. 영구 거부 상태에서는 앱 설정으로 안내합니다.

휴대폰과 워치는 각각 로컬 DB를 사용합니다. 기록 동기화나 휴대폰 측정의 원격 제어·워치 진동 전달은 포함하지 않습니다. 워치는 `supports_snore_clips=false`로 소리 파일 녹음을 차단하며, 복원된 설정이 녹음 켜짐이어도 적용됩니다. 워치에서 밤새 마이크를 사용하므로 실제 기기의 배터리 지속 시간과 감지 성능 확인이 필요합니다.

공식 [Wear OS 패키징 지침](https://developer.android.com/training/wearables/packaging)에 따라 동일한 applicationId를 가진 별도 APK를 사용하고 워치에 독립 실행 속성과 별도 버전 코드를 설정합니다. 원형 화면 목록은 [Compose for Wear OS](https://developer.android.com/training/wearables/compose/lists)를 사용합니다.

```powershell
.\gradlew.bat :app:assemblePhoneDebug :app:assembleWearDebug
.\gradlew.bat :app:assemblePhoneRelease :app:assembleWearRelease
adb devices
# 실제 대상 기기의 serial로 바꿔 설치합니다.
adb -s PHONE_SERIAL install -r app/build/outputs/apk/phone/debug/app-phone-debug.apk
adb -s WATCH_SERIAL install -r app/build/outputs/apk/wear/debug/app-wear-debug.apk
```

## TDD 개발 절차

앞으로 기능 추가와 버그 수정은 다음 순서로 진행합니다.

1. 기대 동작과 경계 조건을 테스트로 작성합니다. 버그는 먼저 재현 테스트를 만듭니다.
2. 관련 테스트를 실행하고 의도한 이유의 실패를 확인합니다(Red).
3. 통과에 필요한 최소 구현을 적용합니다(Green).
4. 통과 상태에서 구조를 정리하고 관련 테스트를 다시 실행합니다(Refactor).
5. 전체 단위 테스트·Lint·빌드를 통과시킵니다. Android 동작을 변경하면 관련 계측 테스트와 실제 기기 확인을 추가합니다.

기존 코드에 대한 회귀 테스트는 처음부터 통과할 수 있습니다. 환경 설정 오류는 기능의 실패 재현으로 취급하지 않습니다. 마이크·서비스 테스트를 순수 계산 테스트로 대체하지 않으며, 미실행 검증은 별도로 기록합니다.

## 테스트 범위

| 테스트 | 검증 내용 | 실행 환경 |
| --- | --- | --- |
| `SnoreDetectorTest` | 순간 소음, 400ms 경계, 실제 청크 시간, 중복 합산 방지, 초기화, 임계값 변경 | JVM (domain) |
| `Pcm16AudioTest` | 무음·빈 입력, 부호, RMS 계산, 부분 버퍼, 극단값, 잘못된 길이, 입력 보존 | JVM (core) |
| `AudioLevelTest` | 기존 dB 수치·소수점 버림·부분 읽기 유지, 고정 시드 PCM의 기존 서비스 계산식과 동등성 | JVM (data) |
| `ChartDataTest` | 빈 기록/0점 구분, 날짜 구간, 주 시작 요일, 월/연도 경계, 윤년, 서머타임 | JVM |
| `BackupRulesTest` | 기존 백업·클라우드·기기 이전에서 녹음/DB 제외 | JVM |
| `ObserveSleepHistoryUseCaseTest` | 기록 부재, 서울 자정·연도 경계, 서머타임의 오늘/어제 구분 | JVM (domain) |
| `DomainMappersTest` | Room → 도메인 변환 시 ID·밀리초·타임라인·파일 경로 보존 | JVM (data) |
| `SleepViewModelTest` | 서비스 상태 반영, 중복 구독 방지, 삭제 위임, 로그·설정·클립 조회 | JVM (ui 두 flavor) |
| `ArchitectureBoundaryTest` | 5개 모듈 의존성 방향, domain/core의 JVM 독립성, core의 도메인 참조 금지, UI의 저장소/서비스 구현 참조 금지 | JVM (app 두 flavor) |
| `UiPermissionTest` | 휴대폰 진동 미리보기 UI 모듈의 자체 권한 선언 | JVM (ui phone) |
| `QuietNightDatabaseTest` | v4/v5/v6→v7 기록·클립 보존, 새 컬럼 기본값, 같은 날짜 업데이트, 측정 저장과 피드백 충돌 방지 | Android 기기/에뮬레이터 |
| `NightRecorderTest`, `AudioReadWindowTest` | 20초 환경 확인, 실제 읽기/누락 분리, 진동 전후 관찰·지터·중단, 재시작 횟수·간격 보존 | JVM (domain) |
| `RecommendGentleVibrationTest` | 고정 Clock·서울 시간대의 7일 범위, 중복/미래/오래된 날짜 제외, 명시적 피드백 조건 | JVM (domain) |
| `VibrationObservationCodecTest` | 완료/미완료 비교의 왕복 저장, 손상·미지원 포맷 방어 | JVM (data) |
| `NightPresentationTest` | 결정적인 엽서·최대 60개 구간·잘못된 값 방어, 비교 불가와 감소의 구분 | JVM (ui 두 flavor) |
| `GentleVibrationSettingsTest` | 여유 모드 저장·재생성·해제와 녹음 기본 꺼짐 보존 | Android 계측 |
| `ThemeSelectionTest` | 다크 기본값, 시스템 설정 추종, 명시적 테마 우선, ViewModel 설정 반영 | JVM (ui 두 flavor) |
| `ThemeSettingsTest` | 세 테마 저장·재생성, 다른 설정 보존, 미지원 값 기본 처리 | Android 계측 |
| `ThemeScreenTest` | 테마 선택과 즉시 전환, 홈·설정·분석 차트의 라이트 테마 상속 | Phone Compose 계측 |
| `HomeDashboardTest` | 두 테마의 첫 밤·준비 안내, 가장 최근 기록 요약·분석 이동, 기존 기록의 정보 부족 표시, 측정 중 종료, 큰 글자 스크롤과 시작 버튼 접근 | Phone Compose 계측 |
| `BottomNavAndEmptyAnalysisTest` | 두 테마의 선택 탭 강조·접근성 상태, 기본/2배 글자 크기의 라벨 잘림 방지, 화면 아래까지 이어지는 테두리 없는 바, 빈 분석 안내와 측정을 시작하지 않는 화면 이동 | Phone Compose 계측 |
| `NightJournalTest` | 엽서 상세 진입, 피드백 콜백, 측정 중 입력 차단 | Phone Compose 계측 |
| `FlavorPackagingTest` | 공통 Application, flavor별 런처 분리, 워치 필수 기능·메타데이터·녹음 차단 | JVM (두 flavor 공통) |
| `MonitoringActionTest` | 마이크 유무, 최초 권한 요청, 재요청/영구 거부, 권한 취소 후 종료 | JVM (`wear` flavor) |
| `WearScreenTest` | 워치 시작·종료 버튼, 진동 설정 콜백 | Wear OS 계측 |
| `WearIntegrationTest` | APK 런처, 공통 마이크 서비스 등록, 녹음 차단 리소스 | Wear OS 계측 |

DB 테스트는 매번 고유한 테스트 DB를 만들고 종료 시 해당 DB만 정리합니다. v5/v6는 내보낸 스키마를 읽고, 스키마 파일이 없는 v4는 기존 컬럼을 재현합니다. Room이 업그레이드된 DB를 열면서 현재 스키마도 검증합니다. ViewModel 테스트는 실패한 assertion도 정상 종료할 수 있도록 runTest 안의 finally에서 타이머를 정리합니다.

공통 ViewModel 테스트는 `ui/src/test`, 휴대폰 차트 테스트는 `ui/src/testPhone`, 워치 권한 테스트는 `ui/src/testWear`에 있습니다. 도메인 테스트는 `domain/src/test/kotlin`, PCM 계산 테스트는 `core/src/test/kotlin`, 백업·매퍼·dB 변환 테스트는 `data/src/test`에 있습니다. 패키징·아키텍처 테스트는 `app/src/test`, DB 통합 테스트는 `app/src/androidTest`, 워치 APK 통합 테스트는 `app/src/androidTestWear`, 워치 화면 테스트는 `ui/src/androidTestWear`에 있습니다.

## 검증 명령

JDK 21과 프로젝트가 사용하는 Android SDK가 필요합니다. Windows PowerShell에서는 저장소 루트에서 실행합니다.

```powershell
.\gradlew.bat :domain:test --tests 'com.example.quietnight.domain.SnoreDetectorTest'
.\gradlew.bat :core:test --tests 'com.example.quietnight.core.audio.Pcm16AudioTest'
.\gradlew.bat :core:test :domain:test :data:testDebugUnitTest :data:lintDebug
.\gradlew.bat :ui:testPhoneDebugUnitTest :ui:testWearDebugUnitTest :ui:lintPhoneDebug :ui:lintWearDebug
.\gradlew.bat :app:testPhoneDebugUnitTest :app:lintPhoneDebug :app:assemblePhoneDebug
.\gradlew.bat :app:testWearDebugUnitTest :app:lintWearDebug :app:assembleWearDebug
.\gradlew.bat :app:assemblePhoneDebugAndroidTest :app:assembleWearDebugAndroidTest
.\gradlew.bat :ui:assembleWearDebugAndroidTest
.\gradlew.bat :ui:assemblePhoneDebugAndroidTest
# 연결된 Android 기기/에뮬레이터가 있을 때 실행
.\gradlew.bat :app:connectedPhoneDebugAndroidTest
.\gradlew.bat :ui:connectedPhoneDebugAndroidTest
# 연결된 Wear OS 기기/에뮬레이터가 있을 때 실행
.\gradlew.bat :app:connectedWearDebugAndroidTest
.\gradlew.bat :ui:connectedWearDebugAndroidTest
```

macOS/Linux에서는 `./gradlew`를 사용합니다. 앱 테스트 결과는 `app/build/reports/tests/testPhoneDebugUnitTest/`, `testWearDebugUnitTest/`에, Lint 결과는 `app/build/reports/lint-results-phoneDebug.html`, `lint-results-wearDebug.html`에 생성됩니다. `assemble…AndroidTest`는 계측 테스트 APK를 빌드할 뿐 테스트를 실행하지 않습니다. 기기가 여러 대면 `ANDROID_SERIAL`을 대상 기기의 serial로 설정하고 계측 테스트를 실행합니다.

Windows PATH에 잘못된 따옴표가 있으면 테스트 JVM이 시작되지 않을 수 있습니다. 이 경우에만 현재 PowerShell 프로세스의 PATH를 정리하고 새 Gradle 프로세스에서 실행합니다. 시스템 환경 변수는 변경하지 않습니다.

```powershell
$env:Path = $env:Path.Replace('"', '')
.\gradlew.bat --no-daemon :core:test :domain:test :data:testDebugUnitTest :ui:testPhoneDebugUnitTest :ui:testWearDebugUnitTest :app:testPhoneDebugUnitTest :app:testWearDebugUnitTest
```

실제 기기에서는 마이크 권한 거부/허용, 측정 시작·종료, 알림 종료 액션, 화면이 꺼진 상태의 측정, 진동 쿨다운과 녹음 설정, 자정 이월을 확인합니다.

워치에서는 작은 원형 화면/큰 글꼴에서 모든 버튼에 접근 가능한지, 베젤·크라운 스크롤과 스와이프 뒤로 가기, 측정 중 화면 종료 후 복귀, 알림에서 종료·앱 열기, 장시간 배터리 소모를 추가로 확인합니다. 계측 테스트 APK 빌드 성공만으로 이 검증을 완료했다고 판단하지 않습니다.
