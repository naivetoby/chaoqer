<!DOCTYPE HTML>
<html>
<head>
    <meta charset="UTF-8">
    <link rel="shortcut icon" type="image/x-icon" href="https://chaoqer.com/favicon.ico">
    <title>PUSH DEMO</title>
</head>
<body>
<style>
    form.form-example {
        display: table;
    }

    div.form-example {
        display: table-row;
    }

    label, input {
        display: table-cell;
        margin-bottom: 10px;
    }

    label {
        padding-right: 10px;
    }

    textarea {
        overflow: auto;
        resize: none;
    }

    input {
        width: 500px;
    }

    input[type = 'submit'] {
        width: 100px;
    }

    a, input, textarea, select {
        outline: 0;
    }

    a {
        text-decoration: underline;
        -webkit-touch-callout: none;
    }

    a:hover {
        text-decoration: underline;
        outline: none;
    }

    a:focus {
        text-decoration: underline;
        outline: none;
    }

    a:active, a:hover {
        outline: 0
    }

    :focus {
        outline: 0;
    }
</style>
<h2>PUSH DEMO</h2>
<pre>
pushAlertMessage            发送通知消息
  - uid                     目标用户
  - originPushType          推送来源: 0 系统 / 1 官方 / 2 互动
  - originUid               消息来源UID(可选值, 推送来源为互动时为必填)
  - title                   标题
  - content                 内容
  - url                     url
  - store                   是否存储: 0 不存储 / 1 存储
  - alert                   是否通知: 0 不通知 / 1 通知

pushAttachMessage           发送透传消息
  - uid                     目标用户
  - originPushType          推送来源: 0 系统 / 1 官方 / 2 互动
  - originUid               消息来源UID(可选值, 推送来源为互动时为必填)
  - attachType              透传类型: 1 对话框(title/content/cancel/confirm/url) / 2 退出登录 / 3 新消息数提醒
  - attachBody              透传内容
      - 暂无
  - store                   是否存储: 0 不存储 / 1 存储
</pre>
<h2>pushTemplateMessage</h2>
<form action="/push/template" class="form-example" method="post">
    <div class="form-example">
        <label for="uid">uid: </label>
        <input name="uid" value="a5f00712b45b499eb15e99d77138d02d"/>
    </div>
    <div class="form-example">
        <label for="originPushType">originPushType: </label>
        <input name="originPushType" value="2" placeholder="0 系统 / 1 官方 / 2 互动"/>
    </div>
    <div class="form-example">
        <label for="originUid">originUid: </label>
        <input name="originUid" value="6ce4443c8fcd4a2b8b407bda86ab4408" placeholder="消息来源UID(可选值, 推送来源为互动时为必填)"/>
    </div>
    <div class="form-example">
        <label for="title">title: </label>
        <input name="title" value="测试标题"/>
    </div>
    <div class="form-example">
        <label for="content">content: </label>
        <input name="content" value="测试内容"/>
    </div>
    <div class="form-example">
        <label for="url">url: </label>
        <input name="url" value="chaoqer://profile/a5f00712b45b499eb15e99d77138d02d"/>
    </div>
    <div class="form-example">
        <label for="store">store: </label>
        <input name="store" value="1"/>
    </div>
    <div class="form-example">
        <label for="alert">alert: </label>
        <input name="alert" value="1"/>
    </div>
    <div class="form-example">
        <input type="submit" value="推送">
    </div>
</form>
<h2>pushAttachMessage</h2>
<form action="/push/attach" class="form-example" method="post">
    <div class="form-example">
        <label for="uid">uid: </label>
        <input name="uid" value="a5f00712b45b499eb15e99d77138d02d"/>
    </div>
    <div class="form-example">
        <label for="originPushType">originPushType: </label>
        <input name="originPushType" value="2" placeholder="0 系统 / 1 官方 / 2 互动"/>
    </div>
    <div class="form-example">
        <label for="originUid">originUid: </label>
        <input name="originUid" value="6ce4443c8fcd4a2b8b407bda86ab4408" placeholder="消息来源UID(可选值, 推送来源为互动时为必填)"/>
    </div>
    <div class="form-example">
        <label for="attachType">attachType: </label>
        <input name="attachType" value="1" placeholder="1 对话框 / 2 退出登录 / 3 新消息数提醒"/>
    </div>
    <div class="form-example">
        <label for="attachBody">attachBody: </label>
        <input name="attachBody" value="" placeholder="选填(JSON字符串格式)"/>
    </div>
    <div class="form-example">
        <label for="store">store: </label>
        <input name="store" value="1"/>
    </div>
    <div class="form-example">
        <input type="submit" value="推送">
    </div>
</form>
</body>
</html>
