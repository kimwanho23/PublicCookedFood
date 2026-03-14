package kwh.PublicCookedFood.storage;

public record ImageDownloadQuery(String imageUrl) {

    public static ImageDownloadQuery of(String imageUrl) {
        return new ImageDownloadQuery(imageUrl);
    }
}
