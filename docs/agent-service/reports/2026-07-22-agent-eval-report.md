# Agent Eval Report

- 日期：2026-07-22
- 分支：`codex/agent-complete-gaps`
- 权威后端：`backend/`
- 模型：`qwen36`
- 数据集：30 条 RAG + 13 条真实模型工具路由

## Summary

- status：pass
- Python：56/56 passed
- Java：25/25 passed
- 工具路由准确率：13/13，100%
- RAG Recall@1 / Recall@3 / Recall@5：95% / 100% / 100%
- RAG MRR：96.67%
- 可见性准确率：100%
- 无依据拒答率：100%
- Spring Internal API：12/12 成功
- Tool P95（排除模型）：28.75ms
- 端到端 P95（包含模型）：约 9.5s
- 越权、未确认写入、换用户确认和幂等重放：自动化安全用例全部通过

## Release decision

核心 MVP 与 P5 运行能力达到当前文档门槛。端到端耗时主要来自模型推理；纯 Tool P95 明显低于 3 秒门槛。生产发布仍需按 `11-production-runbook.md` 配置真实密钥、PostgreSQL 密码、Prometheus 告警接收端和镜像 tag，并执行一次部署级回滚演练。
