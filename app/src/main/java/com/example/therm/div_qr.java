package com.example.therm;

// 割り算の商と余りを配列に保持するクラス
final class div_qr {

    private long num[];

    // コンストラクタ：割られる数を入れる
    div_qr(long a) {
        num = new long[]{a};
    }

    // divideメソッド：割る数を入れて割り算を実行する
    div_qr divide(long b) {
        long array[] = this.getArray();
        num = new long[array.length + 1];

        // 配列を拡張
        System.arraycopy(array, 1, num, 2, array.length - 1);

        // 元の配列の最初の要素を割って、商と余りを新しい配列の最初と２番目の要素に入れる
        num[0] = array[0] / b;
        num[1] = array[0] % b;
        return this;
    }

    // 配列を返す
    long[] getArray() {
        return num;
    }
}

