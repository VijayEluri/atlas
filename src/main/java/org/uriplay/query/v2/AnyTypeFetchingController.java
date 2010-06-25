/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.query.v2;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.uriplay.media.entity.Brand;
import org.uriplay.media.entity.Content;
import org.uriplay.media.entity.Item;
import org.uriplay.media.entity.Playlist;
import org.uriplay.persistence.content.query.KnownTypeQueryExecutor;
import org.uriplay.persistence.servlet.ContentNotFoundException;
import org.uriplay.persistence.servlet.RequestNs;
import org.uriplay.query.content.parser.QueryStringBackedQueryBuilder;
import org.uriplay.query.content.parser.WebProfileDefaultQueryAttributesSetter;
import org.uriplay.remotesite.FetchException;
import org.uriplay.remotesite.NoMatchingAdapterException;

import com.google.common.collect.Lists;

/**
 * Controller to handle the query interface to UriPlay.
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 * @author Lee Denison (lee@metabroadcast.com)
 */
@Controller
public class AnyTypeFetchingController {

	private static final String VIEW = "uriplayModel";
	
	private final KnownTypeQueryExecutor executor;
	private final QueryStringBackedQueryBuilder builder = new QueryStringBackedQueryBuilder(new WebProfileDefaultQueryAttributesSetter());

	public AnyTypeFetchingController(KnownTypeQueryExecutor queryExecutor) {
		this.executor = queryExecutor;
	}

	@RequestMapping(method=RequestMethod.GET)
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		try {
			List<Content> found = Lists.newArrayList();
			
			found.addAll(executor.executeBrandQuery(builder.build(request, Brand.class)));
			found.addAll(executor.executeItemQuery(builder.build(request, Item.class)));
			found.addAll(executor.executePlaylistQuery(builder.build(request, Playlist.class)));
		
			return new ModelAndView(VIEW, RequestNs.GRAPH, found);
			
		} catch (NoMatchingAdapterException nmae) {
			throw new ContentNotFoundException(nmae);
		} catch (FetchException fe) {
			throw new ContentNotFoundException(fe);
		} 
	}
}
