package org.atlasapi.remotesite.getty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Matchers;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class GettyUpdateTaskTest {

    private final GettyAdapter adapter = mock(GettyAdapter.class);
    private final GettyDataHandler handler = mock(GettyDataHandler.class);
    private final GettyTokenFetcher tokenFetcher = mock(GettyTokenFetcher.class);
    private final GettyClient gettyClient = mock(GettyClient.class);
    private final IrisKeywordsFetcher keywordsFetcher = mock(IrisKeywordsFetcher.class);
    private final ScheduledTask task = new GettyUpdateTask(gettyClient, adapter, handler, keywordsFetcher, 90, 25);
    
    @Test
    public void testTask() throws Exception {
        when(tokenFetcher.getToken()).thenReturn("token");
        when(keywordsFetcher.getKeywordsFromOffset(0)).thenReturn(ImmutableList.of("key1", "key2"));
        when(keywordsFetcher.getKeywordsFromOffset(90)).thenReturn(ImmutableList.<String>of());
        when(gettyClient.getVideoResponse(Matchers.eq("key1"), Matchers.eq(1))).thenReturn("val1");
        when(gettyClient.getVideoResponse(Matchers.eq("key2"), Matchers.eq(91))).thenReturn("val2");
        when(adapter.parse(Matchers.eq("val1"), Matchers.eq("key1"))).thenReturn(ImmutableList.of(new VideoResponse(), new VideoResponse()));
        when(adapter.parse(Matchers.eq("val2"), Matchers.eq("key2"))).thenReturn(ImmutableList.<VideoResponse>of());
        
        task.run();
        // first page has 2 keywords so it tries to fetch another page
        verify(keywordsFetcher, times(2)).getKeywordsFromOffset(Matchers.anyInt());
        
        verify(gettyClient, times(1)).getVideoResponse(Matchers.eq("key1"), Matchers.eq(1));
        
        verify(gettyClient, times(1)).getVideoResponse(Matchers.eq("key2"), Matchers.eq(1));
        
        verify(adapter, times(1)).parse(Matchers.eq("val1"), Matchers.eq("key1"));
        verify(adapter, times(0)).parse(Matchers.eq("val2"), Matchers.eq("key1"));
        verify(handler, times(2)).handle(Matchers.any(VideoResponse.class));
    }
    
}
