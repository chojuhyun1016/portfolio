package org.example.order.core.domain.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.common.auth.AccessUserInfo;
import org.example.order.common.auth.AccessUserManager;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VersionEntity {

    @Column(columnDefinition = "bigint COMMENT '등록자'")
    private Long createdUserId;

    @Column(columnDefinition = "bigint COMMENT '등록자유형'")
    private String createdUserType;

    @Column(columnDefinition = "bigint COMMENT '등록일시'")
    private LocalDateTime createdDatetime;

    @Column(columnDefinition = "bigint COMMENT '수정자'")
    private Long modifiedUserId;

    @Column(columnDefinition = "bigint COMMENT '수정자유형'")
    private String modifiedUserType;

    @Column(columnDefinition = "bigint COMMENT '수정일시'")
    private LocalDateTime modifiedDatetime;

    @Version
    @Column(columnDefinition = "bigint COMMENT 'Data Version'")
    private Long version;

    @PrePersist
    public void prePersist() {
        AccessUserInfo userInfo = AccessUserManager.getAccessUser();
        this.createdUserId = userInfo.userId();
        this.createdUserType = userInfo.userType();
        this.createdDatetime = LocalDateTime.now();
        this.modifiedDatetime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        AccessUserInfo userInfo = AccessUserManager.getAccessUser();
        this.modifiedUserId = userInfo.userId();
        this.modifiedUserType = userInfo.userType();
        this.modifiedDatetime = LocalDateTime.now();
    }
}
