/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.remotesite.bliptv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.atlasapi.remotesite.html.HtmlDescriptionOfItem;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;

import com.metabroadcast.common.http.SimpleHttpClient;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
@RunWith(JMock.class)
public class BlipTvClientTest extends TestCase {

	String URI = "/uri";

    private final Mockery context = new Mockery();
	private SimpleHttpClient httpClient = context.mock(SimpleHttpClient.class);

    @Test
	public void testTheClient() throws Exception {
		
		context.checking(new Expectations() {{ 
			one(httpClient).getContentsOf(URI); will(returnValue(itemHtml()));
		}});
		
		BlipTvClient client = new BlipTvClient(httpClient);
		
		HtmlDescriptionOfItem item = client.get(URI);
		
		assertThat(item.getTitle(), startsWith("Temp Life 17: City Girls | The Temp Life"));
		assertThat(item.getDescription(), startsWith("After abruptly canning her assistant"));
		assertThat(item.getThumbnail(), is("http://a.images.blip.tv/Spheriontheweb-TempLife17CityGirls496.jpg"));
		assertThat(item.getVideoSource(), is("http://e.blip.tv/scripts/flash/showplayer.swf?file=http://blip.tv/rss/flash/2125416"));
		
		assertThat(item.getLocationUris().size(), is(6));
		assertThat(item.getLocationUris().get(0), is("http://blip.tv/file/get/Spheriontheweb-TempLife17CityGirls930.mp4"));
	}
	
	protected String itemHtml() throws IOException {
		return IOUtils.toString(new ClassPathResource("blip-tv-item.html").getInputStream());
	}
}
