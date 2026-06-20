package com.apptasticsoftware.rssreader.module.georss;

import com.apptasticsoftware.rssreader.module.georss.internal.GeoRssItemImpl;
import com.apptasticsoftware.rssreader.util.Default;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class GeoFenceFilterTest {

    @Test
    void haversineDistance_samePoint() {
        var coord = new Coordinate(39.9042, 116.4074);
        double distance = GeoFenceFilter.haversineDistance(coord, coord);
        assertThat(distance).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void haversineDistance_beijingToShanghai() {
        var beijing = new Coordinate(39.9042, 116.4074);
        var shanghai = new Coordinate(31.2304, 121.4737);
        double distance = GeoFenceFilter.haversineDistance(beijing, shanghai);
        assertThat(distance).isCloseTo(1068.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    void haversineDistance_beijingToGuangzhou() {
        var beijing = new Coordinate(39.9042, 116.4074);
        var guangzhou = new Coordinate(23.1291, 113.2644);
        double distance = GeoFenceFilter.haversineDistance(beijing, guangzhou);
        assertThat(distance).isCloseTo(1884.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    void haversineDistance_knownDistance_londonToParis() {
        var london = new Coordinate(51.5074, -0.1278);
        var paris = new Coordinate(48.8566, 2.3522);
        double distance = GeoFenceFilter.haversineDistance(london, paris);
        assertThat(distance).isCloseTo(343.0, org.assertj.core.data.Offset.offset(5.0));
    }

    @Test
    void constructor_nullCenter_throwsException() {
        assertThatThrownBy(() -> new GeoFenceFilter(null, 10.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Center coordinate must not be null");
    }

    @Test
    void constructor_negativeRadius_throwsException() {
        var center = new Coordinate(39.9042, 116.4074);
        assertThatThrownBy(() -> new GeoFenceFilter(center, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Radius must not be negative");
    }

    @Test
    void filter_nullStream_throwsException() {
        var filter = new GeoFenceFilter(new Coordinate(0, 0), 10.0);
        assertThatThrownBy(() -> filter.filter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Items stream must not be null");
    }

    @Test
    void filter_itemsWithPointCoordinate_withinRadius() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = createItemWithPoint("39.9042 116.4074");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertThat(result.get(0).getTitle()).hasValue("Test");
    }

    @Test
    void filter_itemsWithPointCoordinate_outsideRadius() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = createItemWithPoint("31.2304 121.4737");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertTrue(result.isEmpty());
    }

    @Test
    void filter_itemsWithoutCoordinate_excluded() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("NoLocation");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertTrue(result.isEmpty());
    }

    @Test
    void filter_itemsWithLineCoordinate_withinRadius() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("RouteNearBeijing");
        item.setGeoRssLine("39.9 116.4 40.0 116.5");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertEquals(1, result.size());
    }

    @Test
    void filter_itemsWithPolygonCoordinate_withinRadius() {
        var center = new Coordinate(31.2304, 121.4737);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("AreaNearShanghai");
        item.setGeoRssPolygon("31.2 121.4 31.3 121.5 31.2 121.5 31.2 121.4");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertEquals(1, result.size());
    }

    @Test
    void filter_itemsWithBoxCoordinate_withinRadius() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("BoxNearBeijing");
        item.setGeoRssBox("39.85 116.35 39.95 116.50");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertEquals(1, result.size());
    }

    @Test
    void filter_mixedItems_correctFiltering() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 200.0);

        var beijing = createItemWithPoint("39.9042 116.4074");
        beijing.setTitle("Beijing");

        var shanghai = createItemWithPoint("31.2304 121.4737");
        shanghai.setTitle("Shanghai");

        var noLocation = new GeoRssItemImpl(Default.getDateTimeParser());
        noLocation.setTitle("NoLocation");

        var result = filter.filter(Stream.of(beijing, shanghai, noLocation)).collect(Collectors.toList());

        assertEquals(1, result.size());
        assertThat(result.get(0).getTitle()).hasValue("Beijing");
    }

    @Test
    void filter_zeroRadius_onlyExactMatch() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 0.0);

        var exact = createItemWithPoint("39.9042 116.4074");
        var nearby = createItemWithPoint("39.9050 116.4080");

        var result = filter.filter(Stream.of(exact, nearby)).collect(Collectors.toList());

        assertEquals(1, result.size());
    }

    @Test
    void filter_integrationWithGeoRssFeedReader() {
        var reader = new GeoRssFeedReader();
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 200.0);

        InputStream feedStream = getClass().getClassLoader().getResourceAsStream("module/georss/geofence.xml");
        assertThat(feedStream).isNotNull();

        var items = reader.read(feedStream);
        var filtered = filter.filter(items).collect(Collectors.toList());

        assertThat(filtered.stream().map(i -> i.getTitle().orElse("")))
                .contains("Beijing", "Route near Beijing")
                .doesNotContain("Shanghai", "Guangzhou", "NoLocation");
    }

    @Test
    void isCoordinateWithinFence_within() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        assertTrue(filter.isCoordinateWithinFence(new Coordinate(39.91, 116.41)));
    }

    @Test
    void isCoordinateWithinFence_outside() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 50.0);

        assertFalse(filter.isCoordinateWithinFence(new Coordinate(31.2304, 121.4737)));
    }

    @Test
    void getCenter() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 100.0);

        assertThat(filter.getCenter()).isEqualTo(center);
    }

    @Test
    void getRadiusKm() {
        var filter = new GeoFenceFilter(new Coordinate(0, 0), 123.45);

        assertThat(filter.getRadiusKm()).isEqualTo(123.45);
    }

    @Test
    void filter_linePartiallyWithinFence() {
        var center = new Coordinate(39.9042, 116.4074);
        var filter = new GeoFenceFilter(center, 100.0);

        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("RouteFromBeijingToFar");
        item.setGeoRssLine("39.9 116.4 31.2304 121.4737");
        var result = filter.filter(Stream.of(item)).collect(Collectors.toList());

        assertEquals(1, result.size());
    }

    private GeoRssItemImpl createItemWithPoint(String point) {
        var item = new GeoRssItemImpl(Default.getDateTimeParser());
        item.setTitle("Test");
        item.setGeoRssPoint(point);
        return item;
    }
}
