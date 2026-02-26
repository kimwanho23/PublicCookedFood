package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "images", indexes = {
        @Index(name = "idx_images_status_reg_time", columnList = "status, regTime"),
        @Index(name = "idx_images_url_status", columnList = "imgUrl, status")
})
public class Images extends BaseEntity {

    public enum ImageStatus {
        TEMP,
        ATTACHED,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //ID

    @Column(nullable = false)
    private String originalFilename; //원본 파일

    @Column(nullable = false)
    private String savedFilename; // 저장 파일(서버)

    @Column(nullable = false, length = 500)
    private String imgUrl; // 이미지 경로

    @Column(length = 100)
    private String contentType; // MIME Type

    @Column
    private Long fileSize; // bytes

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ImageStatus status;

    @Builder
    public Images(Long id, String originalFilename, String savedFilename, String imgUrl,
                  String contentType, Long fileSize, ImageStatus status) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.savedFilename = savedFilename;
        this.imgUrl = imgUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = status == null ? ImageStatus.TEMP : status;
    }

    public static Images createTemporary(String originalFilename,
                                         String savedFilename,
                                         String imgUrl,
                                         String contentType,
                                         long fileSize) {
        return Images.builder()
                .originalFilename(originalFilename)
                .savedFilename(savedFilename)
                .imgUrl(imgUrl)
                .contentType(contentType == null ? "" : contentType)
                .fileSize(fileSize)
                .status(ImageStatus.TEMP)
                .build();
    }

    public void attach() {
        this.status = ImageStatus.ATTACHED;
    }

    public void markDeleted() {
        this.status = ImageStatus.DELETED;
    }

    @PrePersist
    protected void prePersist() {
        if (status == null) {
            status = ImageStatus.TEMP;
        }
    }
}
