# 超圈 App 微服务源码

## 整体架构

> RPC 基于 https://github.com/thinktkj/simple-rpc

> DB 基于 阿里云表格存储 Tablestore https://help.aliyun.com/document_detail/27280.html

> Push 基于 极光推送 https://docs.jiguang.cn//jpush/guideline/intro/

> IM 基于 Vert.x https://vertx.io/

> 音视频解决方案 基于 声网 https://www.agora.io/cn

> DevOps(CI/CD, Maven 库, Docker 镜像库等) 基于 GitLab https://about.gitlab.com/

> Java 构建 基于 Google Jib https://github.com/GoogleContainerTools/jib

> Log 基于 阿里云SLS https://www.aliyun.com/product/sls

## OTS 数据库设计 
~~~txt
account
分区键
- uid UUID 用户ID
预定义列
- country_code_mobile_md5 -> MD5(country_code:mobile)
- country_code 国家地区码
- mobile 手机号
- username 用户账号，前期用uid，后期用户登录
- password 密码
- salt 密码salt
- status 账号状态: -99 注销 / -2 冻结(含永久封号) / -1 限制(所有提交功能) / 0 注册中 / 1 正常
- create_time
- update_time
- active_time 最新活跃时间
二级索引: account_country_code_mobile_index
- country_code_mobile_md5
- uid
预定义列
- password
- salt
- status
多元索引: account_search_index
- uid
- country_code
- mobile
- username
- status
- update_time
- create_time

// 以下可选字段
- deleted_apply_time 注销生效时间 -99
- block_expire_time 冻结过期时间 -2
- wechat_openid 微信OpenID
- wechat_unionid 微信UnionID
- qq_openid QQOpenID
- apple_openid 苹果OpenID

user_profile
分区键
- uid
预定义列
- nickname 昵称
- avatar 头像
- bio 简介
- create_time
- update_time

account_auth
分区键
- uid 用户ID
预定义列
- access_token 账号登录Token
- access_secret 账号登录Secret
- expire_time 账号授权登录过期时间
- login_time 上次登录时间
- create_time
- update_time

user_sms_daily
生命周期
- 86400s
分区键
- country_code_mobile_md5 -> MD5(country_code:mobile)
主键
- create_time

// 圈子
club_meta
分区键
- club_id 圈子ID
预定义列
- uid
- name
- cover
- create_time
多元索引: club_meta_search_index
- club_id
- uid
- name
- cover
- create_time

// 圈子成员
club_member
分区键
- club_id 圈子ID
主键
- uid 用户ID
预定义列
- admin
- create_time
- sequence_id -> create_time:club_id
二级索引: club_member_club_id_index
- club_id
- sequence_id
- uid
预定义列
- create_time
- admin
二级索引: club_member_uid_index
- uid
- sequence_id
- club_id
预定义列
- create_time

// 活动
room_meta
分区键
- room_id 活动ID
预定义列
- uid
- name
- club_id
- closed 是否已关闭 0 正常 / 1 已关闭
- create_time
- close_time
- invite_only 私密
二级索引: room_meta_club_id_index
- club_id
- closed
- room_id
多元索引: room_meta_search_index
- room_id
- uid
- name
- club_id
- invite_only
- closed
- create_time
- close_time

// 用户的所有可见活动
user_room
分区键
- room_id
主键
- uid
预定义列
- create_time
- sequence_id -> create_time:room_id
二级索引: user_room_index
- uid
- sequence_id
- room_id
预定义列
- create_time

// 用户的所有可见的公开活动
user_public_room
分区键
- room_id
预定义列
- create_time
- sequence_id -> create_time:room_id
二级索引: user_public_room_index
- sequence_id
- room_id
预定义列
- create_time

// 日程
event_meta
分区键
- event_id 日程ID
预定义列
- uid
- name
- desc
- member_uid_list
- event_time
- room_id
- create_time
- deleted

// 用户所有可见的日程(暂无)
user_event
分区键
- event_id
主键
- uid
预定义列
- event_time
- sequence_id -> event_time:event_id
二级索引: user_event_index
- uid
- sequence_id
- event_id
预定义列
- event_time

// 用户的所有可见的公开日程
user_public_event
分区键
- event_id
预定义列
- event_time
- sequence_id -> event_time:event_id
二级索引: user_public_event_index
- sequence_id
- event_id
预定义列
- event_time

// 日程预约提醒
event_notify
分区键
- event_id 日志ID
主键
- uid 用户ID
预定义列
- create_time

// 用户黑名单(单向block | 显示block列表 | 无法加入房间[uid为speaker] )
user_block
分区键
- uid
主键
- block_uid
预定义列
- create_time
- sequence_id -> create_time:block_uid
二级索引: user_block_index
- uid
- sequence_id
- block_uid
预定义列
- create_time

// 用户推送
user_message
分区键
- uid
主键
- message_id 自增列
预定义列
- origin_push_type 推送来源: 0 系统 / 1 官方 / 2 互动
- origin_uid 消息来源UID(可选值, 推送来源为2时, 才存在)
- message_type 推送类型: -1 透传消息 / 0 通知消息
- message_body 消息内容(JSON类型字符串)
- unread 是否未读
- deleted 删除状态: 0 正常 / 1 用户删除 / 2 平台删除
- create_time
二级索引: user_message_index
- uid
- deleted
- message_id
二级索引: user_message_unread_index
- uid
- deleted
- unread
- message_id

// 用户名片夹
user_card
分区键
- uid
主键
- origin_uid
预定义列
- deleted 删除状态: 0 正常 / 1 用户删除 / 2 平台删除
- create_time
- sequence_id -> create_time:origin_uid
二级索引: user_card_index
- uid
- deleted
- sequence_id
- origin_uid
预定义列
- create_time

// 用户推送设置
user_push_config
分区键
- uid
主键
- push_config_type 推送设置枚举
预定义列
- enable 是否开启
- create_time
- update_time

// 版本管理
app_version
分区键
- version_id MD5(client_type:version_code) 版本ID
预定义列
- client_type 客户端类型
- version_code 版本号
- version_name 版本名
- version_desc 版本描述
- forced_upgrade 是否强制更新
- download_url 下载地址
- create_time 创建时间
二级索引: app_version_index
- client_type
- version_code
- version_id

// 建议与反馈
app_feedback
分区键
- uid
主键
- origin_feedback_type 反馈来源类型: 0 App / 1 Room
- origin_id
- create_time
预定义列
- client_type 客户端类型: 0 Web / 1 iOS / 2 Android
- version_name 客户端版本名
- version_code 客户端版本号
- feedback_type 反馈类型: 0 default (其他可能包含: 功能Bug, UI异常, 内容太烂, 需求等等)
- feedback_content 反馈内容
- feedback_image_list 反馈图片
- status
- close_time

// 投诉
app_report
分区键
- uid
主键
- origin_report_type 投诉来源: 1 用户 / 2 活动
- origin_id
预定义列
- origin_uid
- origin_content
- origin_content_create_time
- report_reason_type 投诉原因类别: 1 涉嫌违法违规 / 2 色情淫秽 / 3 暴力血腥 / 4 广告营销 / 5 其他
- reason 投诉内容
- status 处理状态
- create_time

// 用户客户端信息
app_user_client_meta
分区键
- client_meta_id: MD5(uid:version_id)
主键
- uid -> uid
- version_id -> client_type:version_code
预定义列
- client_type 客户端类型: 0 Web / 1 iOS / 2 Android
- version_name 客户端版本名
- version_code 客户端版本号
- create_time

// 用户活跃日志
app_user_active_log
分区键
- client_meta_id
主键
- uid
- create_time
预定义列
- api_path API路径
- parameter_map 参数详情
- ip IP地址

// 统计数据
app_stats
分区键
- stats_id -> MD5(type:date)
主键
- type 类型: 0: 次日 / 1: 三日 / 2: 七日 / 3: 三十日
- date 日期: yyyy-MM-dd
预定义列
- add_total 新增数
- active_total 活跃数
- keep_percent 留存率
- one_session_total 一次会话
- create_open_room_total 创建公开活动数
- create_private_room_total 创建私密活动数
- create_open_event_total 创建公开活动日程数
- join_room_member_total 参与活动人数
- active_percent 活跃率

// 管理员账户
admin_account
分区键
- username 账号
预定义列
- nickname 昵称
- avatar 头像
- password 密码
- salt 密码salt
- status 账号状态: -1 冻结 / 0 未激活 / 1 正常
- create_time
- update_time
多元索引: admin_account_search_index
- username
- nickname
- avatar
- status
- update_time
- create_time
~~~

## 许可证

[![license](https://img.shields.io/github/license/thinktkj/smrpc.svg?style=flat-square)](https://github.com/thinktkj/smrpc/blob/master/LICENSE)

使用 Apache License - Version 2.0 协议开源。
