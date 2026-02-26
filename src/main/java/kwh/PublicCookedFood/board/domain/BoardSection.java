package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_section", indexes = {
        @Index(name = "idx_board_section_display_order", columnList = "displayOrder"),
        @Index(name = "idx_board_section_active_display_order", columnList = "active, displayOrder")
})
public class BoardSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sectionKey;

    @Column(nullable = false, length = 100)
    private String sectionName;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean active;

    @Builder
    public BoardSection(Long id, String sectionKey, String sectionName, Integer displayOrder, Boolean active) {
        this.id = id;
        this.sectionKey = sectionKey;
        this.sectionName = sectionName;
        this.displayOrder = displayOrder;
        this.active = active == null || active;
    }

    public static BoardSection createDefault(String sectionKey, String sectionName) {
        return BoardSection.builder()
                .sectionKey(sectionKey)
                .sectionName(sectionName)
                .displayOrder(0)
                .active(true)
                .build();
    }

    public void update(String sectionName, Integer displayOrder, Boolean active) {
        if (sectionName != null) {
            this.sectionName = sectionName;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        if (active != null) {
            this.active = active;
        }
    }
}
