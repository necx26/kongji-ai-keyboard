package com.kongji.aikeyboard;

/** Candidate text and touch height grow together; margins fit inside the strip. */
final class CandidateLayout {
    static final String[] LABELS={"极小","小","中","大","极大"};
    static String label(android.content.Context c,int index){return Language.effective(c).equals("zh")?LABELS[index]:Language.text(c,"候选词大小·"+LABELS[index]);}
    static int font(KeyboardStyle s){return new int[]{12,14,17,20,23}[KeyboardStyle.clamp(s.candidateSize,0,4)];}
    static int keyHeight(KeyboardStyle s){return new int[]{32,36,40,46,52}[KeyboardStyle.clamp(s.candidateSize,0,4)];}
    static int barHeight(KeyboardStyle s){return keyHeight(s)+4;}
}
