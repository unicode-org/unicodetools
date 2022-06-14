package org.unicode.tools;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;

public class GeneratePickerData2 {

    enum Patterns {
        all,
        category_list,
        compatibility,
        enclosed,
        extended,
        historic,
        miscellaneous,
        other,
        scripts,
        strokes,
    }

    enum Categories {
        // general
        limited_use,
        miscellaneous,
        other,

        // scripts
        african_scripts,
        american_scripts,
        east_asian_scripts,
        european_scripts,
        historic_scripts,
        middle_eastern_scripts,
        modern_scripts,
        south_asian_scripts,
        southeast_asian_scripts,
        western_asian_scripts,
        phonetic_alphabet,
        braille,

        // cjk
        han_characters,
        han_radicals,
        hanja,
        hanzi_simplified,
        hanzi_traditional,
        japanese_kana,
        kanbun,
        kanji,
        ideographic_desc_characters,
        consonantal_jamo,
        vocalic_jamo,

        variant_forms,
        small_form_variant,
        full_width_form_variant,
        half_width_form_variant,

        // modifier
        modifier,
        spacing,
        nonspacing,
        tone_marks,

        // numbers
        numbers,
        digits,

        // punctuation
        punctuation,
        dash_connector,
        paired,

        // symbols
        symbols,
        arrows,
        downwards_arrows,
        downwards_upwards_arrows,
        upwards_arrows,
        leftwards_arrows,
        leftwards_rightwards_arrows,
        rightwards_arrows,
        box_drawing,
        bullets_stars,
        currency_symbols,
        musical_symbols,
        technical_symbols,
        sign_standard_symbols,
        letterlike_symbols,
        math_symbols,
        geometric_shapes,
        dingbats,
        divination_symbols,

        // format
        format_whitespace,
        format,
        whitespace,

        // pictographs
        pictographs,
        emoji,
        activities,
        animal,
        animals_nature,
        body,
        building,
        female,
        flag,
        flags,
        food_drink,
        keycap,
        heart,
        male,
        nature,
        objects,
        person,
        place,
        plant,
        smiley,
        smileys_people,
        sport,
        travel,
        travel_places,
        weather,
    }

    public static void main(String[] args) {
        for (String scriptName : ScriptMetadata.getScripts()) {
            Info info = ScriptMetadata.getInfo(scriptName);
            System.out.println(scriptName + "\t" + info);
        }
    }
}
