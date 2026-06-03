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

package net.dv8tion.jda.internal.utils.collections;

import gnu.trove.function.TObjectFunction;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;
import org.agrona.collections.Hashing;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongHashSet;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/*
 * Safety-compatible Trove adapter for this private MusicBot fork.
 * Current standalone benchmark does not prove this is faster than TLongObjectHashMap.
 */
public class AgronaLongObjectMap<V> implements TLongObjectMap<V> {
    private final Long2ObjectHashMap<V> delegate;

    public AgronaLongObjectMap() {
        this.delegate = new Long2ObjectHashMap<>();
    }

    public AgronaLongObjectMap(int initialCapacity) {
        this.delegate = new Long2ObjectHashMap<>(initialCapacity, Hashing.DEFAULT_LOAD_FACTOR);
    }

    public AgronaLongObjectMap(TLongObjectMap<? extends V> source) {
        this(source.size());
        putAll(source);
    }

    @Override
    public long getNoEntryKey() {
        return 0L;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(long key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(long key) {
        return delegate.get(key);
    }

    @Override
    public V put(long key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public V putIfAbsent(long key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public V remove(long key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends Long, ? extends V> map) {
        map.forEach((key, value) -> delegate.put(key.longValue(), value));
    }

    @Override
    public void putAll(TLongObjectMap<? extends V> map) {
        if (map == this) {
            return;
        }
        for (TLongObjectIterator<? extends V> it = map.iterator(); it.hasNext(); ) {
            it.advance();
            delegate.put(it.key(), it.value());
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public TLongSet keySet() {
        return new LiveKeySet();
    }

    @Override
    public long[] keys() {
        return keySet().toArray();
    }

    @Override
    public long[] keys(long[] array) {
        return keySet().toArray(array);
    }

    @Override
    public Collection<V> valueCollection() {
        return new LiveValueCollection();
    }

    @Override
    public Object[] values() {
        return delegate.values().toArray();
    }

    @Override
    public V[] values(V[] array) {
        return delegate.values().toArray(array);
    }

    @Override
    public TLongObjectIterator<V> iterator() {
        return new EntryIterator<>(delegate.entrySet().iterator());
    }

    @Override
    public boolean forEachKey(TLongProcedure procedure) {
        for (long key : keys()) {
            if (!procedure.execute(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean forEachValue(TObjectProcedure<? super V> procedure) {
        for (V value : valueCollection()) {
            if (!procedure.execute(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean forEachEntry(TLongObjectProcedure<? super V> procedure) {
        TLongObjectIterator<V> it = iterator();
        while (it.hasNext()) {
            it.advance();
            if (!procedure.execute(it.key(), it.value())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void transformValues(TObjectFunction<V, V> function) {
        delegate.replaceAllLong((key, value) -> function.execute(value));
    }

    @Override
    public boolean retainEntries(TLongObjectProcedure<? super V> procedure) {
        boolean changed = false;
        Iterator<Map.Entry<Long, V>> iterator = delegate.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, V> entry = iterator.next();
            if (!procedure.execute(entry.getKey(), entry.getValue())) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AgronaLongObjectMap<?>) {
            return delegate.equals(((AgronaLongObjectMap<?>) obj).delegate);
        }
        if (obj instanceof TLongObjectMap<?>) {
            TLongObjectMap<?> other = (TLongObjectMap<?>) obj;
            if (size() != other.size()) {
                return false;
            }
            for (TLongObjectIterator<V> it = iterator(); it.hasNext(); ) {
                it.advance();
                if (!Objects.equals(it.value(), other.get(it.key()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    private static final class EntryIterator<V> implements TLongObjectIterator<V> {
        private final Iterator<Map.Entry<Long, V>> iterator;
        private Map.Entry<Long, V> current;

        private EntryIterator(Iterator<Map.Entry<Long, V>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public void advance() {
            current = iterator.next();
        }

        @Override
        public long key() {
            return current.getKey();
        }

        @Override
        public V value() {
            return current.getValue();
        }

        @Override
        public V setValue(V value) {
            return current.setValue(value);
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private final class LiveKeySet implements TLongSet {
        @Override
        public long getNoEntryValue() {
            return getNoEntryKey();
        }

        @Override
        public int size() {
            return AgronaLongObjectMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return AgronaLongObjectMap.this.isEmpty();
        }

        @Override
        public boolean contains(long value) {
            return AgronaLongObjectMap.this.containsKey(value);
        }

        @Override
        public TLongIterator iterator() {
            return new KeyIterator(delegate.entrySet().iterator());
        }

        @Override
        public long[] toArray() {
            long[] values = new long[size()];
            int i = 0;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                values[i++] = it.next();
            }
            return values;
        }

        @Override
        public long[] toArray(long[] array) {
            long[] values = array.length >= size() ? array : Arrays.copyOf(array, size());
            int i = 0;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                values[i++] = it.next();
            }
            if (values.length > i) {
                values[i] = getNoEntryValue();
            }
            return values;
        }

        @Override
        public boolean add(long value) {
            throw new UnsupportedOperationException("keySet().add is not supported for a map-backed Trove key view");
        }

        @Override
        public boolean remove(long value) {
            if (!contains(value)) {
                return false;
            }
            AgronaLongObjectMap.this.remove(value);
            return true;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object value : collection) {
                if (!(value instanceof Long) || !contains((Long) value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(gnu.trove.TLongCollection collection) {
            for (TLongIterator it = collection.iterator(); it.hasNext(); ) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(long[] array) {
            for (long value : array) {
                if (!contains(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Long> collection) {
            throw new UnsupportedOperationException("keySet().addAll is not supported for a map-backed Trove key view");
        }

        @Override
        public boolean addAll(gnu.trove.TLongCollection collection) {
            throw new UnsupportedOperationException("keySet().addAll is not supported for a map-backed Trove key view");
        }

        @Override
        public boolean addAll(long[] array) {
            throw new UnsupportedOperationException("keySet().addAll is not supported for a map-backed Trove key view");
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean changed = false;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                long value = it.next();
                if (!collection.contains(value)) {
                    it.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(gnu.trove.TLongCollection collection) {
            boolean changed = false;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                long value = it.next();
                if (!collection.contains(value)) {
                    it.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(long[] array) {
            LongHashSet keep = new LongHashSet(Math.max(1, array.length));
            for (long value : array) {
                keep.add(value);
            }
            return retainAll(new AgronaLongSet(keep));
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean changed = false;
            for (Object value : collection) {
                if (value instanceof Long) {
                    changed |= remove((Long) value);
                }
            }
            return changed;
        }

        @Override
        public boolean removeAll(gnu.trove.TLongCollection collection) {
            boolean changed = false;
            for (TLongIterator it = collection.iterator(); it.hasNext(); ) {
                changed |= remove(it.next());
            }
            return changed;
        }

        @Override
        public boolean removeAll(long[] array) {
            boolean changed = false;
            for (long value : array) {
                changed |= remove(value);
            }
            return changed;
        }

        @Override
        public void clear() {
            AgronaLongObjectMap.this.clear();
        }

        @Override
        public boolean forEach(TLongProcedure procedure) {
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                if (!procedure.execute(it.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TLongSet) {
                TLongSet other = (TLongSet) obj;
                return size() == other.size() && containsAll(other);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                hash += Long.hashCode(it.next());
            }
            return hash;
        }
    }

    private static final class KeyIterator implements TLongIterator {
        private final Iterator<? extends Map.Entry<Long, ?>> iterator;
        private Map.Entry<Long, ?> current;

        private KeyIterator(Iterator<? extends Map.Entry<Long, ?>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public long next() {
            current = iterator.next();
            return current.getKey();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException("next() must be called before remove()");
            }
            iterator.remove();
            current = null;
        }
    }

    private final class LiveValueCollection extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator<>(delegate.entrySet().iterator());
        }

        @Override
        public int size() {
            return AgronaLongObjectMap.this.size();
        }

        @Override
        public boolean contains(Object value) {
            return AgronaLongObjectMap.this.containsValue(value);
        }

        @Override
        public boolean remove(Object value) {
            Iterator<Map.Entry<Long, V>> iterator = delegate.entrySet().iterator();
            while (iterator.hasNext()) {
                if (Objects.equals(iterator.next().getValue(), value)) {
                    iterator.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            AgronaLongObjectMap.this.clear();
        }
    }

    private static final class ValueIterator<V> implements Iterator<V> {
        private final Iterator<Map.Entry<Long, V>> iterator;
        private Map.Entry<Long, V> current;

        private ValueIterator(Iterator<Map.Entry<Long, V>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            current = iterator.next();
            return current.getValue();
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException("next() must be called before remove()");
            }
            iterator.remove();
            current = null;
        }
    }

    public static final class AgronaLongSet implements TLongSet {
        private final LongHashSet delegate;

        public AgronaLongSet() {
            this.delegate = new LongHashSet();
        }

        public AgronaLongSet(LongHashSet delegate) {
            this.delegate = delegate;
        }

        @Override
        public long getNoEntryValue() {
            return 0L;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(long value) {
            return delegate.contains(value);
        }

        @Override
        public TLongIterator iterator() {
            return new SetIterator(delegate.iterator());
        }

        @Override
        public long[] toArray() {
            long[] values = new long[delegate.size()];
            int i = 0;
            for (long value : delegate) {
                values[i++] = value;
            }
            return values;
        }

        @Override
        public long[] toArray(long[] array) {
            long[] values = array.length >= delegate.size() ? array : Arrays.copyOf(array, delegate.size());
            int i = 0;
            for (long value : delegate) {
                values[i++] = value;
            }
            if (values.length > i) {
                values[i] = getNoEntryValue();
            }
            return values;
        }

        @Override
        public boolean add(long value) {
            return delegate.add(value);
        }

        @Override
        public boolean remove(long value) {
            return delegate.remove(value);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return delegate.containsAll(collection);
        }

        @Override
        public boolean containsAll(gnu.trove.TLongCollection collection) {
            for (TLongIterator it = collection.iterator(); it.hasNext(); ) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(long[] array) {
            for (long value : array) {
                if (!contains(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Long> collection) {
            return delegate.addAll(collection);
        }

        @Override
        public boolean addAll(gnu.trove.TLongCollection collection) {
            boolean changed = false;
            for (TLongIterator it = collection.iterator(); it.hasNext(); ) {
                changed |= add(it.next());
            }
            return changed;
        }

        @Override
        public boolean addAll(long[] array) {
            boolean changed = false;
            for (long value : array) {
                changed |= add(value);
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return delegate.retainAll(collection);
        }

        @Override
        public boolean retainAll(gnu.trove.TLongCollection collection) {
            boolean changed = false;
            for (TLongIterator it = iterator(); it.hasNext(); ) {
                long value = it.next();
                if (!collection.contains(value)) {
                    it.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(long[] array) {
            LongHashSet keep = new LongHashSet(Math.max(1, array.length));
            for (long value : array) {
                keep.add(value);
            }
            return delegate.retainAll(keep);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return delegate.removeAll(collection);
        }

        @Override
        public boolean removeAll(gnu.trove.TLongCollection collection) {
            boolean changed = false;
            for (TLongIterator it = collection.iterator(); it.hasNext(); ) {
                changed |= remove(it.next());
            }
            return changed;
        }

        @Override
        public boolean removeAll(long[] array) {
            boolean changed = false;
            for (long value : array) {
                changed |= remove(value);
            }
            return changed;
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public boolean forEach(TLongProcedure procedure) {
            for (long value : delegate) {
                if (!procedure.execute(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AgronaLongSet) {
                return delegate.equals(((AgronaLongSet) obj).delegate);
            }
            if (obj instanceof TLongSet) {
                TLongSet other = (TLongSet) obj;
                return size() == other.size() && containsAll(other);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        private static final class SetIterator implements TLongIterator {
            private final Iterator<Long> iterator;

            private SetIterator(Iterator<Long> iterator) {
                this.iterator = iterator;
            }

            @Override
            public long next() {
                return iterator.next();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }
    }
}
