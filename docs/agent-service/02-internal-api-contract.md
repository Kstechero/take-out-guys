# 2026-07-22 补充：分类 Internal API 合同

- `GET /internal/agent/admin/categories`：管理端只读分类分页查询，参数 `name`、`query`（兼容别名）、`type`、`page`、`limit`；后端必须构造 `CategoryPageQueryDTO` 并调用 `CategoryService.pageQuery`。返回 `items/total/generated_at/scope/source`，其中 `total` 是分页服务返回的真实总数。
- `POST /internal/agent/admin/categories`：管理端受确认写接口，对应 Agent tool `create_admin_category`。请求体 `name/type/sort/audit_reason`，请求头必须包含 `X-Confirmation-Token` 和 `Idempotency-Key`，并受 `AGENT_INTERNAL_WRITES_ENABLED` 灰度开关控制。
- 新增分类执行时复用管理端 `CategoryService.save(CategoryDTO)`，因此新分类初始状态遵循现有 service 行为：默认停用。
# Java 涓?Python 鍐呴儴 API 濂戠害

> 鏈枃浠舵槸瀹炵幇鍓嶇殑鍗忚鍩虹嚎銆傛寮忚矾鐢便€佸瓧娈典笌閿欒鐮佺‘璁ゅ悗锛屽簲鍚屾鐢熸垚 OpenAPI 鏂囦欢锛涙湭鍚屾濂戠害涓嶅緱淇敼璺ㄦ湇鍔℃帴鍙ｃ€?
## 閫氱敤绾﹀畾

- Base URL锛氱敱 `SPRING_INTERNAL_BASE_URL` 閰嶇疆锛涗粎鍏佽鍐呯綉璁块棶銆?- 璇锋眰澶达細`X-Request-Id`銆乣X-Agent-Service-Token`銆乣X-Actor-Type`銆乣X-Actor-Id`銆乣X-Actor-Roles`銆乣X-Actor-Expires-At`銆?- 鎵€鏈夊搷搴斾娇鐢?UTF-8 JSON锛屽苟鍖呭惈 `request_id`銆?- 璇诲彇璇锋眰鍙湪缃戠粶澶辫触鏃堕噸璇曚竴娆★紱鍐欒姹傚繀椤绘惡甯?`Idempotency-Key`锛屼笖涓嶅緱鑷姩閲嶈瘯銆?
```json
{
  "ok": true,
  "data": {},
  "error_code": null,
  "message": "",
  "request_id": "6c08e50a-8e6a-4cf1-b2d6-4db0e1ad8cb8"
}
```

澶辫触鏃?`ok=false`锛宍error_code` 浠呭彲浣跨敤锛歚UNAUTHENTICATED`銆乣FORBIDDEN`銆乣NOT_FOUND`銆乣VALIDATION_ERROR`銆乣CONFLICT`銆乣RATE_LIMITED`銆乣UPSTREAM_ERROR`銆乣INTERNAL_ERROR`銆?
## Java 璋冪敤 Agent Service

- Base URL锛氱敱 Java `AGENT_SERVICE_BASE_URL` 閰嶇疆銆?- 璇锋眰澶达細`Content-Type: application/json`銆乣X-Agent-Service-Token`锛沗AGENT_SERVICE_AUTH_TOKEN` 鏈厤缃椂 Agent 瀵硅瘽鎺ュ彛杩斿洖 `503`銆?- Java 鍙紶閫掓渶灏?actor 涓婁笅鏂囷紝涓嶈浆鍙戠敤鎴?JWT銆?
### `POST /v1/user/chat`

```json
{
  "request_id": "uuid",
  "actor": {"type": "user", "id": 1001, "roles": ["USER"], "expires_at": "2026-07-10T13:00:00Z"},
  "session_id": "optional-id",
  "message": "甯垜鎵句竴浠戒笉杈ｇ殑鍗堥",
  "confirmed_action_token": null
}
```

鍝嶅簲瀛楁锛歚answer`銆乣status`銆乣session_id`銆乣citations`銆乣suggested_actions`銆乣confirmation`銆乣trace_id`銆俙status` 鍙?`completed`銆乣waiting_user`銆乣failed`銆乣unavailable`锛沗confirmation` 涓嶄负绌烘椂蹇呴』涓?`waiting_user`锛屼笖 `answer` 鍙鏄庡緟纭鎿嶄綔锛屼笉鑳藉０绉板凡缁忓畬鎴愩€?
### `POST /v1/admin/chat`

璇锋眰涓庡搷搴?envelope 鍜岀敤鎴锋帴鍙ｄ竴鑷达紝浣?`actor.type` 蹇呴』涓?`admin`锛岃鑹茶嚦灏戝寘鍚竴涓鐞嗙瑙掕壊銆傜鐞嗙宸ュ叿鍙敞鍐屽凡瀹屾垚鏉冮檺璇勫鐨勮兘鍔涳紱鏈縼绉昏兘鍔涘繀椤昏繑鍥炴槑纭竟鐣岋紝涓嶅緱鍥炲埌 Java 鎵嬪啓 Tool Calling銆?
### `POST /v1/user/chat/stream` 涓?`POST /v1/admin/chat/stream`

璇锋眰浣撲笌瀵瑰簲鍚屾鑱婂ぉ鎺ュ彛涓€鑷达紝鍝嶅簲涓?`text/event-stream`銆係pring Boot 閫愪簨浠朵唬鐞嗚鍝嶅簲锛涘鏃у墠绔殑 `delta.content` 鍜?`done.sessionId` 瀛楁缁х画鎻愪緵鍏煎鍒悕銆?
### SSE 浜嬩欢

| 浜嬩欢 | data 鍐呭 | 璇存槑 |
| --- | --- | --- |
| `run_started` | `{ "thread_id": "...", "trace_id": "..." }` | 涓€娆″浘杩愯寮€濮?|
| `node_started` | `{ "node": "classify_intent" }` | 鍥捐妭鐐瑰紑濮嬶紝涓嶆毚闇插唴閮?prompt |
| `tool_started` | `{ "tool": "menu_search" }` | 宸ュ叿寮€濮嬶紝涓嶆毚闇叉晱鎰熷弬鏁版垨鍐呴儴鍦板潃 |
| `tool_finished` | `{ "tool": "menu_search", "status": "ok" }` | 宸ュ叿缁撴潫 |
| `delta` | `{ "text": "..." }` | 鍥炵瓟鏂囨湰澧為噺 |
| `citation` | `{ "title": "浼樻儬鍒歌鍒?, "source": "...", "updated_at": "..." }` | RAG 鏉ユ簮 |
| `confirmation` | `{ "token": "...", "summary": "纭鍔犲叆 1 浠解€?, "expires_at": "..." }` | 鍐欐搷浣滅‘璁ゅ崱鐗?|
| `interrupt` | `{ "kind": "confirmation", "request": {} }` | 鍥炬殏鍋滅瓑寰呯敤鎴疯緭鍏?|
| `done` | `{ "session_id": "...", "trace_id": "...", "status": "completed" }` | 姝ｅ父缁撴潫鎴栨殏鍋滅瓑寰呯敤鎴凤紱Spring 浠ｇ悊淇濈暀璇ョ姸鎬?|
| `error` | `{ "code": "UPSTREAM_ERROR", "message": "..." }` | 鍙睍绀虹殑澶辫触淇℃伅 |

### `POST /v1/threads/{thread_id}/resume`

鎭㈠璇锋眰蹇呴』鎼哄甫 `agent_name`銆佹渶灏?actor銆乣request_id` 鍜屾壒鍑?缂栬緫/鎷掔粷鏁版嵁銆傛湇鍔＄鏍￠獙 thread銆丄gent 鍜?actor 涓€鑷村悗鎵嶈兘鎭㈠锛涙仮澶嶄笉寰楅噸鏀惧凡缁忕‘璁ょ殑鍐欐搷浣溿€?
### 缁撴瀯鍖栨帹鑽愪笌璇勪环鑽夌

| Agent API | 鐢ㄩ€?| 绾︽潫 |
| --- | --- | --- |
| `POST /v1/user/recommendations` | 鏍规嵁瀹炴椂鑿滃崟銆侀绠椼€佷汉鏁板拰楗鍋忓ソ鐢熸垚缁撴瀯鍖栨帹鑽?| 鍊欓€夊繀椤绘潵鑷?`/internal/agent/menu/search` |
| `POST /v1/user/reviews/draft` | 鐢熸垚灏氭湭鍙戝竷鐨勮瘎浠疯崏绋?| 蹇呴』鍏堣皟鐢?`/internal/agent/reviews/draft/check` 鏍￠獙鏈汉宸插畬鎴愯鍗曘€佽彍鍝佸綊灞炲拰鏁忔劅鍐呭 |

`POST /internal/agent/reviews/draft/check` 璇锋眰鍖呭惈 `order_id`銆乣dish_id`銆乣rating`銆乣highlights`锛屽彧杩斿洖鐢熸垚鑽夌鎵€闇€鐨勬渶灏忎簨瀹烇紝涓嶆彁浜よ瘎浠枫€?
## Python 璋冪敤 Spring Internal API

| Tool | 鏂规硶涓庤崏妗堣矾鐢?| actor | 椋庨櫓 | 鍏抽敭绾︽潫 |
| --- | --- | --- | --- | --- |
| `shop_status` | `GET /internal/agent/shop/status` | user/admin | read | 杩斿洖褰撳墠鐘舵€佸拰鏇存柊鏃堕棿 |
| `menu_search` | `GET /internal/agent/menu/search` | user/admin | read | query銆侀绠椼€佸彛鍛充负宸叉牎楠屽弬鏁?|
| `recent_orders` | `GET /internal/agent/orders/recent` | user | read | Java 鍥哄畾浣跨敤褰撳墠 actor 鏌ヨ |
| `get_order` | `GET /internal/agent/orders/{order_id}` | user/admin | read | Java 鏍￠獙璁㈠崟褰掑睘鎴栧憳宸ヨ鑹?|
| `get_cart` | `GET /internal/agent/cart` | user | read | 杩斿洖鑴辨晱涓旀渶灏忓寲鐨勮喘鐗╄溅鏁版嵁 |
| `list_addresses` | `GET /internal/agent/addresses` | user | read | 榛樿闅愯棌鎵嬫満鍙蜂笌璇︾粏闂ㄧ墝鍙?|
| `list_available_coupons` | `GET /internal/agent/coupons/available` | user | read | 鐢?Java 鎸夊綋鍓嶇敤鎴峰拰璁㈠崟鏉′欢璁＄畻 |
| `check_sensitive_words` | `POST /internal/agent/sensitive-words/check` | user/admin | read | 鍙敤浜庡唴瀹规鏌ワ紝涓嶆硠闇茶瘝搴撳叏鏂?|
| `add_to_cart` / `update_cart_item` / `remove_from_cart` / `clear_cart` | `POST /internal/agent/cart/changes` | user | write | 纭 token 涓庡箓绛夐敭蹇呭～锛涘姞璐粦瀹?`expected_unit_amount`锛屼环鏍兼垨涓婁笅鏋剁姸鎬佸彉鍖栨椂鎷掔粷 |
| `claim_coupon` | `POST /internal/agent/coupons/{coupon_id}/claim` | user | write | 纭 token 涓庡箓绛夐敭蹇呭～ |
| `query_business_overview` | `GET /internal/agent/admin/business/overview` | admin | read | 鏄惧紡鏃ユ湡鑼冨洿涓庢暟鎹彛寰?|
| `admin_order_search` / `admin_order_detail` | `GET /internal/agent/admin/orders[/{order_id}]` | admin | read | 鍒楄〃蹇呴』鏈夎鍗曞彿銆佺姸鎬佹垨鏃堕棿鑼冨洿锛涜繑鍥炶劚鏁忔憳瑕?|
| `admin_menu_search` / `admin_set_meal_search` | `GET /internal/agent/admin/menu`銆乣GET /internal/agent/admin/setmeals` | admin | read | 鍗曞簵鑼冨洿銆佹渶澶?20 鏉?|
| `admin_coupon_search` | `GET /internal/agent/admin/coupons` | admin | read | 杩斿洖閰嶇疆銆佷綑閲忓拰鏈夋晥鏈燂紝涓嶈繑鍥為鍙栫敤鎴?|
| `admin_review_search` | `GET /internal/agent/admin/reviews` | admin | read | 鐢ㄦ埛鍚嶅拰鍐呭鑴辨晱 |
| `set_shop_status` | `POST /internal/agent/admin/shop/status` | admin | write | 鐏板害寮€鍏炽€佺‘璁ゃ€佸箓绛夈€丷edis 涔愯閿佸拰瀹¤鐞嗙敱蹇呭～ |
| `update_order` | `POST /internal/agent/admin/orders/{order_id}/actions` | admin | write | 浠呭厑璁?2鈫?銆?鈫?銆?鈫?锛宍expected_status` 鍘熷瓙鏍￠獙 |
| `manage_coupon` | `POST /internal/agent/admin/coupons/{coupon_id}/actions` | admin | write | 浠呭厑璁稿惎鍋滐紝`expected_status` 鍘熷瓙鏍￠獙 |
| `create_admin_dish` | `POST /internal/agent/admin/menu/items` | admin | write | 鍗曡彍鍝佹柊澧烇紱瀛楁鐧藉悕鍗曘€佺‘璁ゃ€佸箓绛夈€佺伆搴﹀拰瀹¤鐞嗙敱蹇呭～ |
| `update_admin_dish` | `POST /internal/agent/admin/menu/items/{dish_id}/actions` | admin | write | 鍗曡彍鍝侀儴鍒嗗瓧娈典慨鏀癸紱缁戝畾 `expected_updated_at` 涔愯鐗堟湰锛屼笉寮€鏀惧彛鍛虫浛鎹?|
| `create_admin_coupon` | `POST /internal/agent/admin/coupons` | admin | write | 鍗曚紭鎯犲埜鏂板锛涢噾棰濄€佸彂琛岄噺銆侀檺棰嗘暟鍜屾湁鏁堟湡鐢?Java 鍐嶆牎楠?|

绠＄悊鏌ヨ鍝嶅簲缁熶竴鍖呭惈 `source=spring_internal_api`銆乣scope` 鍜?`generated_at`銆傚綋鍓嶄笟鍔＄郴缁熸槸鍗曢棬搴楅儴缃诧紝鑼冨洿鏄惧紡鏍囪涓?`single_store`銆傜鐞嗗啓鎺ュ彛榛樿鐢?`AGENT_INTERNAL_WRITES_ENABLED=false` 鍏抽棴锛岄獙鏀舵垨鐏板害鐜蹇呴』鏄惧紡寮€鍚€?
绠＄悊鍐欐搷浣滃彧淇敼鍗曚釜 Redis 鍊兼垨鍗曚釜涓氬姟鑱氬悎锛歊edis 浣跨敤 `WATCH/MULTI/EXEC`锛岃鍗曞拰浼樻儬鍒哥姸鎬佷娇鐢ㄥ甫 `expected_status` 鐨勫師瀛?SQL锛岃彍鍝佷慨鏀逛娇鐢?`expected_updated_at` 涔愯 SQL锛涘墠缃潯浠跺啿绐佹椂涓嶄骇鐢熷彉鏇达紝寮傚父鏃朵簨鍔″洖婊氥€傚箓绛夌姸鎬佽嫢杩涘叆 `FAILED_UNKNOWN`锛岀姝㈣嚜鍔ㄩ噸鏀撅紝鐢辩鐞嗗憳渚濇嵁缁撴瀯鍖栧璁℃棩蹇楁牳瀵硅祫婧愮幇鍊煎悗閲嶆柊鍙戣捣鏂扮殑纭锛屼綔涓哄畨鍏ㄨˉ鍋挎祦绋嬨€?
## 鍐欏伐鍏风‘璁ゅ崗璁?
1. Python 鍥捐妭鐐规牴鎹?Tool 鍙傛暟鐢熸垚 canonical action锛氬伐鍏峰悕銆乤ctor銆佽祫婧愩€佸弬鏁版憳瑕併€佽繃鏈熸椂闂淬€?2. 鏈嶅姟绔繚瀛樼‘璁よ褰曞苟杩斿洖涓嶅彲鐚滄祴鐨?`confirmation_token`锛屾湁鏁堟湡寤鸿 5 鍒嗛挓銆?3. 鐢ㄦ埛閫氳繃鍘熷璇濅細璇濇彁浜?token锛汸ython 楠岃瘉 token 涓庝細璇濄€乤ctor銆佹憳瑕佹湭鍙樸€?4. Python 璋冪敤 Java 鍐欐帴鍙ｏ紱Java 鍐嶆牎楠屾湇鍔¤韩浠姐€乤ctor銆佺‘璁?token銆佽祫婧愬綊灞炲拰 `Idempotency-Key`銆?5. Java 杩斿洖鏈€缁堜簨瀹烇紱妯″瀷鍙兘鎹鍥炵瓟鈥滃凡瀹屾垚鈥濇垨鈥滄湭瀹屾垚鈥濄€?
鎭㈠鎺ュ彛鐨?`decision` 鏀寔 `approve`銆乣edit`銆乣reject`銆俙edit` 蹇呴』鎼哄甫缁撴瀯鍖?`edited_arguments`锛涙湇鍔＄浣滃簾鏃?token銆佹寜瀵瑰簲 Tool schema 閲嶆柊鏍￠獙骞剁敓鎴愭柊 token锛屾棫 token 涓嶅緱鍐嶆鎵ц銆?
