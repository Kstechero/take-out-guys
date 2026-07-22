---
title: 菜品评价与人工客服指南
maintainer: backend-team
updated_at: 2026-07-13
domain: service
visibility: user
status: approved
source_refs:
  - backend/sky-server/src/main/java/com/sky/service/impl/ReviewServiceImpl.java
  - backend/sky-server/src/main/java/com/sky/service/impl/CustomerServiceServiceImpl.java
  - backend/sky-server/src/main/java/com/sky/service/impl/UserAiChatServiceImpl.java
---

# 菜品评价与人工客服指南

## 评价条件

用户只能评价属于自己的已完成订单，并且只能评价该订单明细中实际存在的菜品。评分范围为 1 到 5 分，评价内容不能为空。同一用户对同一订单中的同一菜品不能重复提交评价。

评价提交前会检查敏感词；命中敏感内容时拒绝提交。AI 可以协助生成评价草稿，但草稿不代表已经发布，最终内容仍需由用户确认并通过现有评价接口提交。AI 帮写评价同样只适用于本人已完成订单中的可评价菜品。

用户可以查看自己的评价和菜品公开评价，可以点赞或取消点赞，也可以删除本人评价。评价是否已经提交、哪些菜品待评价属于实时数据，应查询评价状态接口，不能根据对话历史推断。

## 人工客服会话

每个用户可以创建或继续自己的开放客服会话。用户只能查看和发送自己会话中的消息，也可以主动结束会话；已经结束的会话不能继续发送消息。

客服消息内容不能为空，发送前会进行敏感词检查。命中敏感词时消息不会发送。管理员可以查看客服会话、回复开放会话并结束会话，但用户和管理员都不能通过知识检索读取其他用户的会话原文。

## 需要转人工的情况

配送范围无法确认、订单进入已接单后需要特殊取消、退款到账异常、赔付诉求或知识库没有明确规则时，应建议联系人工客服。Agent 不得承诺人工处理结果、赔付金额或完成时限。
