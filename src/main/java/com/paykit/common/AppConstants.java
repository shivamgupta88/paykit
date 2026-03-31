package com.paykit.common;

public final class AppConstants {

    private AppConstants() {}

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final int INVOICE_NUMBER_PAD_LENGTH = 6;
    public static final String INVOICE_PREFIX = "INV-";

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    public static final long IDEMPOTENCY_TTL_HOURS = 24;

    public static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 128;
}
