package org.atlasapi.remotesite.bbc.nitro.extract;

import com.google.api.client.util.Strings;
import com.google.common.base.Optional;
import com.metabroadcast.atlas.glycerin.model.Brand;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.ContentExtractor;

public class NitroPersonExtractor implements
        ContentExtractor<Brand.Contributions.Contribution, Optional<Person>> {

    @Override
    public Optional<Person> extract(Brand.Contributions.Contribution contribution) {
        Brand.Contributions.Contribution.Contributor.Name contributorName = contribution
                .getContributor().getName();

        if (contributorName == null) {
            return Optional.absent();
        }

        Person person = new Person();
        person.setCanonicalUri(NitroUtil.uriFor(contribution));
        person.setCurie(NitroUtil.curieFor(contribution));
        person.setPublisher(Publisher.BBC_NITRO);

        if (!Strings.isNullOrEmpty(contributorName.getPresentation())) {
            person.setGivenName(contributorName.getPresentation());
        } else {
            person.setGivenName(contributorName.getGiven());
            person.setFamilyName(contributorName.getFamily());
        }
        return Optional.of(person);
    }
}
