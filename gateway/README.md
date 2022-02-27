# API 网关

## Auth

> * hearder中添加 'Authorization' : 'Basic ' + btoa(uid + ':' + token)
> * hearder中添加 'Client-Type' : 0: Web / 1: iOS / 2: Android
> * hearder中添加 'Version-Name' : 版本名称
> * hearder中添加 'Version-Code' : 版本号(整数值)

## 客户端公共参数

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| ts       |    是      |   无     |   当前毫秒数    |
| sign       |    是      |   无     |   所有其他参数key排序后`key=value` join `$$$` 在一起，加上'$$$accessSecret={accessSecret}'， 最后求MD5值，用户未登录是使用默认值accessSecret为`Hx!0k!K}5E.]EoP` )    |

## Http Error Code

|      HTTP状态码    |         Message                       |                   备注                   |
|:---------------------------|:--------------------------------------------|:--------------------------------------------|
|         400             |      Params Not Valid                |       请求参数不正确                              |
|         401             |      Authorized Failed               |       权限校验失败(未登录或者登录已过期)             |
|         403                |      Forbidden                          |       禁止访问         |
|         404                |      Not Found                          |       找不到     |
|         405                |      Method Not Allowed              |       请求方法不对     |
|         500                 |    Internal Server Error           |       服务器内部错误                            |
|         503                 |    Service Unavailable             |       服务不可用                               |
|         504                |      Gateway Timeout                  |       请求超时(服务器负载过高，未能及时处理请求)     |

## Result

|      状态码 / status   |      提示消息 /message        |         结果 (错误码) / result (errorCode)         |
|:---------------------------|:--------------------------------------------|:----------------------------|
|         1                 |  String / 默认 [ Success ]            |    JSON / JSONArray / 默认 [ 空JSON对象 ]  |
|         0                 |   String / 默认 [  Failure ]       |      int / 默认 [ 0  ]   | 

## Operate Error Code

|      业务状态码    |         Message                       |
|:---------------------------|:--------------------------------------------|
|         -1             |     短时间内重复调用(直接忽略请求)                            |
|         0             |     默认错误码(提示 message 信息)                            |
|         100000             |     账号不存在             |
|         100001             |     账号已存在             |
|         100002             |     账号已被冻结             |
|         200000             |     用户资料未初始化             |
|         200001             |     用户资料已初始化             |
|         200002             |     用户不存在             |
|         200003             |     消息不存在             |
|         400000             |     活动不存在             |
|         400001             |     无法加入此活动(有主持人拉黑了你)             |
|         500000             |     圈子不存在             |
|         600000             |     日程不存在             |

## API

### Account

##### 1. 获取短信验证码

**POST**   <u>/account/captcha</u>

**鉴权：** 无

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| countryCode       |    是      |   86     |   国家地区码    |
| mobile       |    是      |   无     |   手机号    |

**响应数据**

```json
{
  "result": {},
  "message": "验证码已发送，请注意查收",
  "status": 1
}
```

##### 2. 短信验证码登录/注册账号

**POST**   <u>/account/login/captcha</u>

**鉴权：** 无

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| countryCode       |    是      |   86     |   国家地区码    |
| mobile       |    是      |   无     |   手机号    |
| captcha       |    是      |   无     |   短信验证码 /^\d{4,6}$/   |

**响应数据**

```json
{
  "result": {
    "uid": "2701a6fbd0464da99789c9ad1ca1c654",
    "expireTime": 1608207590160,
    "accessToken": "c184241930be43b5bc36812ce3de56b5",
    "accessSecret": "e31bc25bcc6641a1a998eacb6ca9d244"
  },
  "message": "登录成功/注册成功",
  "status": 1
}
```

##### 3. 退出登录

**POST**   <u>/account/logout</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "退出成功",
  "status": 1
}
```

### User

##### 1. 初始化用户资料

**POST**   <u>/user/profile/init</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| nickname       |    是      |  无     |   昵称   |
| avatar       |    是      |  无     |  头像   |

**响应数据**

```json
{
  "result": {
    "uid": "861da0739b7542e69b81fe4baebfd816",
    "nickname": "Toby",
    "avatar": "https://v.chaoley.com/image/default/C1FF9777D4CE48C1889F89C6497A8EFD-6-2.png",
    "bio": "你好哇...",
    "blocked": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 更新用户资料

**PUT**   <u>/user/profile</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| nickname       |    是      |  无     |   昵称   |
| avatar       |    是      |  无     |  头像   |
| bio       |    否      |  无     |   介绍   |

**响应数据**

```json
{
  "result": {
    "uid": "861da0739b7542e69b81fe4baebfd816",
    "nickname": "Toby",
    "avatar": "https://v.chaoley.com/image/default/C1FF9777D4CE48C1889F89C6497A8EFD-6-2.png",
    "bio": "你好哇...",
    "blocked": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 3. 获取用户资料

**GET**   <u>/user/{uid:[a-f0-9]{24}}/profile</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "uid": "861da0739b7542e69b81fe4baebfd816",
    "nickname": "Toby",
    "avatar": "https://v.chaoley.com/image/default/C1FF9777D4CE48C1889F89C6497A8EFD-6-2.png",
    "bio": "你好哇...",
    "blocked": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 4. 分页获取黑名单列表

**GET**   <u>/user/block</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "createTime": 1616747477657,
        "sequenceId": "1616747477657:250228d2eb8e4db8b4fc6b8e3e2a902d",
        "uid": "250228d2eb8e4db8b4fc6b8e3e2a902d",
        "userProfileResult": {
          "avatar": "https://cdn.chaoqer.com/image/250228d2eb8e4db8b4fc6b8e3e2a902d/avatar/b8eb601c205e4518af93c3ef7b734ef0.jpg",
          "bio": "开心就好",
          "sendCard": 1,
          "cardTotal": 3,
          "blocked": 1,
          "nickname": "Tony",
          "uid": "250228d2eb8e4db8b4fc6b8e3e2a902d"
        }
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 5. 拉黑用户

**POST**   <u>/user/{uid:[a-f0-9]{24}}/block</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "拉黑成功",
  "status": 1
}
```

##### 6. 取消拉黑用户

**DELETE**   <u>/user/{uid:[a-f0-9]{24}}/block</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "取消拉黑成功",
  "status": 1
}
```

##### 7. 分页获取消息列表

**GET**   <u>/user/message</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "messageId": 1617987103601000,
        "sequenceId": "1617987103601000",
        "userMessageResult": {
          "createTime": 1617987103576,
          "messageBody": {
            "title": "有人邀请你参加活动",
            "content": "这是一条测试\\n嘎嘎嘎",
            "url": "chaoqer://room/73470423d7154aeb89cb61aec025a7f2"
          },
          "messageId": 1617987103601000,
          "messageType": 0,
          "originPushType": 2,
          "originUid": "a5f00712b45b499eb15e99d77138d02d",
          "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
          "unread": 1
        }
      },
      {
        "messageId": 1617986857810000,
        "sequenceId": "1617986857810000",
        "userMessageResult": {
          "createTime": 1617986857769,
          "messageBody": {
            "title": "开杰是不是傻逼？",
            "content": "是",
            "url": "chaoqer://profile/a5f00712b45b499eb15e99d77138d02d"
          },
          "messageId": 1617986857810000,
          "messageType": 0,
          "originPushType": 0,
          "originUid": "",
          "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
          "unread": 1
        }
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 8. 获取未读消息总数

**GET**   <u>/user/message/unread_total</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": 5,
  "message": "Success",
  "status": 1
}
```

##### 9. 发送名片

**POST**   <u>/user/{uid:[a-f0-9]{24}}/card</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "对方已收到你的名片",
  "status": 1
}
```

##### 10. 删除名片

**DELETE**   <u>/user/{uid:[a-f0-9]{24}}/card</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "删除成功",
  "status": 1
}
```

##### 11. 分页获取名片列表

**GET**   <u>/user/card</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "createTime": 1616747477657,
        "sequenceId": "1616747477657:250228d2eb8e4db8b4fc6b8e3e2a902d",
        "uid": "250228d2eb8e4db8b4fc6b8e3e2a902d",
        "userProfileResult": {
          "avatar": "https://cdn.chaoqer.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/30326497ad814f039a73d03035a46288.jpg",
          "bio": "- 超圈打杂@LitGeek\n- 👆👇👉👈🖕",
          "blocked": 0,
          "nickname": "铭恺",
          "sendCard": 1,
          "cardTotal": 3,
          "uid": "a5f00712b45b499eb15e99d77138d02d"
        }
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

### Club

##### 1. 创建圈子

**POST**   <u>/club</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| name       |   是      |   无     |  名称  |
| cover       |   是      |   无     |  封面  |

**响应数据**

```json
{
  "result": {
    "cover": "https://cdn.litgee.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/21b85549210b4f93bbda0304c6635b09.jpg",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
    "createTime": 1614421213212,
    "name": "产品交流圈",
    "clubId": "b4688e4583244aad8363d390073c86ff",
    "memberTotal": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 获取圈子

**GET**   <u>/club/{clubId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "clubId": "ea01330db4ad49288dce2b8bcb00240b",
    "cover": "https://cdn.litgee.com/image/250228d2eb8e4db8b4fc6b8e3e2a902d/cover/759d404cbe07494785a1c498dc328373.jpg",
    "createTime": 1614426859268,
    "memberTotal": 3,
    "joined": 1,
    "name": "Bug交流圈",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
  },
  "message": "Success",
  "status": 1
}
```

##### 3. 分页获取圈子列表

**GET**   <u>/club</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "clubId": "b4688e4583244aad8363d390073c86ff",
        "clubResult": {
          "clubId": "b4688e4583244aad8363d390073c86ff",
          "cover": "https://cdn.litgee.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/21b85549210b4f93bbda0304c6635b09.jpg",
          "createTime": 1614421213212,
          "memberTotal": 0,
          "joined": 1,
          "name": "产品交流圈",
          "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
        },
        "createTime": 1614421213477,
        "sequenceId": "1614421213477:b4688e4583244aad8363d390073c86ff"
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 4. 分页获取圈子成员列表

**GET**   <u>/club/{clubId}/member</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "createTime": 1614421213477,
        "sequenceId": "1614421213477:b4688e4583244aad8363d390073c86ff",
        "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
        "userProfileResult": {
          "avatar": "https://cdn.litgee.com/image/avatar/xxx.jpg",
          "bio": "你好哇。。。",
          "nickname": "Toby",
          "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
          "sendCard": 1,
          "cardTotal": 3,
          "blocked": 0
        }
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 5. 加入圈子

**POST**   <u>/club/{clubId}/join</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "cover": "https://cdn.litgee.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/21b85549210b4f93bbda0304c6635b09.jpg",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
    "createTime": 1614421213212,
    "name": "产品交流圈",
    "joined": 1,
    "clubId": "b4688e4583244aad8363d390073c86ff",
    "memberTotal": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 6. 退出圈子

**POST**   <u>/club/{clubId}/leave</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "Success",
  "status": 1
}
```

### Room

##### 1. 创建活动

**POST**   <u>/room</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| name       |   是      |   无     |  主题  |
| clubId       |   是      |   无     |  圈子ID  |
| inviteOnly       |   否      |   0     |  私密(当inviteOnly为1时，自动忽略clubId)  |

**响应数据**

```json
{
  "result": {
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
    "createTime": 1614433968620,
    "name": "产品活动",
    "clubId": "ea01330db4ad49288dce2b8bcb00240b",
    "inviteOnly": 0,
    "roomId": "e8c7d8325a7143e69648dbc799565f93"
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 获取活动

**GET**   <u>/room/{roomId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "clubId": "5e0c7a50a85e48fdb386f2ddbdd6e971",
    "clubResult": {
      "clubId": "5e0c7a50a85e48fdb386f2ddbdd6e971",
      "inviteOnly": 0,
      "cover": "https://cdn.chaoqer.com/image/6ce4443c8fcd4a2b8b407bda86ab4408/cover/331a4fe4981e4df9896b547336276c4c.jpg",
      "createTime": 1616514888950,
      "joined": 0,
      "memberTotal": 1,
      "name": "业余足球圈",
      "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
    },
    "createTime": 1616559267214,
    "memberList": [
      {
        "avatar": "https://cdn.chaoqer.com/image/6ce4443c8fcd4a2b8b407bda86ab4408/avatar/49d8ec7c29474523868a73eb0d395557.jpg",
        "bio": "-超圈程序员@LitGeek\n\n-有bug别找我！",
        "nickname": "Toby",
        "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
        "blocked": 0
      }
    ],
    "memberTotal": 1,
    "name": "测试",
    "roomId": "cda55a0bfb834f5ab18eb0867f01017a",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
  },
  "message": "Success",
  "status": 1
}
```

##### 3. 分页获取活动列表

**GET**   <u>/room</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "createTime": 1614434249522,
        "roomId": "6d54737e3dda4ae4abf07acacea3d7a1",
        "roomResult": {
          "clubId": "ea01330db4ad49288dce2b8bcb00240b",
          "clubResult": {
            "clubId": "ea01330db4ad49288dce2b8bcb00240b",
            "inviteOnly": 0,
            "cover": "https://cdn.litgee.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/21b85549210b4f93bbda0304c6635b09.jpg",
            "createTime": 1614426859268,
            "memberTotal": 1,
            "name": "产品交流圈2",
            "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
          },
          "createTime": 1614434249522,
          "name": "产品活动",
          "roomId": "6d54737e3dda4ae4abf07acacea3d7a1",
          "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
          "memberList": [
            {
              "avatar": "https://cdn.litgee.com/image/avatar/xxx.jpg",
              "bio": "你好哇。。。",
              "nickname": "Toby",
              "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
              "blocked": 0
            }
          ]
        },
        "sequenceId": "1614434249522:6d54737e3dda4ae4abf07acacea3d7a1"
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 4. 获取活动声网Token

**POST**   <u>/room/{roomId}/token</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "roomId": "6d54737e3dda4ae4abf07acacea3d7a1",
    "token": "00675842dc78ef64861aa0125c4b5f5a3c8IABvTXBv2cq/+RpsthBTL0niE8+1bB8Nbdcgn9OAfde77sl0yyYAAAAAIgATOdxlZaE7YAQAAQD1XTpgAgD1XTpgAwD1XTpgBAD1XTpg"
  },
  "message": "Success",
  "status": 1
}
```

##### 5. 主持人关闭活动

**POST**   <u>/room/{roomId}/close</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "Success",
  "status": 1
}
```

##### 6. 获取活动Cache

**POST**   <u>/room/{roomId}/cache</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": "992058c18549424ea76c1c0d958fcdc1",
  "message": "Success",
  "status": 1
}
```

##### 7. 邀请用户加入活动

**POST**   <u>/room/{roomId}/invite/{uid}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "邀请成功",
  "status": 1
}
```

### Event

##### 1. 创建日程

**POST**   <u>/event</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| name       |   是      |   无     |  标题，不能超过100个字  |
| desc       |   否      |   无     |  介绍，不能超过300个字  |
| eventTime       |   是      |   0     |  日程时间，毫秒数  |
| memberUidList       |   是      |   无     |  主持人或者嘉宾, 如: ["uid1","uid2","uid3"] |

**响应数据**

```json
{
  "result": {
    "createTime": 1620732258091,
    "desc": "测试介绍",
    "eventId": "6bdcf14e7fd742bcb9198d60e5452d8c",
    "eventTime": 1620748800000,
    "eventTimeStatus": 0,
    "memberUidList": [
      "6ce4443c8fcd4a2b8b407bda86ab4408",
      "800fd752a5284eeb82396fe7478175eb",
      "a5f00712b45b499eb15e99d77138d02d"
    ],
    "name": "测试日程",
    "notify": 0,
    "uid": "800fd752a5284eeb82396fe7478175eb"
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 编辑日程

**PUT**   <u>/event/{eventId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| name       |   是      |   无     |  标题，不能超过100个字  |
| desc       |   否      |   无     |  介绍，不能超过300个字  |
| eventTime       |   是      |   0     |  日程时间，毫秒数  |
| memberUidList       |   是      |   无     |  主持人或者嘉宾, 如: ["uid1","uid2","uid3"] |

**响应数据**

```json
{
  "result": {
    "createTime": 1620732258091,
    "desc": "测试介绍",
    "eventId": "6bdcf14e7fd742bcb9198d60e5452d8c",
    "eventTime": 1620748800000,
    "eventTimeStatus": 0,
    "memberUidList": [
      "6ce4443c8fcd4a2b8b407bda86ab4408",
      "800fd752a5284eeb82396fe7478175eb",
      "a5f00712b45b499eb15e99d77138d02d"
    ],
    "name": "测试日程",
    "notify": 0,
    "uid": "800fd752a5284eeb82396fe7478175eb"
  },
  "message": "Success",
  "status": 1
}
```

##### 3. 删除日程

**DELETE**   <u>/event/{eventId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "Success",
  "status": 1
}
```

##### 4. 获取日程

**GET**   <u>/event/{eventId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "createTime": 1620732258091,
    "desc": "测试介绍",
    "eventId": "6bdcf14e7fd742bcb9198d60e5452d8c",
    "eventTime": 1620748800000,
    "eventTimeStatus": 0,
    "memberUidList": [
      "6ce4443c8fcd4a2b8b407bda86ab4408",
      "800fd752a5284eeb82396fe7478175eb",
      "a5f00712b45b499eb15e99d77138d02d"
    ],
    "name": "测试日程",
    "notify": 0,
    "uid": "800fd752a5284eeb82396fe7478175eb"
  },
  "message": "Success",
  "status": 1
}
```

##### 5. 分页获取日程列表

**GET**   <u>/event</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| lastId       |    否      |   无     |   为空表示第一页，每次使用返回列表中最后一个对象的sequenceId |
| count       |   是      |   无     |  count只能在0到100之间    |

**响应数据**

```json
{
  "result": {
    "page": {
      "count": 10
    },
    "list": [
      {
        "eventId": "99f43e8566b4413288fb02e0f4a9acc3",
        "eventResult": {
          "createTime": 1620736603909,
          "desc": "测试介绍2",
          "eventId": "99f43e8566b4413288fb02e0f4a9acc3",
          "eventTime": 1620748800000,
          "eventTimeStatus": 0,
          "memberUidList": [
            "800fd752a5284eeb82396fe7478175eb",
            "a5f00712b45b499eb15e99d77138d02d"
          ],
          "name": "日程",
          "notify": 0,
          "uid": "800fd752a5284eeb82396fe7478175eb"
        },
        "eventTime": 1620736603909,
        "sequenceId": "1620736603909:99f43e8566b4413288fb02e0f4a9acc3"
      },
      {
        "eventId": "8cb9cbc876db40b89c5635dd8ff8d0d0",
        "eventResult": {
          "createTime": 1620731990093,
          "desc": "测试介绍",
          "eventId": "8cb9cbc876db40b89c5635dd8ff8d0d0",
          "eventTime": 1620748800000,
          "eventTimeStatus": 0,
          "memberUidList": [
            "6ce4443c8fcd4a2b8b407bda86ab4408",
            "800fd752a5284eeb82396fe7478175eb",
            "a5f00712b45b499eb15e99d77138d02d"
          ],
          "name": "测试日程",
          "notify": 0,
          "uid": "800fd752a5284eeb82396fe7478175eb"
        },
        "eventTime": 1620731990093,
        "sequenceId": "1620731990093:8cb9cbc876db40b89c5635dd8ff8d0d0"
      }
    ]
  },
  "message": "Success",
  "status": 1
}
```

##### 6. 预约日程

**POST**   <u>/event/{eventId}/notify</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "Success",
  "status": 1
}
```

##### 7. 通过日程开始活动

**POST**   <u>/event/{eventId}/start</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "createTime": 1620742489049,
    "eventId": "d74b3a79a39742aa80999455cc5a634a",
    "eventTime": 1620742462510,
    "eventTimeStatus": 1,
    "memberUidList": [
      "800fd752a5284eeb82396fe7478175eb"
    ],
    "name": "测试活动",
    "notify": 0,
    "roomId": "7b641bee240e4d84ad1a7f44b9811406",
    "roomResult": {
      "createTime": 1620742499846,
      "inviteOnly": 0,
      "memberList": [
        {
          "allowLive": 0,
          "avatar": "https://cdn.chaoqer.com/image/800fd752a5284eeb82396fe7478175eb/avatar/82aaffd72d5d49e88363949429a1f4e6.jpg",
          "bio": "https://cdn.chaoqer.com/flutter-ci/122755a6.apk",
          "blocked": 0,
          "cardTotal": 0,
          "nickname": "枫叶",
          "sendCard": 0,
          "uid": "800fd752a5284eeb82396fe7478175eb"
        }
      ],
      "memberTotal": 0,
      "name": "测试活动",
      "roomId": "7b641bee240e4d84ad1a7f44b9811406",
      "uid": "800fd752a5284eeb82396fe7478175eb"
    },
    "uid": "800fd752a5284eeb82396fe7478175eb"
  },
  "message": "Success",
  "status": 1
}
```

### Aliyun

##### 1. 创建上传图片凭证

**POST**   <u>/aliyun/image/upload</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| imageDir       |   是      |   无     |  图片目录: 头像 avatar / 封面 cover / 活动 room / 反馈 feedback  |

**响应数据**

```json
{
  "result": {
    "uploadAddress": "https://chaoley.oss-cn-shenzhen.aliyuncs.com/image/861da0739b7542e69b81fe4baebfd816/cover/56da4708cf5348708c775f0cb09d7520.jpg?Expires=1608622430&OSSAccessKeyId=STS.NUWSLyUZTESoazaY4hX5ZheQW&Signature=UFqaQliQ2K79lVP9RTfxWCFo26s%3D&security-token=CAIS7wF1q6Ft5B2yfSjIr5biGPbNuIV18pGEY1zQvTQ9VLp2h6D6tTz2IHtKf3JvBeoXsPQxmW9R7fwTlqp%2FRoEdyvduzks2vPpt6gqET9frma7ctM4p6vCMHWyUFGSIvqv7aPn4S9XwY%2Bqkb0u%2B%2BAZ43br9c0fJPTXnS%2Brr76RqddMKRAK1QCNbDdNNXGtYpdQdKGHaOITGUHeooBKJVxE461Il2TkvsvTvn5DH0HeE0g2mkN1yjp%2FqP52pY%2FNrOJpCSNqv1IR0DPGajnULtEQQpfkm0vEboGyd78v4BEJK%2Fg2CNOPY6NprIR%2FYgzus5nGcxhqAAR1D8CO8ja%2FJEEwdU1lYDSG3piki2yR7W8JOiEqjHRo%2B0riKNYrqDoLXzi8forQYAqdDUKY7vINwEnoVi%2BPfTA%2B5ZT9x%2FGAYBK5AyV2Dmn1quWRSt%2BSvrKhNLRbXsGc9IsBrMLMzE%2FcIFu43jdS80t04od01r2aox2p3lPp%2BMi%2Bf",
    "imageId": "56da4708cf5348708c775f0cb09d7520",
    "imageURL": "https://cdn.chaoley.com/image/861da0739b7542e69b81fe4baebfd816/cover/56da4708cf5348708c775f0cb09d7520.jpg"
  },
  "message": "Success",
  "status": 1
}
```

### Push

##### 1. 更新JPushID

**POST**   <u>/push/{jPushId}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {},
  "message": "Success",
  "status": 1
}
```

### App

##### 1. 获取最新版本

**GET**   <u>/app/version/upgrade</u>

**鉴权：** 否

**参数校验：** 否

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "clientType": 1,
    "createTime": 1618818435105,
    "downloadUrl": "https://apps.apple.com/cn/app/id1556448380",
    "forcedUpgrade": 0,
    "versionCode": 19,
    "versionDesc": "1. 增加了名片夹功能 - 现在可以向其他人发送你的名片了，当对方收到你的名片，他/她就可以在App内直接邀请你加入某个活动。 2. 增加了通知功能 - 当有人给你发送名片或在App内邀请你加入活动时，你可以收到对应的通知了。 3. 增加了私密活动 - 现在可以创建私密的活动了，只有被邀请的人才能加入一个私密的活动。 4. 修复了一些已知问题",
    "versionId": "14205a41a5b5dd563fbc816b9a4c4df8",
    "versionName": "0.1.7"
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 反馈

**POST**   <u>/app/feedback</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| originFeedbackType       |   是      |   无     |  反馈来源类型: 0 App / 1 Room  |
| originId                 |   是      |   无     |  反馈来源ID  |
| feedbackType             |   是      |   无     |  反馈类型: 0 default (其他可能包含: 功能Bug, UI异常, 内容太烂, 需求等等)  |
| feedbackContent          |   是      |   无     |  反馈内容(不能超过500个字)  |
| feedbackImageList        |   否      |   无     |  反馈图片, 如: ["img1","img2"]  |

**响应数据**

```json
{
  "result": {},
  "message": "反馈成功",
  "status": 1
}
```

##### 3. 投诉

**POST**   <u>/app/report</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| originReportType         |   是      |   无     |  反馈来源类型: 1 用户 / 2 活动  |
| originId                 |   是      |   无     |  投诉来源ID  |
| reportReasonType         |   是      |   无     |  投诉原因类别: 1 涉嫌违法违规 / 2 色情淫秽 / 3 暴力血腥 / 4 广告营销 / 5 其他  |
| reason                   |   是      |   无     |  投诉原因  |

**响应数据**

```json
{
  "result": {},
  "message": "投诉成功",
  "status": 1
}
```

##### 4. 获取链接预览

**POST**   <u>/app/link/preview/{linkMD5:32位MD5值(纯小写)，如果link不是http://或者https://开头，请补上http://}</u>

**鉴权：** 用户登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "image": "https://blog.toby.vip/upload/2019/9/WechatIMG15-c314e4df57b54f8fb995444617ddca1a.jpeg",
    "description": "创业吧，趁年轻！告别了上一段的创业之旅，Toby 正在修身养性，准备开启下一段未知的旅程。从这篇文章开始，Toby 将带领大家一起直面创业的困难与挑战，写下一些浅薄的经验和思考，与大家共勉，共同学习成长。目前大概会从这几个方面展开：创业路上之基础实践篇创业路上之个人成长篇创业路上之职业技能篇 - 技",
    "title": "创业路上之职业技能篇 - 技术 - 服务端及运维 - Tboy的技术角落",
    "url": "https://blog.toby.vip/archives/%E5%88%9B%E4%B8%9A%E8%B7%AF%E4%B8%8A%E4%B9%8B%E8%81%8C%E4%B8%9A%E6%8A%80%E8%83%BD%E7%AF%87-%E6%8A%80%E6%9C%AF-%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%8F%8A%E8%BF%90%E7%BB%B4"
  },
  "message": "Success",
  "status": 1
}
```

### H5

##### 1. 获取微信Config

**POST**   <u>/h5/weixin/config</u>

**鉴权：** 微信登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |
| url       |   是      |   无     |  当前路径URL  |

**响应数据**

```json
{
  "result": {
    "signature": "7746a74050c1639706bc84ec59c139b26a73a0a2",
    "appId": "wxdf43cbdae5004909",
    "jsapi_ticket": "LIKLckvwlJT9cWIhEQTwfGnwmIrYHQ03Uc50v2sITHQ0yEHz5cqR-Q89_0UySQZSfFHhrzwhsblt_nYlgVAP8g",
    "nonceStr": "25499f0d0e0642108148becb7e408451",
    "url": "https://dev-h5.chaoqer.com/room/26a9bc4b4a524e68b75c46eeb9bf58fa/6ce4443c8fcd4a2b8b407bda86ab4408",
    "timestamp": "1616667594"
  },
  "message": "Success",
  "status": 1
}
```

##### 2. 获取活动

**POST**   <u>/h5/room/{roomCacheId}</u>

**鉴权：** 微信登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "closed": 1,
    "clubId": "2e3276e6c28c4366ae12d6a16147bd27",
    "clubResult": {
      "clubId": "2e3276e6c28c4366ae12d6a16147bd27",
      "inviteOnly": 0,
      "cover": "https://cdn.chaoqer.com/image/a5f00712b45b499eb15e99d77138d02d/cover/f2a80cf2a04f4d1987e145e538ee389f.jpg",
      "createTime": 1614928767587,
      "joined": 0,
      "memberTotal": 7,
      "name": "超圈爸爸群",
      "uid": "a5f00712b45b499eb15e99d77138d02d"
    },
    "createTime": 1616597173743,
    "fromMember": {
      "avatar": "https://cdn.chaoqer.com/image/6ce4443c8fcd4a2b8b407bda86ab4408/avatar/49d8ec7c29474523868a73eb0d395557.jpg",
      "bio": "-超圈程序员@LitGeek\n\n-有bug别找我！",
      "nickname": "Toby",
      "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
    },
    "memberList": [
      {
        "avatar": "https://cdn.chaoqer.com/image/6ce4443c8fcd4a2b8b407bda86ab4408/avatar/49d8ec7c29474523868a73eb0d395557.jpg",
        "bio": "-超圈程序员@LitGeek\n\n-有bug别找我！",
        "nickname": "Toby",
        "uid": "6ce4443c8fcd4a2b8b407bda86ab4408",
        "blocked": 0
      }
    ],
    "memberTotal": 1,
    "roomCacheId": "f6fa3eef2e7344abad2035e13b19b79d",
    "roomId": "c45e03f148de498f92d63cfc31690abb",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
  },
  "message": "Success",
  "status": 1
}
```

##### 3. 获取圈子

**POST**   <u>/h5/club/{clubId}</u>

**鉴权：** 微信登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "clubId": "5e0c7a50a85e48fdb386f2ddbdd6e971",
    "cover": "https://cdn.chaoqer.com/image/6ce4443c8fcd4a2b8b407bda86ab4408/cover/331a4fe4981e4df9896b547336276c4c.jpg",
    "createTime": 1616514888950,
    "joined": 0,
    "memberTotal": 3,
    "name": "业余足球圈",
    "uid": "6ce4443c8fcd4a2b8b407bda86ab4408"
  },
  "message": "Success",
  "status": 1
}
```

##### 4. 获取用户资料

**POST**   <u>/h5/profile/{uid}</u>

**鉴权：** 微信登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "avatar": "https://cdn.chaoqer.com/image/250228d2eb8e4db8b4fc6b8e3e2a902d/avatar/b8eb601c205e4518af93c3ef7b734ef0.jpg",
    "bio": "开心就好",
    "nickname": "Tony",
    "uid": "250228d2eb8e4db8b4fc6b8e3e2a902d",
    "blocked": 0
  },
  "message": "Success",
  "status": 1
}
```

##### 5. 获取日程

**POST**   <u>/h5/event/{eventId}</u>

**鉴权：** 微信登录

**参数校验：** 是

**请求参数**

| 参数名称 | 是否必填 | 默认值 | 描述 |
| -------- | -------- | ------ | ---- |

**响应数据**

```json
{
  "result": {
    "createTime": 1620736603909,
    "desc": "测试介绍2",
    "eventId": "99f43e8566b4413288fb02e0f4a9acc3",
    "eventTime": 1620748800000,
    "eventTimeStatus": 0,
    "memberList": [
      {
        "allowLive": 0,
        "avatar": "https://cdn.chaoqer.com/image/800fd752a5284eeb82396fe7478175eb/avatar/82aaffd72d5d49e88363949429a1f4e6.jpg",
        "bio": "https://cdn.chaoqer.com/flutter-ci/122755a6.apk",
        "blocked": 0,
        "cardTotal": 0,
        "nickname": "枫叶",
        "sendCard": 0,
        "uid": "800fd752a5284eeb82396fe7478175eb"
      },
      {
        "allowLive": 0,
        "avatar": "https://cdn.chaoqer.com/image/a5f00712b45b499eb15e99d77138d02d/avatar/c083b4ff35eb41359c4a8a194204f955.jpg",
        "bio": "- 超圈打杂@LitGeek\n- 👆👇👉👈🖕",
        "blocked": 0,
        "cardTotal": 0,
        "nickname": "铭恺",
        "sendCard": 0,
        "uid": "a5f00712b45b499eb15e99d77138d02d"
      }
    ],
    "memberUidList": [
      "800fd752a5284eeb82396fe7478175eb",
      "a5f00712b45b499eb15e99d77138d02d"
    ],
    "name": "日程",
    "notify": 0,
    "uid": "800fd752a5284eeb82396fe7478175eb"
  },
  "message": "Success",
  "status": 1
}
```
