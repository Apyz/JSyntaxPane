package org.simon.util.swing;

import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author Emil Simon
 */

public class JSyntaxPane extends JTextPane implements DocumentListener {
    
    private final SimpleAttributeSet afterKeywordAttribs;
    private final SimpleAttributeSet keywordAttribs;
    private final SimpleAttributeSet commentAttribs;
    private final SimpleAttributeSet numberAttribs;
    private final SimpleAttributeSet stringAttribs;
    private final SimpleAttributeSet normalAttribs;
    
    
    
    public JSyntaxPane () {
        super ();
        this.getDocument().addDocumentListener(this);
        this.getStyledDocument().putProperty(StyleConstants.TabSet, 4);
        
        afterKeywordAttribs = new SimpleAttributeSet ();
        afterKeywordAttribs.addAttribute(StyleConstants.Bold, true);
        keywordAttribs = new SimpleAttributeSet ();
        keywordAttribs.addAttribute(StyleConstants.Foreground, Color.blue);
        commentAttribs = new SimpleAttributeSet ();
        commentAttribs.addAttribute(StyleConstants.Foreground, Color.gray);
        commentAttribs.addAttribute(StyleConstants.Italic, true);
        numberAttribs = new SimpleAttributeSet ();
        numberAttribs.addAttribute(StyleConstants.Foreground, new Color (110,145,0));
        stringAttribs = new SimpleAttributeSet ();
        stringAttribs.addAttribute(StyleConstants.Italic, true);
        stringAttribs.addAttribute(StyleConstants.Foreground, Color.orange);
        normalAttribs = new SimpleAttributeSet ();
        normalAttribs.addAttribute(StyleConstants.Bold, false);
        normalAttribs.addAttribute(StyleConstants.Italic, false);
        normalAttribs.addAttribute(StyleConstants.Foreground, Color.black);
    }
    
    
    
    public boolean isKeyword (String word) {
        if (word.isEmpty())
            return false;
        
        for (JSKeywordsEnum keyword : JSKeywordsEnum.values()) {
            if (word.equals(keyword.toString().toLowerCase()))
                return true;
        }
        return false;
    }
    
    public boolean isNamedKeyword (String word) {
        return word.equals("var") || word.equals("function") || word.equals("class");
    }
    
    public boolean isNumber (String word) {
        if (word.isEmpty())
            return false;
        
        for (Character ch : word.toCharArray()) {
            if (!Character.isDigit(ch) && (ch!='.') && (ch!=','))
                return false;
        }
        return true;
    }
    
    
    
    public void paintText () throws BadLocationException {
        StyledDocument doc = this.getStyledDocument();
        String token = "";
        boolean lastTokenWasKeyword = false;
        boolean lastTokenWasBackslash = false;
        Character stringify = null;
        
        int position;
        char[] chars = (doc.getText(0, doc.getLength())+" ").toCharArray();
        for (position=0;position<chars.length;position++) {
            Character ch = chars[position];
            
            if (ch=='/') {
                if (position+1 < chars.length) {
                    if (chars[position+1]==ch) {
                        int len = this.getLineLength(position);
                        doc.setCharacterAttributes(position, len, commentAttribs, true);
                        position += len;
                        continue;
                    }
                }
            }
            
            if ((ch=='\\') && (stringify!=null)) {
                lastTokenWasBackslash = true;
                token += ch;
                doc.setCharacterAttributes(position, 1, stringAttribs, true);
            } else {
                if ((ch=='\'') || (ch=='\"')) {
                    if (stringify == null) {
                        stringify = ch;
                        token += ch;
                        doc.setCharacterAttributes(position, 1, stringAttribs, true);
                    } else {
                        if (ch.equals(stringify) && !lastTokenWasBackslash) {
                            stringify = null;
                        }
                        token += ch;
                        doc.setCharacterAttributes(position, 1, stringAttribs, true);
                        lastTokenWasKeyword = false;
                        token = "";
                    }
                } else if (stringify!=null) {
                    token += ch;
                    doc.setCharacterAttributes(position, 1, stringAttribs, true);
                } else if (Character.isWhitespace(ch)) {
                    if (isKeyword(token)) {
                        doc.setCharacterAttributes(position-token.length(), token.length(), keywordAttribs, true);
                        if (isNamedKeyword(token))
                            lastTokenWasKeyword = true;
                    } else if (isNumber(token)) {
                        doc.setCharacterAttributes(position-token.length(), token.length(), numberAttribs, true);
                        lastTokenWasKeyword = false;
                    } else {
                        if (lastTokenWasKeyword) {
                            doc.setCharacterAttributes(position-token.length(), token.length(), afterKeywordAttribs, true);
                            lastTokenWasKeyword = false;
                        } else {
                            doc.setCharacterAttributes(position-token.length(), token.length(), normalAttribs, true);
                            lastTokenWasKeyword = false;
                        }
                    }
                    token = "";
                } else {
                    if (Character.isAlphabetic(ch) || Character.isDigit(ch) || Character.isWhitespace(ch)) {
                        token += ch;
                    } else {
                        if (isKeyword(token)) {
                            doc.setCharacterAttributes(position-token.length(), token.length(), keywordAttribs, true);
                            lastTokenWasKeyword = true;
                        } else if (isNumber(token)) {
                            doc.setCharacterAttributes(position-token.length(), token.length(), numberAttribs, true);
                            lastTokenWasKeyword = false;
                        } else {
                            if (lastTokenWasKeyword) {
                                doc.setCharacterAttributes(position-token.length(), token.length(), afterKeywordAttribs, true);
                                lastTokenWasKeyword = false;
                            } else {
                                doc.setCharacterAttributes(position-token.length(), token.length(), normalAttribs, true);
                                lastTokenWasKeyword = false;
                            }
                        } 
                        token = "";
                    }
                }
                lastTokenWasBackslash = false;
            }
            
            position++;
        }
        doc.setCharacterAttributes(doc.getLength(), 1, normalAttribs, true);
    }
    
    
    
    private int getLineLength (int start) throws BadLocationException {
        StyledDocument doc = this.getStyledDocument();
        int len = 0;
        
        for (;len<doc.getLength()-start;len++) {
            char c = doc.getText(start+len,2).charAt(0);
            if (c=='\n') return len;
            len++;
        }
        
        return -1;
    }
    
    
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(() -> {
            try { paintText(); }
            catch (BadLocationException ex) {  }
        });
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(() -> {
            try { paintText(); }
            catch (BadLocationException ex) {  }
        });
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }
    
}
