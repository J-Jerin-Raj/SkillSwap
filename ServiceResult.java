/**
 * [CO4]: Generic Class demonstrating Type Parameters, Generic Methods,
 * and result encapsulation across application tiers.
 */
public class ServiceResult<T> {
    private final boolean success;
    private final String message;
    private final T data;

    public ServiceResult(boolean success, String message, T data) {
        this.success = success;
        this.message = (message == null) ? "" : message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // Generic static factory methods
    public static <T> ServiceResult<T> success(String message, T data) {
        return new ServiceResult<>(true, message, data);
    }

    public static <T> ServiceResult<T> failure(String message) {
        return new ServiceResult<>(false, message, null);
    }
}
