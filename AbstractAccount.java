/**
 * [CO2]: Abstract Class demonstrating Encapsulation, Abstract methods,
 * static/final keywords, and interface implementation.
 */
public abstract class AbstractAccount implements Authenticatable {
    protected final String name;
    protected final String password;
    protected final String contact;

    public AbstractAccount(String name, String password, String contact) {
        this.name = (name == null) ? "" : name.trim();
        this.password = (password == null) ? "" : password.trim();
        this.contact = (contact == null) ? "" : contact.trim();
    }

    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null) return false;
        return this.password.equals(rawPassword.trim());
    }

    /** Abstract method for account representation */
    public abstract String getAccountSummary();
}
