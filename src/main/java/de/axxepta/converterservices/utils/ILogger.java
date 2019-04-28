package de.axxepta.converterservices.utils;

@FunctionalInterface
public interface ILogger<T> {
    void log(T t);
}