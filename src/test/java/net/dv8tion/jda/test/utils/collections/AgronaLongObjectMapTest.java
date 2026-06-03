/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.test.utils.collections;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.utils.collections.AgronaLongObjectMap;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AgronaLongObjectMapTest {
    @Test
    void basicOperationsMatchTrove() {
        TLongObjectMap<String> trove = new TLongObjectHashMap<>();
        TLongObjectMap<String> agrona = new AgronaLongObjectMap<>();

        assertThat(agrona.put(1, "one")).isEqualTo(trove.put(1, "one"));
        assertThat(agrona.put(2, "two")).isEqualTo(trove.put(2, "two"));
        assertThat(agrona.put(1, "first")).isEqualTo(trove.put(1, "first"));
        assertEqualContent(agrona, trove);

        assertThat(agrona.get(1)).isEqualTo(trove.get(1));
        assertThat(agrona.get(999)).isEqualTo(trove.get(999));
        assertThat(agrona.containsKey(2)).isEqualTo(trove.containsKey(2));
        assertThat(agrona.containsValue("first")).isEqualTo(trove.containsValue("first"));
        assertThat(agrona.size()).isEqualTo(trove.size());

        assertThat(agrona.remove(2)).isEqualTo(trove.remove(2));
        assertEqualContent(agrona, trove);

        agrona.clear();
        trove.clear();
        assertEqualContent(agrona, trove);
    }

    @Test
    void putAllMatchesTrove() {
        Map<Long, String> source = new HashMap<>();
        source.put(10L, "ten");
        source.put(20L, "twenty");

        TLongObjectMap<String> trove = new TLongObjectHashMap<>();
        TLongObjectMap<String> agrona = new AgronaLongObjectMap<>();
        trove.putAll(source);
        agrona.putAll(source);
        assertEqualContent(agrona, trove);

        TLongObjectMap<String> sourceMap = new TLongObjectHashMap<>();
        sourceMap.put(30, "thirty");
        sourceMap.put(40, "forty");
        trove.putAll(sourceMap);
        agrona.putAll(sourceMap);
        assertEqualContent(agrona, trove);

        agrona.putAll(agrona);
        assertEqualContent(agrona, trove);
    }

    @Test
    void iteratorRemoveRemovesFromBackingMap() {
        TLongObjectMap<String> map = sampleAgrona();

        TLongObjectIterator<String> iterator = map.iterator();
        while (iterator.hasNext()) {
            iterator.advance();
            if (iterator.key() == 2) {
                iterator.remove();
            }
        }

        assertThat(map.containsKey(2)).isFalse();
        assertThat(map.size()).isEqualTo(2);
    }

    @Test
    void keySetIsLiveAndMutableLikeTrove() {
        TLongObjectMap<String> trove = sampleTrove();
        TLongObjectMap<String> agrona = sampleAgrona();

        assertThat(agrona.keySet().contains(1)).isEqualTo(trove.keySet().contains(1));
        assertThat(agrona.keySet().toArray())
                .containsExactlyInAnyOrder(trove.keySet().toArray());

        assertThat(agrona.keySet().remove(2)).isEqualTo(trove.keySet().remove(2));
        assertEqualContent(agrona, trove);

        assertThat(agrona.keySet().removeAll(new long[] {1, 99}))
                .isEqualTo(trove.keySet().removeAll(new long[] {1, 99}));
        assertEqualContent(agrona, trove);

        trove = sampleTrove();
        agrona = sampleAgrona();
        assertThat(agrona.keySet().retainAll(new long[] {1, 3}))
                .isEqualTo(trove.keySet().retainAll(new long[] {1, 3}));
        assertEqualContent(agrona, trove);

        TLongIterator iterator = agrona.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == 1) {
                iterator.remove();
            }
        }
        assertThat(agrona.containsKey(1)).isFalse();

        TLongObjectMap<String> unsupportedAddMap = agrona;
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unsupportedAddMap.keySet().add(10))
                .withMessageContaining("map-backed");
    }

    @Test
    void valueCollectionIsLiveAndMutableLikeTrove() {
        TLongObjectMap<String> trove = sampleTrove();
        TLongObjectMap<String> agrona = sampleAgrona();

        assertThat(agrona.valueCollection()).containsExactlyInAnyOrderElementsOf(trove.valueCollection());

        assertThat(agrona.valueCollection().remove("two"))
                .isEqualTo(trove.valueCollection().remove("two"));
        assertEqualContent(agrona, trove);

        trove = sampleTrove();
        agrona = sampleAgrona();
        assertThat(agrona.valueCollection().removeAll(java.util.List.of("one", "missing")))
                .isEqualTo(trove.valueCollection().removeAll(java.util.List.of("one", "missing")));
        assertEqualContent(agrona, trove);

        trove = sampleTrove();
        agrona = sampleAgrona();
        assertThat(agrona.valueCollection().retainAll(java.util.List.of("one", "three")))
                .isEqualTo(trove.valueCollection().retainAll(java.util.List.of("one", "three")));
        assertEqualContent(agrona, trove);

        Collection<String> values = agrona.valueCollection();
        java.util.Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            if ("one".equals(iterator.next())) {
                iterator.remove();
            }
        }
        assertThat(agrona.containsKey(1)).isFalse();
    }

    @Test
    void retainEntriesAndTransformValuesMatchTrove() {
        TLongObjectMap<String> trove = sampleTrove();
        TLongObjectMap<String> agrona = sampleAgrona();

        assertThat(agrona.retainEntries((key, value) -> key != 2))
                .isEqualTo(trove.retainEntries((key, value) -> key != 2));
        assertEqualContent(agrona, trove);

        agrona.transformValues(value -> value + "!");
        trove.transformValues(value -> value + "!");
        assertEqualContent(agrona, trove);
    }

    @Test
    void miscUtilNewLongMapSupportsBackedViews() {
        TLongObjectMap<String> map = MiscUtil.newLongMap();
        map.put(1, "one");
        map.put(2, "two");

        map.keySet().remove(1);
        map.valueCollection().remove("two");

        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void simplePerformanceMeasurement() {
        int size = 100_000;

        MapTiming trove = timeMap(new TLongObjectHashMap<>(), size);
        MapTiming agrona = timeMap(new AgronaLongObjectMap<>(), size);

        System.out.printf(
                "TLongObjectHashMap put=%dns get=%dns remove=%dns iterate=%dns%n",
                trove.putNanos, trove.getNanos, trove.removeNanos, trove.iterateNanos);
        System.out.printf(
                "AgronaLongObjectMap put=%dns get=%dns remove=%dns iterate=%dns%n",
                agrona.putNanos, agrona.getNanos, agrona.removeNanos, agrona.iterateNanos);

        assertThat(trove.sum).isEqualTo(agrona.sum);
    }

    private static TLongObjectMap<String> sampleAgrona() {
        TLongObjectMap<String> map = new AgronaLongObjectMap<>();
        populate(map);
        return map;
    }

    private static TLongObjectMap<String> sampleTrove() {
        TLongObjectMap<String> map = new TLongObjectHashMap<>();
        populate(map);
        return map;
    }

    private static void populate(TLongObjectMap<String> map) {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
    }

    private static void assertEqualContent(TLongObjectMap<String> actual, TLongObjectMap<String> expected) {
        assertThat(actual.size()).isEqualTo(expected.size());
        for (TLongObjectIterator<String> it = expected.iterator(); it.hasNext(); ) {
            it.advance();
            assertThat(actual.get(it.key())).isEqualTo(it.value());
        }
        assertThat(actual.keySet()).isEqualTo(new TLongHashSet(expected.keys()));
        assertThat(actual.valueCollection()).containsExactlyInAnyOrderElementsOf(expected.valueCollection());
    }

    private static MapTiming timeMap(TLongObjectMap<String> map, int size) {
        long start = System.nanoTime();
        for (int i = 0; i < size; i++) {
            map.put(i, "value-" + i);
        }
        long putNanos = System.nanoTime() - start;

        long sum = 0;
        start = System.nanoTime();
        for (int i = 0; i < size; i++) {
            String value = map.get(i);
            if (value != null) {
                sum += value.length();
            }
        }
        long getNanos = System.nanoTime() - start;

        start = System.nanoTime();
        for (TLongObjectIterator<String> it = map.iterator(); it.hasNext(); ) {
            it.advance();
            sum += it.key();
        }
        long iterateNanos = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < size; i += 2) {
            map.remove(i);
        }
        long removeNanos = System.nanoTime() - start;

        return new MapTiming(putNanos, getNanos, removeNanos, iterateNanos, sum);
    }

    private record MapTiming(long putNanos, long getNanos, long removeNanos, long iterateNanos, long sum) {}
}
