package org.qpeek.qpeek.domain.notification.enums;

public enum NotificationType {
    BEFORE_DAY,   // D-1
    IMMINENT,    // 임박(N시간 전)
    DUE,         // 마감 시각
    OVERDUE,     // 초과
    REPORT       // 데일리 클로징 보고
}
