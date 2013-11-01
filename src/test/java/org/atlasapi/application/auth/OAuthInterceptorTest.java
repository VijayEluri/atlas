package org.atlasapi.application.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigInteger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.users.User;
import org.atlasapi.media.common.Id;
import org.atlasapi.output.NotAuthorizedException;
import org.atlasapi.output.UserProfileIncompleteException;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.ids.NumberToShortStringCodec;

public class OAuthInterceptorTest {

    @Test
    public void testProtectsUrlNoUser() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/myprotectedUrl.json");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream stream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(stream);
        UserFetcher userFetcher = mock(UserFetcher.class);
        NumberToShortStringCodec idCodec = mock(NumberToShortStringCodec.class);
        when(userFetcher.userFor(request)).thenReturn(Optional.<User>absent());
        OAuthInterceptor interceptor = OAuthInterceptor
                .builder()
                .withUserFetcher(userFetcher)
                .withIdCodec(idCodec)
                .withUrlsToProtect(ImmutableSet.of(
                        "/myprotectedUrl"))
                .withUrlsNotNeedingCompleteProfile(ImmutableSet.of(""))
                .build();
        assertFalse(interceptor.preHandle(request, response, null));
    }
    
    @Test
    public void testProtectsUrlNeedingFullProfile() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(Id.valueOf(new BigInteger("5000")));
        when(user.isProfileComplete()).thenReturn(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/myprotectedUrl.json");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream stream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(stream);
        UserFetcher userFetcher = mock(UserFetcher.class);
        NumberToShortStringCodec idCodec = mock(NumberToShortStringCodec.class);
        when(idCodec.encode(new BigInteger("5000"))).thenReturn("bcdf");
        when(userFetcher.userFor(request)).thenReturn(Optional.<User>of(user));
        OAuthInterceptor interceptor = OAuthInterceptor
                .builder()
                .withUserFetcher(userFetcher)
                .withIdCodec(idCodec)
                .withUrlsToProtect(ImmutableSet.of(
                        "/myprotectedUrl"))
                .withUrlsNotNeedingCompleteProfile(ImmutableSet.of(""))
                .build();
       assertFalse(interceptor.preHandle(request, response, null));
    }
    
    @Test
    public void testNeedingFullProfileException() throws Exception  {
        User user = mock(User.class);
        when(user.getId()).thenReturn(Id.valueOf(new BigInteger("5000")));
        when(user.isProfileComplete()).thenReturn(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/something/bcdf.json");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream stream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(stream);
        UserFetcher userFetcher = mock(UserFetcher.class);
        NumberToShortStringCodec idCodec = mock(NumberToShortStringCodec.class);
        when(idCodec.encode(new BigInteger("5000"))).thenReturn("bcdf");
        when(userFetcher.userFor(request)).thenReturn(Optional.<User>of(user));
        OAuthInterceptor interceptor = OAuthInterceptor
                .builder()
                .withUserFetcher(userFetcher)
                .withIdCodec(idCodec)
                .withUrlsToProtect(ImmutableSet.of(
                        "/myprotectedUrl"))
                .withUrlsNotNeedingCompleteProfile(ImmutableSet.of("/something/:uid"))
                .build();
        assertTrue(interceptor.preHandle(request, response, null));
    }
    
    @Test
    public void testNotProtectingOtherUrl() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(Id.valueOf(new BigInteger("5000")));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/other.json");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream stream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(stream);
        UserFetcher userFetcher = mock(UserFetcher.class);
        NumberToShortStringCodec idCodec = mock(NumberToShortStringCodec.class);
        when(idCodec.encode(new BigInteger("5000"))).thenReturn("bcdf");
        when(userFetcher.userFor(request)).thenReturn(Optional.<User>of(user));
        OAuthInterceptor interceptor = OAuthInterceptor
                .builder()
                .withUserFetcher(userFetcher)
                .withIdCodec(idCodec)
                .withUrlsToProtect(ImmutableSet.of(
                        "/myprotectedUrl"))
                .withUrlsNotNeedingCompleteProfile(ImmutableSet.of("/something/:uid"))
                .build();
        assertTrue(interceptor.preHandle(request, response, null));
    }
}
