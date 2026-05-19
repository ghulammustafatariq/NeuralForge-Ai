package com.superior.mindforgeai;

public interface DeleteContentCallback {
    void onDeleteExplanation(int index);
    void onDeleteStep(int index);
    void onDeleteFormula(int index);
    void onDeleteCode(int index);
}
