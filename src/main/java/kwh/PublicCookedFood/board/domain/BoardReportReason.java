package kwh.PublicCookedFood.board.domain;

import lombok.Getter;

@Getter
public enum BoardReportReason {
    SPAM("스팸/홍보"),
    ABUSE("욕설/혐오/괴롭힘"),
    OBSCENE("음란/불건전 내용"),
    PERSONAL_INFO("개인정보 노출"),
    OTHER("기타");

    private final String label;

    BoardReportReason(String label) {
        this.label = label;
    }

}
