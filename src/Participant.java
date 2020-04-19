public enum Participant {
    OUTSIDE("localhost", 9010),
    TM ("localhost", 9000),
    A ("localhost", 9001),
    B ("localhost", 9002),
    C ("localhost", 9003),
    D ("localhost", 9004),
    E ("localhost", 9005);

    private final String host;
    private final int port;
    Participant(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
