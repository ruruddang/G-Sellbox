# 📦 G-SellBox

마인크래프트 서버 전용 **판매 상자 플러그인**  
플레이어는 판매 상자를 열어 아이템을 판매/등록하고, 서버는 DB에 가격·카테고리를 저장하여 경제 플러그인(Vault)과 연동합니다.

---

## ✨ 주요 기능
- **DB 기반 관리**: 아이템 가격·카테고리를 MySQL에 저장 → 재시작/크래시에도 안전
- **Vault 연동**: 판매 정산 자동 처리
- **GUI 지원**: 직관적 상자 UI + 가격 정보(카탈로그) UI
- **검색/정렬/카테고리 필터**: 가격 정보 GUI에서 빠른 탐색
- **판매 로그 기록**: 정산 내역을 JSON으로 저장
- **플레이어별 판매 캐시**: 임시 보관 후 한 번에 정산
- **탭 자동완성**: 하위 명령어/카테고리 자동완성 지원

---

## 🕹️ 사용 방법
- **판매 상자 열기**: 판매 상자 블록(ItemsAdder) **우클릭**
- **가격 정보(카탈로그) 열기**: **웅크린 상태(Shift) + 우클릭**
- **관리자용 열람**: `/판매상자 <닉네임>`

---

## 📥 설치 방법
1. `plugins` 폴더에 `G-SellBox.jar` 배치
2. 서버 실행(최초 실행 시 `config.yml` 생성)
3. `config.yml`에서 DB/카테고리/GUI 설정 후 저장
4. 서버 재시작 또는 `/판매상자 리로드`

---

## ⚙️ config.yml 예시
```yaml
database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/gsellbox?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=utf8"
  username: "root"
  password: "password"
  pool:
    maximumPoolSize: 10
    minimumIdle: 2
    idleTimeout: 30000
    maxLifetime: 1800000
    connectionTimeout: 3000

gui:
  sell_title: "§0판매 상자"
  info_title_prefix: "§0판매 아이템: §8"

itemsadder:
  sell_block_id: "server:sell_box"

categories:
  - 농작물
  - 고기
  - 물고기
  - 광물
  - 요리
  - 보석
  - 채집
  - 식물
  - 기타

economy:
  decimals: 2           # 정산 금액 소수 자리수
  rounding: "HALF_UP"   # 반올림 정책 (FLOOR/CEIL/HALF_UP/HALF_EVEN 등)
