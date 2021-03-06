package org.atlasapi.remotesite.itunes.epf;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.itunes.epf.model.EpfPricing;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.currency.Price;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;

import com.google.api.client.repackaged.com.google.common.base.Objects;

public class ItunesPricingLocationExtractor implements ContentExtractor<ItunesEpfPricingSource, Maybe<Location>> {

    @Override
    public Maybe<Location> extract(ItunesEpfPricingSource source) {
        String countryCode = iso3Code(source.getCountry());
        Integer storefrontId = source.getCountryCodes().get(countryCode);
        Integer extractedStorefrontId = source.getRow().get(EpfPricing.STOREFRONT_ID);

        if (!Objects.equal(extractedStorefrontId, storefrontId)) {
            return Maybe.nothing();
        }

        BigDecimal sdPrice = source.getRow().get(EpfPricing.SD_PRICE);
        if (sdPrice == null) {
            return Maybe.nothing();
        }
        Location location = new Location();
        location.setTransportType(TransportType.APPLICATION);
        location.setTransportSubType(TransportSubType.ITUNES);
        location.setEmbedId(source.getRow().get(EpfPricing.VIDEO_ID).toString());

        Policy policy = new Policy();
        policy.addAvailableCountry(source.getCountry());
        policy.setRevenueContract(RevenueContract.PAY_TO_BUY);

        Currency currency = Currency.getInstance(new Locale("en", source.getCountry().code()));
        policy.setPrice(new Price(currency, sdPrice.movePointRight(currency.getDefaultFractionDigits()).intValue()));

        location.setPolicy(policy);

        return Maybe.just(location);
    }

    private String iso3Code(Country country) {
        return new Locale(Locale.ENGLISH.getLanguage(), country.code()).getISO3Country().toLowerCase();
    }

}
