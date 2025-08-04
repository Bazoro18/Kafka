public class BookingEvent {
    private String bookingId;
    private String userId;
    private String movie;
    private String theaterId;
    private String timestamp;
    private int seats;
    private double amount;

    public BookingEvent(String bookingId, String userId, String movie, String theaterId, String timestamp, int seats, double amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.movie = movie;
        this.theaterId = theaterId;
        this.timestamp = timestamp;
        this.seats = seats;
        this.amount = amount;
    }
    public String getBookingId() {
        return bookingId;
    }
    public String getUserId() {
        return userId;
    }
    public String getMovie() {
        return movie;
    }
    public String getTheaterID() {
        return theaterId;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public int getSeats() {
        return seats;
    }
    public double getAmount() {
        return amount;
    }
}