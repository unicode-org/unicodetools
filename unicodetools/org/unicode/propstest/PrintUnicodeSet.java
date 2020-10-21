package org.unicode.propstest;

import com.ibm.icu.text.UnicodeSet;

public class PrintUnicodeSet {
    public static void main(String[] args) {
	System.out.println(new UnicodeSet(
		"[‾‽‸⁂↚↛↮↙↜↝↞↟↠↡↢↣↤↥↦↧↨↫↬↭↯↰↱↲↳↴↵↶↷↸↹↺↻↼↽↾↿⇀⇁⇂⇃⇄⇇⇈⇉⇊⇋⇌⇐⇍"
			+ "⇑⇒⇏⇓⇔⇎⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪⇵∀∂∃∅∉∋∎∏∑≮≯∓∕⁄∗∘∙∝∟∠∣∥∧∫∬∮∴∵∶∷∼∽∾"
			+ "≃≅≌≒≖≣≦≧≪≫≬≳≺≻⊁⊃⊆⊇⊕⊖⊗⊘⊙⊚⊛⊞⊟⊥⊮⊰⊱⋭⊶⊹⊿⋁⋂⋃⋅⋆⋈⋒⋘⋙⋮⋯⋰⋱■□▢▣▤▥▦▧▨▩▬▭▮▰△▴▵▷▸▹►▻▽▾"
			+ "▿◁◂◃◄◅◆◇◈◉◌◍◎◐◑◒◓◔◕◖◗◘◙◜◝◞◟◠◡◢◣◤◥◦◳◷◻◽◿⨧⨯⨼⩣⩽⪍⪚⪺₢₣₤₰₳₶₷₨﷼]"
		).toPattern(false));
    }
}
