package com.chaoqer.common.entity.base;

import com.chaoqer.common.util.DigestUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class AuthedDTO {

    // 已认证用户ID
    private String authedUid;
    // 客户端信息
    private ClientInfo clientInfo;

    public boolean isUserLogin() {
        return StringUtils.isNotBlank(authedUid);
    }

    public String getVersionId() {
        if (clientInfo == null) {
            return "";
        }
        return clientInfo.getClientType() + ":" + clientInfo.getVersionCode();
    }

    public String getClientMetaId() {
        if (clientInfo == null || !isUserLogin()) {
            return "";
        }
        return DigestUtil.md5(authedUid + ":" + getVersionId());
    }

    /**
     * 创建一个默认传输对象, 默认不带客户端信息
     */
    public AuthedDTO buildDTO() {
        return buildDTO(false);
    }

    /**
     * 创建一个默认传输对象
     */
    public AuthedDTO buildDTO(boolean needClientInfo) {
        return buildDTO(new AuthedDTO(), needClientInfo);
    }

    /**
     * 创建一个指定传输对象, 默认不带客户端信息
     */
    public <T extends AuthedDTO> T buildDTO(T authedDTO) {
        return buildDTO(authedDTO, false);
    }

    /**
     * 创建一个指定传输对象
     */
    public <T extends AuthedDTO> T buildDTO(T authedDTO, boolean needClientInfo) {
        authedDTO.setAuthedUid(this.authedUid);
        if (needClientInfo) {
            authedDTO.setClientInfo(clientInfo);
        }
        return authedDTO;
    }

}
