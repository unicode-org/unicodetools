package org.unicode.text.tools;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class TestAlphaComposite extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int PREF_W = 400;
    private static final int PREF_H = PREF_W;
    private static final Stroke BASIC_STROKE = new BasicStroke(6f);
    BufferedImage backgroundImage;
    BufferedImage overlayImage;

    public TestAlphaComposite() {
        backgroundImage = createBackGroundImage();
        overlayImage = createOverlayImage();
    }

    private BufferedImage createBackGroundImage() {
        BufferedImage img = new BufferedImage(PREF_W, PREF_H,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(BASIC_STROKE);
        g2.setColor(Color.blue);
        int circleCount = 10;
        for (int i = 0; i < circleCount ; i++) {
            int x = (i * PREF_W) / (2 * circleCount);
            int y = x;
            int w = PREF_W - 2 * x;
            int h = w;
            g2.drawOval(x, y, w, h);
        }
        g2.dispose();
        return img;
    }

    private BufferedImage createOverlayImage() {
        BufferedImage img = new BufferedImage(PREF_W, PREF_H,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(BASIC_STROKE);
        g2.setColor(Color.red);
        int circleCount = 10;
        for (int i = 0; i < circleCount + 1; i++) {
            int x1 = (i * PREF_W) / (circleCount);
            int y1 = 0;
            int x2 = PREF_W - x1;
            int y2 = PREF_H;
            float alpha = (float)i / circleCount;
            if (alpha > 1f) {
                alpha = 1f;
            }
            // int rule = AlphaComposite.CLEAR;
            int rule = AlphaComposite.SRC_OVER;
            Composite comp = AlphaComposite.getInstance(rule , alpha );
            g2.setComposite(comp );
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.dispose();
        return img;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREF_W, PREF_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, null);
        }
        if (overlayImage != null) {
            g.drawImage(overlayImage, 0, 0, null);
        }
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("TestAlphaComposite");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new TestAlphaComposite());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGui();
            }
        });
    }
}