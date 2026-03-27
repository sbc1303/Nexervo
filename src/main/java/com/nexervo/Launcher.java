package com.nexervo;

/**
 * Punto de entrada del JAR.
 *
 * Por qué existe esta clase separada de Main:
 * Cuando JavaFX 11+ se empaqueta en un JAR sin módulos, el classloader
 * no puede cargar una clase que extienda Application directamente desde
 * el MANIFEST.MF. Launcher no tiene dependencias de JavaFX, por lo que
 * el JAR arranca correctamente en cualquier máquina (Windows, macOS, Linux).
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
