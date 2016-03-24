public class Contact {

    public String email;
    public String fullName;
    public boolean debug;

    public boolean ok;

    public Contact(String csvSource) {
        try {
            String[] elements = csvSource.split(",");

            if (elements.length != 3) {
                System.out.println("Contact - bad csvSource = " + csvSource);
                ok = false;
                return;
            }

            email = elements[0].trim();
            fullName = elements[1].trim();
            debug = Boolean.parseBoolean(elements[2].trim());

            ok = true;
        } catch (Exception e) {
            ok = false;
        }
    }

}
