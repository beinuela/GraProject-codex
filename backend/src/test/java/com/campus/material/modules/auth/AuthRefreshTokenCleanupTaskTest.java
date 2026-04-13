package com.campus.material.modules.auth;

import com.campus.material.modules.auth.mapper.AuthRefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRefreshTokenCleanupTaskTest {

    @Mock
    private AuthRefreshTokenMapper authRefreshTokenMapper;

    private AuthRefreshTokenCleanupTask task;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        task = new AuthRefreshTokenCleanupTask(authRefreshTokenMapper);
    }

    @Test
    void cleanupNowShouldReturnDeletedCount() {
        when(authRefreshTokenMapper.delete(any())).thenReturn(3);

        int removed = task.cleanupNow();

        assertEquals(3, removed);
        verify(authRefreshTokenMapper, times(1)).delete(any());
    }

    @Test
    void scheduledCleanupShouldInvokeDelete() {
        when(authRefreshTokenMapper.delete(any())).thenReturn(1);

        task.scheduledCleanup();

        verify(authRefreshTokenMapper, times(1)).delete(any());
    }
}
