package org.atlasapi.remotesite.bbc.nitro;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.metabroadcast.atlas.glycerin.Glycerin;
import com.metabroadcast.atlas.glycerin.GlycerinException;
import com.metabroadcast.atlas.glycerin.GlycerinResponse;
import com.metabroadcast.atlas.glycerin.model.Programme;
import com.metabroadcast.atlas.glycerin.queries.ProgrammesQuery;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

/**
 * Used to paginate over Nitro Programmes to reduce the heap overhead.
 *
 * Takes an iterable of {@link ProgrammesQuery} which is used to get
 * the an iterable of Programmes per page.
 * Used as part of {@link OffScheduleContentIngestTask}
 */
public class PaginatedProgrammeRequest implements Iterable<List<Programme>> {

    private final Iterable<ProgrammesQuery> programmeQueries;
    private final Glycerin client;

    public PaginatedProgrammeRequest(Glycerin client, Iterable<ProgrammesQuery> queries) {
        this.client = client;
        this.programmeQueries = queries;
    }

    @Override
    public Iterator<List<Programme>> iterator() {
        return new ProgrammeIterator(client, programmeQueries);
    }

    private static class ProgrammeIterator implements Iterator<List<Programme>> {

        private final Glycerin client;
        private final Iterator<ProgrammesQuery> programmeQueries;
        private GlycerinResponse<Programme> currentResponse;

        public ProgrammeIterator(Glycerin client, Iterable<ProgrammesQuery> programmeQueries) {
            this.client = client;
            this.programmeQueries = programmeQueries.iterator();
        }

        /**
         * Checks if current page of programmes is present so that next method can be used
         * to retrieve the next page of Programmes.
         * At first will get the first page as a list, then get next page as a list, if there are any.
         * If not will get next Programme object and repeat previous steps.
         * This has been done to decrease the heap overhead when querying Nitro.
         * @return returns true if the current Programme is present, else returns false.
         */
        @Override
        public boolean hasNext() {
                if (currentResponse == null) { // Getting the first page.
                    return executeRequest();
                } else {
                    return currentResponse.hasNext();
                }
        }

        private boolean executeRequest() {
            try {
                if (programmeQueries.hasNext()) {
                    currentResponse = client.execute(programmeQueries.next());
                    return true;
                } else {
                    return false;
                }
            } catch (GlycerinException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public List<Programme> next() {
            try {
                if (currentResponse == null) {
                    boolean gotSomething = executeRequest();
                    if (!gotSomething) {
                        throw new NoSuchElementException();
                    }
                }

                ImmutableList<Programme> programmes = currentResponse.getResults();
                if (currentResponse.hasNext()) {
                    currentResponse = currentResponse.getNext();
                } else {
                    currentResponse = null;
                }
                return programmes;
            } catch (GlycerinException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}