package com.bt.pi.sss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

public class DhtUserManagerTest {
    private DhtUserManager dhtUserManager;
    private String accessKey = "abc123";
    private User user;
    private String secretKey = "secret";
    private String userName = "fred";
    private PiIdBuilder piIdBuilder;
    private PId userAccessKeyId;
    private PId userId;
    private UserAccessKey userAccessKey;
    private BlockingDhtCache blockingDhtCache;

    @Before
    public void setUp() throws Exception {
        this.dhtUserManager = new DhtUserManager();
        this.userAccessKey = new UserAccessKey(userName, accessKey);
        this.user = new User(userName, accessKey, secretKey);

        this.piIdBuilder = mock(PiIdBuilder.class);
        this.dhtUserManager.setPiIdBuilder(this.piIdBuilder);
        this.blockingDhtCache = mock(BlockingDhtCache.class);
        this.dhtUserManager.setUserCache(this.blockingDhtCache);

        this.userAccessKeyId = mock(PId.class);
        when(this.piIdBuilder.getPId(UserAccessKey.getUrl(accessKey))).thenReturn(userAccessKeyId);
        this.userId = mock(PId.class);
        when(this.piIdBuilder.getPId(User.getUrl(userAccessKey.getUsername()))).thenReturn(userId);

        when(this.blockingDhtCache.get(userAccessKeyId)).thenReturn(userAccessKey);
        when(this.blockingDhtCache.get(userId)).thenReturn(user);
    }

    @Test
    public void testGetUserByAccessKey() {
        // setup

        // act
        User result = this.dhtUserManager.getUserByAccessKey(accessKey);

        // assert
        assertEquals(user, result);
    }

    @Test
    public void testUserExistsTrue() {
        // setup

        // act
        boolean result = this.dhtUserManager.userExists(accessKey);

        // assert
        assertTrue(result);
    }

    @Test
    public void testUserExistsFalse() {
        // setup

        // act
        boolean result = this.dhtUserManager.userExists("zzzzz");

        // assert
        assertFalse(result);
    }

}
