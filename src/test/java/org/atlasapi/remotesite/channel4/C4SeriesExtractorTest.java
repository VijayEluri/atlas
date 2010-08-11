package org.atlasapi.remotesite.channel4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Series;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class C4SeriesExtractorTest extends TestCase {

	private final AtomFeedBuilder seriesFeed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares-series-3.atom"));
	
	@SuppressWarnings("unchecked")
	public void testParsingASeries() throws Exception {
		
		Series series = new C4SeriesExtractor().extract(seriesFeed.build());
		
		assertThat(series.getCanonicalUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-3"));
		assertThat(series.getCurie(), is("c4:ramsays-kitchen-nightmares-series-3"));
		assertThat(series.getAliases(), is((Set<String>) ImmutableSet.of("tag:www.channel4.com,2009:/programmes/ramsays-kitchen-nightmares/episode-guide/series-3")));

		assertThat(series.getTitle(), is("Series 3 - Ramsay's Kitchen Nightmares"));
		assertThat(series.getDescription(), startsWith("Multi Michelin-starred chef Gordon Ramsay"));
		
		List<Episode> episodes = (List) series.getItems();
		
		Episode firstEpisode = episodes.get(0);
		
		assertThat(firstEpisode.getCanonicalUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-3/episode-1"));
		assertThat(firstEpisode.getCurie(), is("c4:ramsays-kitchen-nightmares-series-3-episode-1"));

		assertThat(series.getSeriesNumber(), is(3));
		
		assertThat(firstEpisode.getSeriesNumber(), is(3));
		assertThat(firstEpisode.getEpisodeNumber(), is(1));


		// since this is not a /4od feed there should be no On Demand entries
		assertThat(firstEpisode.getVersions(), is((Set) ImmutableSet.of()));

		// The outer adapter will notice that this is the same as the brand title and will replace it with the series and episode number
		assertThat(firstEpisode.getTitle(), is("Ramsay's Kitchen Nightmares"));
	}
}
