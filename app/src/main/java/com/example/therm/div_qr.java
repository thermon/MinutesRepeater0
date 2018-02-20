package com.example.therm;

import java.util.ArrayList;

// 割り算の商と余りを配列に保持するクラス
final class div_qr {

    private ArrayList<Integer> num = new ArrayList<>(5);

    // コンストラクタ：割られる数を入れる
    div_qr(int a) {
        num.add(a);
    }

    // divideメソッド：割る数を入れて割り算を実行する
    div_qr divide(int b) {
        // ArrayListの最後の要素＝前回の計算の商
        // 最後の要素に余りを入れて
        // 商の要素を追加する
        int last = num.size() - 1;
        int a = num.get(last);

        num.set(last, a % b);
        num.add(a / b);

        return this;
    }

    // 配列を返す
    int[] getArray() {
        // numを逆順にする
        int size = num.size();
        int[] num2 = new int[num.size()];

        for (int idx = 0; idx < size; idx++) {
            num2[idx] = num.get(size - idx - 1);
        }
        return num2;
    }
}
