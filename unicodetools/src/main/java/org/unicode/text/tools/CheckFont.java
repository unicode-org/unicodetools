package org.unicode.text.tools;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.ibm.icu.text.UnicodeSet;

public class CheckFont {
    static final UnicodeSet EMOJI_CHARS = new UnicodeSet(
            "[©®‼⁉℗™ℹ↔-↙↩↪⌚⌛⌤-⌨⌫⌬⍾⎆-⎈⎋⎗-⎚⏏⏣⏩-⏺Ⓜ▪▫▶◀◻-◾☀-☆☎-☒☔-☠☢-☤☮☯☸-☾♈-♯♲♻♾-⚅⚐-⚝⚠⚡⚪⚫⚰⚱⚽-⛊⛌-⛡⛨-✘✨✳✴❄❇❌❎❓-❕❗❢-❧➕-➗➡➰➿⤴⤵⬅-⬇⬛⬜⭐⭕⸙〠〰〽㊗㊙￼🀀-🀫🀰-🂓🂠-🂮🂱-🂿🃁-🃏🃑-🃵🅰🅱🅾🅿🆊🆎🆏🆑-🆚🇦-🇿🈁🈂🈚🈯🈲-🈺🉐🉑🌀-🌬🌰-🍽🎀-🏎🏔-🏷🐀-📾🔀-🔿🕊🕐-🕹🕻-🖣🖥-🙂🙅-🙏🙬-🙯🚀-🛏🛠-🛬🛰-🛳{#⃣}{0⃣}{1⃣}{2⃣}{3⃣}{4⃣}{5⃣}{6⃣}{7⃣}{8⃣}{9⃣}{🇦🇩}{🇦🇪}{🇦🇫}{🇦🇬}{🇦🇮}{🇦🇱}{🇦🇲}{🇦🇴}{🇦🇶}{🇦🇷}{🇦🇸}{🇦🇹}{🇦🇺}{🇦🇼}{🇦🇽}{🇦🇿}{🇧🇦}{🇧🇧}{🇧🇩}{🇧🇪}{🇧🇫}{🇧🇬}{🇧🇭}{🇧🇮}{🇧🇯}{🇧🇱}{🇧🇲}{🇧🇳}{🇧🇴}{🇧🇶}{🇧🇷}{🇧🇸}{🇧🇹}{🇧🇻}{🇧🇼}{🇧🇾}{🇧🇿}{🇨🇦}{🇨🇨}{🇨🇩}{🇨🇫}{🇨🇬}{🇨🇭}{🇨🇮}{🇨🇰}{🇨🇱}{🇨🇲}{🇨🇳}{🇨🇴}{🇨🇷}{🇨🇺}{🇨🇻}{🇨🇼}{🇨🇽}{🇨🇾}{🇨🇿}{🇩🇪}{🇩🇯}{🇩🇰}{🇩🇲}{🇩🇴}{🇩🇿}{🇪🇨}{🇪🇪}{🇪🇬}{🇪🇭}{🇪🇷}{🇪🇸}{🇪🇹}{🇫🇮}{🇫🇯}{🇫🇰}{🇫🇲}{🇫🇴}{🇫🇷}{🇬🇦}{🇬🇧}{🇬🇩}{🇬🇪}{🇬🇫}{🇬🇬}{🇬🇭}{🇬🇮}{🇬🇱}{🇬🇲}{🇬🇳}{🇬🇵}{🇬🇶}{🇬🇷}{🇬🇸}{🇬🇹}{🇬🇺}{🇬🇼}{🇬🇾}{🇭🇰}{🇭🇲}{🇭🇳}{🇭🇷}{🇭🇹}{🇭🇺}{🇮🇩}{🇮🇪}{🇮🇱}{🇮🇲}{🇮🇳}{🇮🇴}{🇮🇶}{🇮🇷}{🇮🇸}{🇮🇹}{🇯🇪}{🇯🇲}{🇯🇴}{🇯🇵}{🇰🇪}{🇰🇬}{🇰🇭}{🇰🇮}{🇰🇲}{🇰🇳}{🇰🇵}{🇰🇷}{🇰🇼}{🇰🇾}{🇰🇿}{🇱🇦}{🇱🇧}{🇱🇨}{🇱🇮}{🇱🇰}{🇱🇷}{🇱🇸}{🇱🇹}{🇱🇺}{🇱🇻}{🇱🇾}{🇲🇦}{🇲🇨}{🇲🇩}{🇲🇪}{🇲🇫}{🇲🇬}{🇲🇭}{🇲🇰}{🇲🇱}{🇲🇲}{🇲🇳}{🇲🇴}{🇲🇵}{🇲🇶}{🇲🇷}{🇲🇸}{🇲🇹}{🇲🇺}{🇲🇻}{🇲🇼}{🇲🇽}{🇲🇾}{🇲🇿}{🇳🇦}{🇳🇨}{🇳🇪}{🇳🇫}{🇳🇬}{🇳🇮}{🇳🇱}{🇳🇴}{🇳🇵}{🇳🇷}{🇳🇺}{🇳🇿}{🇴🇲}{🇵🇦}{🇵🇪}{🇵🇫}{🇵🇬}{🇵🇭}{🇵🇰}{🇵🇱}{🇵🇲}{🇵🇳}{🇵🇷}{🇵🇸}{🇵🇹}{🇵🇼}{🇵🇾}{🇶🇦}{🇷🇪}{🇷🇴}{🇷🇸}{🇷🇺}{🇷🇼}{🇸🇦}{🇸🇧}{🇸🇨}{🇸🇩}{🇸🇪}{🇸🇬}{🇸🇭}{🇸🇮}{🇸🇯}{🇸🇰}{🇸🇱}{🇸🇲}{🇸🇳}{🇸🇴}{🇸🇷}{🇸🇸}{🇸🇹}{🇸🇻}{🇸🇽}{🇸🇾}{🇸🇿}{🇹🇨}{🇹🇩}{🇹🇫}{🇹🇬}{🇹🇭}{🇹🇯}{🇹🇰}{🇹🇱}{🇹🇲}{🇹🇳}{🇹🇴}{🇹🇷}{🇹🇹}{🇹🇻}{🇹🇼}{🇹🇿}{🇺🇦}{🇺🇬}{🇺🇲}{🇺🇸}{🇺🇾}{🇺🇿}{🇻🇦}{🇻🇨}{🇻🇪}{🇻🇬}{🇻🇮}{🇻🇳}{🇻🇺}{🇼🇫}{🇼🇸}{🇽🇰}{🇾🇪}{🇾🇹}{🇿🇦}{🇿🇲}{🇿🇼}]");

    public static void main(String args[]) {
        JFrame f = new JFrame("JColorChooser Sample");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JButton button = new JButton("abc⌚⌛⏩-⏬⏰⏳Ⓜ▪▫▶◀◻-◾☀");

        Font myFont = new Font("AppleColorEmoji", Font.ITALIC | Font.BOLD, 12);

        button.setFont(myFont);

        f.add(button, BorderLayout.CENTER);
        f.setSize(300, 200);
        f.setVisible(true);
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (Font font: e.getAllFonts()) {
            String fontName = font.getFontName();
            if (fontName.equals("AppleColorEmoji")) {
                System.out.println(fontName);
                BufferedImage img = new BufferedImage(144, 72, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gif = img.createGraphics();
                gif.setFont(font);
                
                FontRenderContext frc = gif.getFontRenderContext();
                UnicodeSet s = new UnicodeSet();
                for (String cp : EMOJI_CHARS) {
                    GlyphVector gv = font.createGlyphVector(frc, cp.toCharArray());
                    boolean isOk = true;
                    for (int i = 0; i < gv.getNumGlyphs(); ++i) {
                        int code = gv.getGlyphCode(i);
                        if (code < 0) {
                            isOk = false;
                        }
                    }
                    if (isOk) {
                        s.add(cp);
//                        gif.drawString(cp, 36, 36);
//                        ImageWriter write = ImageIO.getImageWritersBySuffix("gif").next();
//                        ByteArrayOutputStream out = new ByteArrayOutputStream();
//                        try {
//                            ImageOutputStream imageos = ImageIO.createImageOutputStream(out);
//                            write.setOutput(imageos);
//                            write.write(gif);
//                            imageos.close();  // or imageos.flush();                        } catch (FileNotFoundException e) {
//                        } catch (Exception e2) {
//                        }

                    }
                }
                System.out.println("static final UnicodeSet APPLE_CHARS = new UnicodeSet(\n\""
                        + s.toPattern(false) + "\");");
            }
        }
    }
    
}
