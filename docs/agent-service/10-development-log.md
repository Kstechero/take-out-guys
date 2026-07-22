# 2026-07-22 补充：分类查询与新增分类工具重写

## 本次完成
- 将 Agent 的分类查询从 `CategoryService.list(type)` 改为管理端分页服务 `CategoryService.pageQuery(CategoryPageQueryDTO)`，与后台分类管理页面保持同一查询口径。
- Python `admin_category_search` 支持 `name/type/page/limit`，保留 `query` 兼容别名，并在调用后端前移除旧的 `status` 过滤，避免 Agent 自己模拟管理端查询逻辑。
- 新增 `create_admin_category` 管理写工具：先查重并生成确认卡，确认后由 Spring Internal API 调用 `CategoryService.save(CategoryDTO)`。
- 新增后端 `InternalAgentAdminCategoryCreateDTO` 与 `POST /internal/agent/admin/categories`，保留确认 token、幂等键、灰度开关、审计理由和 Java 侧参数校验。
- 修复管理端系统提示词中“当前 Agent 不支持创建分类”的过期规则，明确新增分类、菜品、优惠券等写操作都必须走确认工具。
- 修复 `app/confirmations.py` 中历史乱码提示语导致的语法风险，新增分类确认执行链路已接入 `ConfirmationService`。

## 验证
- `python -m pytest tests/test_admin_tools.py tests/test_spring_internal_client.py`：20 项通过。
- `mvn -pl sky-server -am test-compile -DskipTests`：Java 主代码和测试代码编译通过。

## 当前 Agent 查询建议
- 查询“某分类下有多少菜品”：先 `admin_category_search(name=<分类名>, type=1, page=1, limit=20)` 获取分类 ID，再 `admin_menu_search(category_id=<分类ID>, status=1, page=1, limit=20)`，以 `total` 作为数量。
- 新增菜品缺少分类时：先用 `admin_category_search` 查已有分类；确实没有合适分类时，再用 `create_admin_category` 生成确认卡，确认完成后再新增菜品。
# Agent Service 寮€鍙戞棩蹇?
## 2026-07-22 琛ュ厖锛氱鐞嗙宸ュ叿杈圭晫銆佽彍鍝佸垎椤垫煡璇笌杈撳嚭绋冲畾鎬т慨澶?
### 鏈瀹屾垚
- 淇绠＄悊绔彧璇诲伐鍏?`limit` 瓒婄晫瀵艰嚧 LangChain 宸ュ叿璋冪敤澶辫触鐨勯棶棰橈細Python Pydantic schema 鍦ㄦ牎楠屽墠灏嗙鐞嗙鍙鏌ヨ鐨?`limit` 澶瑰埌鍚堝悓涓婇檺 20锛岄伩鍏嶆ā鍨嬩紶鍏?`limit=50` 鏃朵腑鏂暣杞璇濄€?- 灏?`admin_menu_search` 鍜?`admin_set_meal_search` 鏀逛负绠＄悊绔垎椤垫煡璇㈠彛寰勶細鏀寔 `name`銆乣category_id`銆乣status`銆乣page`銆乣limit`锛屽苟淇濈暀 `query` 浣滀负 `name` 鐨勫吋瀹瑰埆鍚嶃€?- Spring Internal API 鐨?`/internal/agent/admin/menu` 鐢辨墜宸?`listWithFlavor()` 鍚庤繃婊わ紝鏀逛负鏋勯€?`DishPageQueryDTO` 骞惰皟鐢?`DishService.pageQuery()`锛沗/internal/agent/admin/setmeals` 鍚岀悊鏀逛负 `SetmealService.pageQuery()`銆?- 鑿滃搧/濂楅鏌ヨ杩斿洖鐨?`total` 鐜板湪鏉ヨ嚜绠＄悊绔垎椤垫湇鍔＄殑鐪熷疄鎬绘暟锛宍items` 浠呰〃绀哄綋鍓嶉〉锛岄伩鍏嶁€滄寜鍒嗙被鏌ヨ鍗存€绘槸杩斿洖鍚屼竴鎵瑰墠 20 鏉♀€濈殑璇垽銆?- 绠＄悊绔郴缁熸彁绀鸿瘝鏂板涓氬姟鑼冨洿杈圭晫锛氬彧鍥炵瓟澶栧崠骞冲彴绠＄悊绔繍钀ラ棶棰橈紝涓嶅啀鍥炵瓟鍐掓场鎺掑簭銆侀€氱敤缂栫▼鏁欏銆侀棽鑱娿€佸啓浣溿€佺炕璇戠瓑鏃犲叧闂銆?- 绠＄悊绔浘鍏ュ彛鏂板浠ｇ爜绾?domain guard锛氭槑鏄鹃潪绠＄悊绔笟鍔￠棶棰樺湪杩涘叆妯″瀷鍜屽伐鍏峰墠鐩存帴杩斿洖鍥哄畾鎷掔瓟锛岄槻姝㈢鐞嗙 Agent 閫€鍖栦负閫氱敤鑱婂ぉ鍔╂墜銆?- 娓呯悊 `admin_queries.py` 涓殑涔辩爜宸ュ叿鎻忚堪锛屼娇妯″瀷鑳界湅鍒版竻鏅扮殑鑿滃搧/濂楅鍒嗛〉鏌ヨ鍙傛暟鍜屽垎绫绘煡璇娇鐢ㄦ柟寮忋€?- 鍚屾璐ㄩ噺闂ㄧ鐭╅樀锛屽皢宸叉敞鍐岀殑 `admin_category_search` 绾冲叆 `tool_test_matrix.json`銆?
### 楠岃瘉
- `python -m pytest`锛?6 椤归€氳繃銆?- `mvn -pl sky-server -am clean test-compile -DskipTests`锛氬悗绔富浠ｇ爜涓庢祴璇曚唬鐮佺紪璇戦€氳繃銆?- `mvn -pl sky-server -Dtest=InternalAgentAdminControllerTest test` 褰撳墠椤圭洰 Surefire 閰嶇疆鏈彂鐜?JUnit 5 娴嬭瘯锛屽嚭鐜?`Tests run: 0`锛涙湰娆′互鍚庣 `test-compile` 瑕嗙洊 Java 缂栬瘧姝ｇ‘鎬с€?
### 褰撳墠鏌ヨ寤鸿
- 鏌ヨ鏌愬垎绫讳笅鑿滃搧鏁伴噺锛氬厛璋冪敤 `admin_category_search(query="涓婚", type=1, status=1)` 鑾峰彇鐪熷疄鍒嗙被 ID锛屽啀璋冪敤 `admin_menu_search(category_id=<鍒嗙被ID>, status=1, page=1, limit=20)`锛屼互杩斿洖鐨?`total` 浣滀负鐪熷疄鏁伴噺銆?- 濡傛灉 `total > limit`锛岀户缁敤 `page=2/3...` 缈婚〉鏌ョ湅鏄庣粏锛屼笉搴旇妯″瀷鍑綋鍓嶉〉鏉℃暟鎺ㄦ柇鎬绘暟銆?
## 2026-07-22

### 鏈瀹屾垚
- 瀹屾垚 P0鈥揚4 浜や粯瀹¤锛氬悓姝ュ搷搴斿鍔?`status`锛孲SE 淇濈暀鍥捐妭鐐广€佸伐鍏枫€佸紩鐢ㄣ€佺‘璁ゃ€佷腑鏂拰鏈€缁堢姸鎬佷簨浠躲€?- 鐢ㄦ埛鍥炬敼涓烘樉寮忚妭鐐瑰苟浣跨敤 LangGraph 鍘熺敓 `interrupt()`锛汼QLite checkpointer 鍜岀‘璁ゅ簱鏀寔杩涚▼閲嶅惎鍚庢仮澶嶏紝搴旂敤閫€鍑烘椂鍏抽棴杩炴帴銆?- 鐢ㄦ埛鍔犺喘銆佽喘鐗╄溅淇敼/鍒犻櫎/娓呯┖鍜岄鍒告帴鍏ユ壒鍑嗐€佺紪杈戙€佹嫆缁濄€佽繃鏈熴€乤ctor/session 缁戝畾鍙婁竴娆℃€х‘璁わ紱鍔犺喘缁戝畾棰勮浠锋牸锛孞ava 鍐嶆牎楠屽湪鍞姸鎬佸拰瀹炴椂浠锋牸銆?- 鐙珛绠＄悊鍥炬帴鍏ヨ鍗曘€佸晢鍝併€佸椁愩€佷紭鎯犲埜銆侀棬搴椼€佽瘎浠枫€佺粡钀ョ粺璁′笌杩愯惀鐭ヨ瘑鏌ヨ锛岀粨鏋滅粺涓€杈撳嚭鏉ユ簮銆佸崟搴楄寖鍥村拰鐢熸垚鏃堕棿銆?- 寮€鏀鹃棬搴楃姸鎬併€佽鍗曟祦杞€佷紭鎯犲埜鍚仠涓変釜鍙楁帶绠＄悊 Tool锛氶粯璁ゅ叧闂伆搴﹀紑鍏筹紝Java 浜屾閴存潈銆佸瓧娈电櫧鍚嶅崟銆侀鏈熺姸鎬佷箰瑙傛牎楠屻€丷edis/SQL 鍘熷瓙鍙樻洿銆?4 灏忔椂骞傜瓑鐘舵€佸拰缁撴瀯鍖栧璁℃棩蹇楅綈鍏ㄣ€?- 琛ラ綈绠＄悊 Agent 鏂板鑿滃搧鎵ц绔偣锛屽苟鏂板鍗曡彍鍝佷箰瑙備慨鏀瑰拰鏂板浼樻儬鍒革紱涓夎€呭潎浣跨敤纭鍗°€佺伆搴︺€佸箓绛夈€丣ava 涓氬姟浜屾鏍￠獙涓庣粨鏋勫寲瀹¤銆傜鐞嗙纭鍗″睍绀烘棫鍊?鏂板€艰鎯呫€?- Spring 鐢ㄦ埛绔笌绠＄悊绔€傞厤纭鍗°€佹仮澶嶆帴鍙ｅ拰鐘舵€佸瓧娈碉紱鐢ㄦ埛绔敮鎸佷慨鏀规暟閲忥紝涓や釜鍓嶇鍧囨敮鎸佹壒鍑?鍙栨秷銆?- Spring Internal API 瀹㈡埛绔鍙璇锋眰澧炲姞涓€娆′紶杈撳眰澶辫触閲嶈瘯锛涘啓璇锋眰淇濇寔鍗曟灏濊瘯锛岄伩鍏嶉噸澶嶄笟鍔″壇浣滅敤銆?- 鏂板璇昏姹傚彲閲嶈瘯銆佸啓璇锋眰涓嶉噸璇曠殑鍥炲綊娴嬭瘯銆?- Agent Service 娴嬭瘯銆侀潤鎬佹鏌ヤ笌瀛楄妭鐮佺紪璇戝叏閮ㄩ€氳繃銆?
### 楠岃瘉
- `pytest -q`锛?9 椤归€氳繃锛沗ruff check app tests scripts` 涓?`compileall` 閫氳繃銆?- `mvn.cmd -q clean test`锛?3 椤归€氳繃锛? 澶辫触銆? 閿欒銆?- 绠＄悊绔?`vue-tsc` 涓?Vite production build 閫氳繃锛涚敤鎴风鑱婂ぉ SFC 瑙ｆ瀽鍜岃剼鏈紪璇戦€氳繃銆?
### P0鈥揚4 鍓╀綑杈圭晫
- P5 宸茶ˉ鍏?Prometheus 鎸囨爣銆佺粨鏋勫寲璇锋眰 trace銆佹寜瀹㈡埛绔檺娴併€丼pring Internal API 鐔旀柇銆丳ostgreSQL 鍏变韩 checkpointer銆佺敓浜?Compose 鍜屽洖婊氭墜鍐岋紱妯″瀷 token 鎴愭湰缁х画閫氳繃妯″瀷渚涘簲鏂?usage/璐﹀崟姹囨€汇€?- 绠＄悊鍐欎粎寮€鏀惧凡璇勫鐨勫崟璧勬簮鍔ㄤ綔锛涘垹闄ゃ€佹壒閲忎慨鏀广€佸憳宸ユ潈闄愩€佸椁愬拰鏁忔劅璇嶅啓鍏ヤ粛涓嶆敞鍐屻€?
鏈枃浠舵寜鏃ユ湡璁板綍 Agent 寰湇鍔¤縼绉荤殑瀹為檯寮€鍙戝唴瀹广€侀獙璇佺粨鏋溿€佹湭瀹屾垚椤瑰拰椋庨櫓銆傛柊璁板綍杩藉姞鍦ㄩ《閮紝涓嶈鐩栧巻鍙茶褰曘€傚綋鍓嶇洰鏍囨柟妗堜互 `LANGCHAIN_RAG_AGENT_MICROSERVICE_PLAN.md` 涓哄噯锛屽巻鍙茶褰曚腑鐨勯樁娈靛悕绉般€佸伐鍏锋暟閲忓拰娴嬭瘯鏁伴噺涓嶈嚜鍔ㄨ涓哄綋鍓嶉獙鏀跺彛寰勩€?
## 2026-07-13

### 宸插畬鎴?
- 寤虹珛 FastAPI 鍒嗗眰缁撴瀯锛欰PI銆丟raph銆乀ools銆丼pring Internal API Client銆丳ydantic schema銆乸rompt 鍜屼緷璧栨敞鍏ャ€?- 瀹炵幇鐢ㄦ埛渚?`POST /v1/user/chat`锛屽畬鎴愯惀涓氱姸鎬佷笌鑿滃崟鎼滅储鐨勯鎵瑰彧璇荤紪鎺掋€?- 瀹炵幇 `get_shop_status`銆乣menu_search` 涓や釜 LangChain 鍏煎 Tool锛汸ython 涓嶇洿杩炰笟鍔℃暟鎹簱銆?- Spring Boot 鐢ㄦ埛瀵硅瘽鍏ュ彛鏀逛负鐩存帴璋冪敤 Agent API锛岀Щ闄?`UserAiChatServiceImpl` 瀵规墜鍐?`AiToolCallingClient`銆丷egistry銆丒xecutor 鍜?Java RAG 缂栨帓鐨勪緷璧栥€?- Spring Boot 绠＄悊绔亰澶┿€丼SE 涓庡仴搴锋鏌ユ敼涓鸿皟鐢?Agent API锛岀Щ闄?`AdminAiChatServiceImpl` 瀵规墜鍐?Tool Calling 鍜岀洿杩炴ā鍨嬪仴搴锋鏌ョ殑渚濊禆銆?- 鏂板 `POST /v1/admin/chat`锛屽己鍒?admin actor锛涚鐞嗙宸ュ叿鏈縼绉绘椂杩斿洖鏄庣‘鑳藉姏杈圭晫銆?- 淇濇寔鍘?`/user/ai/chat` 涓?`/user/ai/chat/stream` 鍓嶇鍗忚锛汼SE 褰撳墠鐢?Java 寮傛璋冪敤 Agent API 鍚庤浆涓哄吋瀹逛簨浠躲€?- 鏂板 Spring Boot `/internal/agent/shop/status` 涓?`/internal/agent/menu/search`锛岃繑鍥炵粺涓€ envelope锛屽苟闄愬埗鑿滃崟缁撴灉鏈€澶?10 椤广€?- Java 鍒?Python銆丳ython 鍒?Java 鍧囧己鍒?`X-Agent-Service-Token` 鏈嶅姟璁よ瘉锛沘ctor 涓婁笅鏂囦粎鍖呭惈绫诲瀷銆両D銆佽鑹插拰鐭湡杩囨湡鏃堕棿锛屼笉杞彂鐢ㄦ埛 JWT銆?- 鏇存柊 ADR锛氱敤鎴蜂晶鐩存帴浣跨敤 Agent API锛屼笉缁存姢 `legacy|python` 杩愯鏃跺弻璺敱锛涙晠闅滈噰鐢ㄥ弸濂介檷绾т笌閮ㄧ讲鐗堟湰鍥炴粴銆?- 鎺ュ叆 OpenAI-compatible `ornith` 妯″瀷锛屾ā鍨?URL銆並ey銆佹ā鍨嬪悕銆佹俯搴︺€佽秴鏃跺拰鏈€澶?token 鍧囩敱鐜鍙橀噺閰嶇疆锛孉PI Key 鏈啓鍏ヤ粨搴撱€?- 灏嗗叧閿瘝瑙勫垯璺敱鏇挎崲涓虹湡瀹?LangGraph `model 鈫?tools 鈫?model` 寰幆锛岀敱妯″瀷閫夋嫨 `get_shop_status` 鎴?`menu_search`銆?- 浣跨敤 LangGraph `MemorySaver` 淇濆瓨鏈繘绋嬪唴鐨勫杞秷鎭紝骞朵互 `actor_type + actor_id + session_id` 闅旂浼氳瘽銆?- 鏂板鐢ㄦ埛绔拰绠＄悊绔嫭绔嬬郴缁熸彁绀鸿瘝銆佹ā鍨嬪紓甯稿畨鍏ㄩ檷绾у拰鑴辨晱缁撴瀯鍖栭敊璇棩蹇椼€?- 鏂板 Agent Service 娴佸紡鎺ュ彛锛汮ava SSE 浠ｇ悊閫?token 杞彂 `delta`銆乣tool_status` 鍜?`done`锛屽悓鏃朵繚鐣欐棫鍓嶇瀛楁鍒悕銆?
### 绗簩闃舵锛氱敤鎴峰彧璇诲伐鍏?
- 鏂板鏈€杩戣鍗曘€佽鍗曡鎯呫€佽喘鐗╄溅銆佽劚鏁忓湴鍧€銆佸彲鐢ㄤ紭鎯犲埜銆佹晱鎰熻瘝妫€鏌?6 缁?Spring Internal API 涓?LangChain Tools銆?- Python 渚т娇鐢ㄤ弗鏍?Pydantic 杈撳叆/杈撳嚭妯″瀷锛屾墍鏈夊疄鏃跺拰鐢ㄦ埛绉佹湁鏁版嵁鍙€氳繃 Spring Internal API 鑾峰彇锛屼笉鐩磋繛涓氬姟鏁版嵁搴撱€?- Spring 閴存潈鎷︽埅鍣ㄥ湪璇锋眰鏈熼棿鍐欏叆 `BaseContext`锛屽苟鍦ㄨ姹傜粨鏉熷悗娓呯悊锛屽鐢ㄧ幇鏈夎鍗曞綊灞炲拰褰撳墠鐢ㄦ埛鏌ヨ瑙勫垯銆?- 鐢ㄦ埛绉佹湁宸ュ叿浠呭悜 user actor 娉ㄥ唽锛沘dmin actor 鍙兘浣跨敤鍏叡鍙宸ュ叿鍜屾晱鎰熻瘝妫€鏌ャ€?- Internal API 杩斿洖鏈€灏忓繀瑕佸瓧娈碉細璁㈠崟涓嶅寘鍚墜鏈哄彿銆佸畬鏁村湴鍧€鍜岀敤鎴?ID锛涘湴鍧€浠呰繑鍥炲鍚嶃€佺數璇濆強闂ㄧ墝鎺╃爜锛涙晱鎰熻瘝妫€鏌ヤ笉杩斿洖璇嶅簱鍛戒腑鍒楄〃銆?- 鏂板 Internal API 涓撶敤寮傚父鏄犲皠锛岀粺涓€杩斿洖 `FORBIDDEN`銆乣NOT_FOUND`銆乣VALIDATION_ERROR` 鎴?`INTERNAL_ERROR` envelope銆?
### 绗笁闃舵锛歊AG 鐭ヨ瘑搴?
- 寤虹珛 `docs/agent-service/knowledge/` 瀹℃牳鐭ヨ瘑鐩綍鍜屾枃妗ｆ竻鍗曘€?- 浠?legacy 鍚庣瀹為檯涓氬姟鏍￠獙涓暣鐞嗕笅鍗曢厤閫併€佸彇娑堥€€娆俱€佷紭鎯犲埜銆佽瘎浠峰鏈嶅強绠＄悊绔鍗?SOP 鍏?5 浠藉彲绱㈠紩鏂囨。銆?- 鎵€鏈夋鏂囧鍔犵粺涓€ YAML 鍏冩暟鎹€佷唬鐮佷簨瀹炴潵婧愩€佸彲瑙佹€у拰瀹炴椂鏁版嵁杈圭晫锛屾湭鍐欏叆鐢ㄦ埛鏁版嵁銆佷氦鏄撴槑缁嗘垨瀵嗛挜銆?- 鏄庣‘閰嶉€佽寖鍥淬€侀€€娆惧埌璐︽椂闂淬€佽禂浠樺拰娲诲姩鍙犲姞绛夋殏鏃犵郴缁熶緷鎹殑鍦烘櫙蹇呴』鎷掔粷鐚滄祴鎴栬浆浜哄伐銆?- 瀹炵幇 approved Markdown front matter 鏍￠獙銆佹爣棰樿涔夊垏鍒嗐€佸唴瀹?hash 鍜屽彲閲嶅澧為噺鍏ュ簱锛涙枃妗ｅ垹闄ゆ垨鍙鎬у彉鏇翠細娓呯悊鏃у悜閲忋€?- 瀹炵幇 2048 缁存湰鍦板搱甯屽悜閲忕储寮曘€佷綑寮︽绱€佷笟鍔″煙棰勮繃婊ゃ€佷簩鍏冪粍杞婚噺閲嶆帓鍜?`0.15` 缃俊搴﹂槇鍊笺€?- 瀹炵幇 user/public/admin 妫€绱㈠墠鍙鎬ц繃婊わ紝user 鏃犳硶鍙洖绠＄悊绔?SOP锛宎dmin 涓嶄細鍙洖鐢ㄦ埛鐭ヨ瘑銆?- 娉ㄥ唽 `search_service_knowledge` 涓?`search_operational_knowledge` LangChain Tools锛岃繑鍥?snippets銆乻core 鍜?citation銆?- 闈炴祦寮忓搷搴旇繑鍥炵粨鏋勫寲 citations锛汼SE 鏂板 `citation` 浜嬩欢锛涚煡璇嗘棤渚濇嵁鏃?Graph 寮哄埗杩斿洖缁熶竴鎷掔瓟锛岄樆姝㈡ā鍨嬭ˉ鍏ㄣ€?- 鏂板 20 鏉″簲鍛戒腑涓?10 鏉″簲鎷掔瓟鐨勭绾?RAG 璇勬祴闆嗭紝瑕嗙洊鍏ㄩ儴棣栨壒鐭ヨ瘑鍩熷拰璺ㄨ鑹茶秺鏉冦€?
### 楠岃瘉

- `pytest -q`锛?6 鏉℃祴璇曢€氳繃锛屽寘鍚叆搴撱€佸閲忓鐢ㄣ€佸垹闄ゆ竻鐞嗐€佸彲瑙佹€с€?0 鏉?RAG 璇勬祴銆佸己鍒舵嫆绛斿拰 SSE 寮曠敤浜嬩欢銆?- `ruff check app tests scripts`锛氶€氳繃銆?- `python -m compileall -q app scripts`锛氶€氳繃銆?- `python -m scripts.ingest_knowledge`锛? 浠芥枃妗ｇ敓鎴?14 涓?chunk锛涚浜屾鎵ц澶嶇敤鍏ㄩ儴 14 涓悜閲忋€?- `mvn -pl sky-server -am test`锛歊eactor 鏋勫缓閫氳繃锛? 鏉?Java 娴嬭瘯閫氳繃銆?- 鐪熷疄妯″瀷鍩虹璋冪敤锛氶€氳繃锛涜惀涓氱姸鎬佸拰鑿滃崟闂鍧囪繑鍥炴爣鍑?OpenAI Tool Call銆?- 鐪熷疄妯″瀷 + 妯℃嫙 Spring Internal API 瀹屾暣闂幆锛氳惀涓氬洖绛斻€佽彍鍗曞洖绛斿拰 citation 鍧囬€氳繃銆?- 鐪熷疄妯″瀷娴佸紡闂幆锛歚delta`銆乣tool_status`銆乣done` 鍧囬€氳繃銆?
### 鍘嗗彶寰呮帹杩涳紙鐜板凡瀹屾垚锛?
- 纭 token銆佸箓绛夊拰鍐欏伐鍏蜂袱闃舵纭宸插湪 2026-07-22 P2/P4 瀹屾垚銆?- 绠＄悊绔彧璇?Internal API 涓?LangChain Tools 宸插湪 2026-07-22 P3 瀹屾垚銆?
### 宸茬煡闄愬埗

- `/user/ai/recommend` 宸茶浆鍙戠粨鏋勫寲 Agent `/v1/user/recommendations`锛屽€欓€夋潵鑷疄鏃惰彍鍗?Tool锛屾ā鍨嬪彧鐢熸垚鎺ㄨ崘鎬荤粨銆?- `/user/ai/review/write` 宸茶浆鍙戠粨鏋勫寲 Agent `/v1/user/reviews/draft`锛汼pring 鍏堟牎楠屾湰浜哄凡瀹屾垚璁㈠崟銆佽彍鍝佸綊灞炲拰鏁忔劅鍐呭锛孉gent 鐢熸垚鏈彂甯冭崏绋裤€?- Internal API 闇€瑕佽缃?`AGENT_INTERNAL_AUTH_TOKEN`锛屽苟涓?Agent Service 鐨?`SPRING_INTERNAL_AUTH_TOKEN` 淇濇寔涓€鑷淬€?- Agent API 涓ょ闇€瑕佷娇鐢ㄧ浉鍚岀殑 `AGENT_SERVICE_AUTH_TOKEN`锛涙湭閰嶇疆鏃跺璇濇帴鍙ｄ細鎷掔粷鏈嶅姟銆?- 鏈湴榛樿浣跨敤 SQLite锛涚敓浜?Compose 浣跨敤 PostgreSQL `AsyncPostgresSaver` 鍏变韩鍥剧姸鎬侊紝纭瀛樺偍浣跨敤鍏变韩鎸佷箙鍗峰苟瑕佹眰浼氳瘽绮樻€ц矾鐢便€?- 褰撳墠榛樿 embedding 涓虹绾垮瓧绗?n-gram 鍝堝笇鍚戦噺锛岄€傚悎鏈」鐩皬鍨嬩腑鏂囪鍒欏簱锛涚煡璇嗚妯℃墿澶у墠搴旇瘎娴嬪苟鍒囨崲鐢熶骇璇箟 embedding 涓庡悜閲忔暟鎹簱銆?# 2026-07-22 琛ュ厖锛氱鐞嗙纭涓庤彍鍝佸垎绫诲畨鍏ㄤ慨澶?
### 鏈瀹屾垚
- 淇娴佸紡 Agent 璇锋眰鎼哄甫 `confirmed_action_token` 鏃舵湭杩涘叆纭鎵ц閾捐矾鐨勯棶棰橈紱娴佸紡纭鐜板湪浼氱洿鎺ヨ皟鐢ㄧ‘璁ゆ墽琛岄€昏緫骞惰繑鍥?`done` 鐘舵€侊紝閬垮厤鎶娾€滅‘璁も€濆綋浣滄櫘閫氭秷鎭啀娆￠€佸叆 `admin_agent` 鑺傜偣銆?- 鏂板绠＄悊绔垎绫绘煡璇㈣兘鍔涳細Spring Internal API 澧炲姞 `/internal/agent/admin/categories`锛孭ython client/schema/tool 澧炲姞 `admin_category_search`銆?- 鏂板鑿滃搧鍓嶈姹?Agent 鏌ヨ鐪熷疄鍚敤鑿滃搧鍒嗙被锛屽苟鍦?Python `create_admin_dish` 宸ュ叿鐢熸垚纭鍗＄墖鍓嶆牎楠?`category_id`銆?- Spring 鍐欏叆渚ф柊澧炰簩娆℃牎楠岋細鑿滃搧鏂板鏃?`category_id` 蹇呴』寮曠敤宸插惎鐢ㄧ殑鑿滃搧鍒嗙被锛岄槻姝㈡ā鍨嬫垨璋冪敤鏂逛紶鍏ヤ笉瀛樺湪鐨勫垎绫?ID銆?- 鎭㈠绠＄悊绔郴缁熸彁绀鸿瘝涓哄彲璇讳腑鏂囷紝骞舵槑纭柊澧炶彍鍝佸墠蹇呴』鏌ヨ鍒嗙被銆佷笉鑳界紪閫犲垎绫诲悕绉版垨 `category_id`銆?- README 鍜?Agent Service 寮€鍙戝懡浠ゆ敼涓虹洿鎺ヤ娇鐢ㄦ湰鏈?Python锛歚python -m pip install -e ".[dev]"`銆乣python -m uvicorn ...`銆乣python -m pytest`銆?
### 楠岃瘉
- `python -m pytest agent-service\tests\test_admin_tools.py agent-service\tests\test_spring_internal_client.py agent-service\tests\test_admin_operations_graph.py`锛?7 椤归€氳繃銆?- `mvn -pl sky-server -Dtest=InternalAgentAdminControllerTest test`锛氭湭瀹屾垚锛涢娆″洜娌欑缃戠粶鏃犳硶涓嬭浇 Maven parent POM 澶辫触锛岄殢鍚庣綉缁滄巿鏉冭姹傝鍙栨秷銆?
### 椋庨櫓涓庡悗缁?- 闇€瑕佸湪鍏峰 Maven 渚濊禆缂撳瓨鎴栧厑璁哥綉缁滀笅杞界殑鐜涓ˉ璺?`InternalAgentAdminControllerTest`銆?- 鑻ュ墠绔粛閫氳繃鑷劧璇█鈥滅‘璁も€濆彂閫佹櫘閫氳亰澶╂秷鎭€屼笉鏄皟鐢?`/admin/ai/chat/resume`锛屼笟鍔＄‘璁や粛鏃犳硶鎵ц锛涘綋鍓?admin web 宸蹭娇鐢?resume 鎺ュ彛銆?
