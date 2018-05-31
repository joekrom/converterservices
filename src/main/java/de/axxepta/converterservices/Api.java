package de.axxepta.converterservices;

public abstract class Api {
    public static final int VERSION = 1;

    protected Api() {
        init(Api.VERSION);
    }

    protected abstract void init(int version) throws IllegalStateException;
}
