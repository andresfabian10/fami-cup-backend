package com.famicup.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

public final class NombreEquipoUtil {

    private static final Map<String, String> NAMES_ES = Map.ofEntries(
            Map.entry("argentina", "Argentina"),
            Map.entry("australia", "Australia"),
            Map.entry("austria", "Austria"),
            Map.entry("belgium", "Belgica"),
            Map.entry("belgiumw", "Belgica"),
            Map.entry("bosniaherzegovina", "Bosnia y Herzegovina"),
            Map.entry("brazil", "Brasil"),
            Map.entry("canada", "Canada"),
            Map.entry("chile", "Chile"),
            Map.entry("colombia", "Colombia"),
            Map.entry("costarica", "Costa Rica"),
            Map.entry("cotedivoire", "Costa de Marfil"),
            Map.entry("croatia", "Croacia"),
            Map.entry("czechrepublic", "Republica Checa"),
            Map.entry("denmark", "Dinamarca"),
            Map.entry("drcongo", "RD Congo"),
            Map.entry("ecuador", "Ecuador"),
            Map.entry("egypt", "Egipto"),
            Map.entry("england", "Inglaterra"),
            Map.entry("france", "Francia"),
            Map.entry("germany", "Alemania"),
            Map.entry("ghana", "Ghana"),
            Map.entry("honduras", "Honduras"),
            Map.entry("iran", "Iran"),
            Map.entry("italy", "Italia"),
            Map.entry("ivorycoast", "Costa de Marfil"),
            Map.entry("japan", "Japon"),
            Map.entry("korea", "Corea del Sur"),
            Map.entry("mexico", "Mexico"),
            Map.entry("morocco", "Marruecos"),
            Map.entry("netherlands", "Paises Bajos"),
            Map.entry("newzealand", "Nueva Zelanda"),
            Map.entry("nigeria", "Nigeria"),
            Map.entry("norway", "Noruega"),
            Map.entry("panama", "Panama"),
            Map.entry("paraguay", "Paraguay"),
            Map.entry("peru", "Peru"),
            Map.entry("poland", "Polonia"),
            Map.entry("portugal", "Portugal"),
            Map.entry("qatar", "Catar"),
            Map.entry("romania", "Rumania"),
            Map.entry("saudiarabia", "Arabia Saudita"),
            Map.entry("scotland", "Escocia"),
            Map.entry("senegal", "Senegal"),
            Map.entry("serbia", "Serbia"),
            Map.entry("southafrica", "Sudafrica"),
            Map.entry("southkorea", "Corea del Sur"),
            Map.entry("spain", "Espana"),
            Map.entry("sweden", "Suecia"),
            Map.entry("switzerland", "Suiza"),
            Map.entry("tunisia", "Tunez"),
            Map.entry("turkey", "Turquia"),
            Map.entry("uruguay", "Uruguay"),
            Map.entry("usa", "Estados Unidos"),
            Map.entry("uzbekistan", "Uzbekistan"),
            Map.entry("wales", "Gales"));

    private NombreEquipoUtil() {
    }

    public static String displayName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return NAMES_ES.getOrDefault(normalize(value), value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }
}
