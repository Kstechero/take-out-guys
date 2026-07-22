# 2026-07-22 补充：分类查询与新增分类工具口径

- `admin_category_search` 改为管理端分类分页查询口径，后端复用 `CategoryService.pageQuery(CategoryPageQueryDTO)`；支持 `name`、`type`、`page`、`limit`，`query` 仅作为旧调用兼容别名并在进入 Spring Internal API 前转换为 `name`。
- 分类查询返回的 `total` 来自管理端分页服务，`items` 只代表当前页；不要再用 `CategoryService.list(type)` 的启用分类列表替代管理端分页结果。
- `create_admin_category` 已注册为受确认保护的管理写工具，参数为 `name`、`type`（1=菜品分类，2=套餐分类）、`sort`、`audit_reason`。
- `create_admin_category` 执行端点为 `POST /internal/agent/admin/categories`，Java 侧复用管理端原有 `CategoryService.save(CategoryDTO)`；按现有管理端 service 行为，新分类初始状态为停用。
- 分类新增属于单资源高风险写操作，必须走确认卡、灰度开关、幂等键和审计理由；删除分类、批量改分类和分类启停仍未注册给 Agent。
# Agent 宸ュ叿鐩綍

## 宸ュ叿璁捐瑙勫垯

- 涓€涓伐鍏峰彧瀹屾垚涓€涓ǔ瀹氫笟鍔″姩浣滐紝宸ュ叿鍚嶄娇鐢ㄨ嫳鏂?`snake_case`銆?- 鎵€鏈夊弬鏁扮敱 Pydantic 涓ユ牸鏍￠獙锛涚姝㈡帴鏀惰嚜鐢卞舰鎬?SQL銆乁RL銆佷换鎰?JSON 鎴栫敤鎴疯韩浠藉瓧娈点€?- 宸ュ叿鎻忚堪闈㈠悜妯″瀷锛屼絾鏉冮檺銆佸綊灞炲拰鍙傛暟鏍￠獙蹇呴』鐢?Java 涓?Python 浠ｇ爜鎵ц銆?- `write` 宸ュ叿榛樿涓嶅彲鍦ㄧ涓€杞墽琛岋紝蹇呴』杩涘叆纭鐘舵€侊紱绠＄悊鍐欏伐鍏蜂粎娉ㄥ唽宸插畬鎴愬悎鍚屻€佺伆搴︺€佸璁″拰骞傜瓑璇勫鐨勮兘鍔涖€?- 宸ュ叿缁撴灉杩斿洖闈㈠悜鍥炵瓟鐨?DTO锛屼笉杩斿洖瀵嗙爜銆佷护鐗屻€佹敮浠樹俊鎭€佸畬鏁存墜鏈哄彿鎴栨暟鎹簱瀹炰綋銆?
## 鐢ㄦ埛绔?P1 鍙宸ュ叿

| Tool | 绫诲瀷 | 鏉ユ簮鏄犲皠 | 杈撳叆 | 杈撳嚭 | 鏉冮檺涓庨檺鍒?|
| --- | --- | --- | --- | --- | --- |
| `shop_status` | read | Spring Internal API | 鏃?| 鐘舵€併€佹洿鏂版椂闂?| user/admin |
| `menu_search` | read | Spring Internal API | query銆乨ietary_preferences銆乥udget_max銆乴imit | 鑿滃搧/濂楅鍊欓€?| user/admin锛宭imit 鏈€澶?10 |
| `recent_orders` | read | Spring Internal API | status銆乴imit | 褰撳墠鐢ㄦ埛璁㈠崟鎽樿 | user锛屼粎鏌ヨ鑷繁 |
| `get_order` | read | Spring Internal API | order_id | 璁㈠崟璇︽儏銆佺姸鎬?| Java 鏍￠獙褰掑睘 |
| `get_cart` | read | legacy `get_cart` | 鏃?| 璐墿杞︽憳瑕?| user锛屼粎褰撳墠鐢ㄦ埛 |
| `list_addresses` | read | legacy `list_addresses` | default_only | 鑴辨晱鍦板潃鎽樿 | user锛屼粎褰撳墠鐢ㄦ埛 |
| `list_available_coupons` | read | legacy `list_coupons` | order_amount | 鍙敤浼樻儬鍒告憳瑕?| user锛屼粎褰撳墠鐢ㄦ埛 |
| `search_service_knowledge` | read | Python RAG index | query銆乨omain | 鏂囨。鐗囨鍜屾潵婧?| user/public visibility锛屼綆缃俊搴︽嫆绛?|
| `check_sensitive_words` | read | 鐜版湁鏁忔劅璇嶈兘鍔?| text | safe銆乵asked_text | 涓嶈繑鍥炴晱鎰熻瘝璇嶅簱 |

## 鐢ㄦ埛绔?P2 鍐欏伐鍏?
| Tool | 杈撳叆 | 纭鎽樿绀轰緥 | 骞傜瓑閿?|
| --- | --- | --- | --- |
| `add_to_cart` / `update_cart_item` / `remove_from_cart` / `clear_cart` | action銆乨ish_id/setmeal_id銆乹uantity锛涘姞璐繀椤诲甫鍒氭煡璇㈠埌鐨?`expected_unit_amount` | 鈥滅‘璁ゅ皢瀹繚楦′竵鏁伴噺鏀逛负 2 鍚楋紵鈥?| `actor + session + confirmation_token` |
| `claim_coupon` | coupon_id | 鈥滅‘璁ら鍙栨弧 30 鍑?5 浼樻儬鍒稿悧锛熲€?| `actor + coupon_id + confirmation_token` |

AI 璇勪环鍔熻兘鍙敓鎴愯崏绋匡細璁㈠崟瀹屾垚鐘舵€佸拰鏁忔劅璇嶆鏌ユ槸鍓嶇疆鏉′欢锛屾渶缁堟彁浜や粛鐢辩幇鏈夌敤鎴风璇勪环鎺ュ彛瀹屾垚銆?
## 绠＄悊绔伐鍏峰垎绾?
| 鍒嗙骇 | 鍏佽宸ュ叿 | 璇存槑 |
| --- | --- | --- |
| A锛氬彧璇?P3 | `query_business_overview`銆乣admin_order_search`銆乣admin_order_detail`銆乣get_shop_status`銆乣admin_menu_search`銆乣admin_set_meal_search`銆乣admin_coupon_search`銆乣admin_review_search`銆乣search_operational_knowledge` | 宸插紑鏀撅紝缁撴灉鍖呭惈鏉ユ簮銆佽寖鍥村拰鐢熸垚鏃堕棿 |
| B锛氶渶纭 P4 | `set_shop_status`銆乣update_order`銆乣manage_coupon`銆乣create_admin_dish`銆乣update_admin_dish`銆乣create_admin_coupon` | 宸插紑鏀撅紱鏃у€?鏂板€奸瑙堛€佷簩娆＄‘璁ゃ€佺伆搴﹀紑鍏炽€佷箰瑙傛牎楠屻€佸箓绛夊拰瀹¤鐞嗙敱 |
| C锛氭殏缂?| `manage_employee`銆乣change_my_password`銆乣manage_category`銆佽彍鍝佸垹闄?鎵归噺淇敼銆乣manage_setmeal`銆乣manage_sensitive_word` | 娑夊強楂橀闄╂潈闄愭垨鎵归噺鏁版嵁鍙樻洿锛孧VP 涓嶆敞鍐?|

## 宸ュ叿閿欒鏄犲皠

| Java 閿欒 | Tool 杩斿洖 | 瀵圭敤鎴风殑鍥炵瓟鍘熷垯 |
| --- | --- | --- |
| `FORBIDDEN` | `{ "ok": false, "code": "FORBIDDEN" }` | 涓嶆硠闇茶祫婧愬瓨鍦ㄦ€э紝璇存槑鏃犳潈璁块棶 |
| `NOT_FOUND` | `{ "ok": false, "code": "NOT_FOUND" }` | 璇存槑鏈壘鍒板苟寤鸿妫€鏌ヤ俊鎭?|
| `VALIDATION_ERROR` | `{ "ok": false, "code": "VALIDATION_ERROR", "fields": [...] }` | 璇锋眰鐢ㄦ埛琛ュ厖鎴栦慨姝ｅ弬鏁?|
| `UPSTREAM_ERROR` / timeout | `{ "ok": false, "code": "TEMPORARY_UNAVAILABLE" }` | 璇存槑鏆備笉鍙敤锛屼笉铏氭瀯缁撴灉 |

## 2026-07-22 琛ュ厖锛氱鐞嗙鐩綍鏌ヨ宸ュ叿鍙ｅ緞

- `admin_menu_search` 鏄鐞嗙鑿滃搧鍒嗛〉鏌ヨ鐨?Agent 灏佽锛屽悗绔繀椤诲鐢?`DishService.pageQuery(DishPageQueryDTO)`锛屼笉寰楀湪 Agent internal controller 涓墜宸ユ媺鍙栧叏閲忓垪琛ㄥ啀鎸夊悕绉版垨鍒嗙被浜屾杩囨护銆?- `admin_set_meal_search` 鏄鐞嗙濂楅鍒嗛〉鏌ヨ鐨?Agent 灏佽锛屽悗绔繀椤诲鐢?`SetmealService.pageQuery(SetmealPageQueryDTO)`銆?- 涓や釜鐩綍鏌ヨ宸ュ叿鍧囨敮鎸?`name`銆乣category_id`銆乣status`銆乣page`銆乣limit`锛沗query` 浠呬綔涓烘棫璋冪敤鍏煎鍒悕锛岃繘鍏?Spring Internal API 鍓嶈浆鎹负 `name`銆?- `limit` 鏈€澶?20锛汸ython schema 浼氬皢杩囧ぇ鐨勬ā鍨嬭緭鍏ュす鍒?20锛岄伩鍏嶅伐鍏疯皟鐢ㄥ洜鍙傛暟瓒婄晫澶辫触銆?- 杩斿洖鍊间腑 `total` 琛ㄧず绗﹀悎绠＄悊绔垎椤垫潯浠剁殑鐪熷疄鎬绘暟锛宍items` 鍙〃绀哄綋鍓嶉〉璁板綍銆侫gent 鍥炵瓟鈥滄煇鍒嗙被涓嬫湁澶氬皯鑿滃搧/濂楅鈥濇椂蹇呴』浣跨敤 `total`锛屼笉寰楃敤褰撳墠椤垫潯鏁版帹鏂€?- 鎸夊垎绫绘煡璇㈠墠蹇呴』鍏堣皟鐢?`admin_category_search` 鑾峰彇鐪熷疄鍒嗙被 ID锛涗笉寰楁妸鍒嗙被鍚嶇О浼犵粰 `admin_menu_search.name` 鍚庡０绉版槸鍦ㄦ寜鍒嗙被鏌ヨ銆?
