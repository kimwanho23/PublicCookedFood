package kwh.PublicCookedFood.board.domain;

public enum BoardReportStatus {
    OPEN("\uCC98\uB9AC \uB300\uAE30"),
    RESOLVED("\uCC98\uB9AC \uC644\uB8CC"),
    REJECTED("\uBC18\uB824");

    private final String label;

    BoardReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
