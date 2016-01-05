package org.unicode.text.tools;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class CompositionEx extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CompositionEx() {

        add(new Surface());

        setTitle("Composition");
        setSize(400, 120);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                CompositionEx ex = new CompositionEx();
                ex.setVisible(true);
            }
        });
    }
}