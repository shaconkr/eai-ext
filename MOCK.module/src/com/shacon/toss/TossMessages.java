package com.shacon.toss;

public class TossMessages {

    enum TossFrame {
        START("0800100"),       // 개시전문 (계좌검증용)
        RESTART("0801100"),     // 재개시전문
        END("0800300"),         // 종료전문
        ENDPRE("0800310"),      // 종료예고전문
        SYSCHK("0800800"),      // 시스템 체크/테스트 전문
        ERRNOTI("0600900");     // 가상계좌 장애통보전문

        private final String stringValue;

        TossFrame(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }

    }
}
