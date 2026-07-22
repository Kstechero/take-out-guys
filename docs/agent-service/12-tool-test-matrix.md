# Tool 五维测试矩阵

`agent-service/evals/tool_test_matrix.json` 是发布门禁：每个实际注册 Tool 必须出现在矩阵中，并覆盖成功、空结果、权限不足、参数错误和超时五个维度。测试采用“工具专项 + 共享边界”组合：Pydantic schema 统一覆盖参数错误，Registry 与 Java Internal API 统一覆盖权限，Spring 客户端统一覆盖超时/重试/熔断，各工具与对应 Internal Controller 覆盖成功和空结果。写 Tool 在此基础上还必须通过未确认、过期、换人、编辑、幂等重放和 Java 最终拒绝用例。

`test_tool_quality_gate.py` 会从用户图和管理图读取真实注册工具，与矩阵做精确集合比较；新增工具若没有同步五维证据，测试会直接失败。
