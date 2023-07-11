package xyz.fxcilities.core.collections.expiringmap;

import xyz.fxcilities.core.Checks;
import xyz.fxcilities.core.collections.expiringmap.internal.NamedThreadFactory;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe map that expires entries. Optional features include expiration policies, variable
 * entry expiration, lazy entry loading, and expiration listeners.
 *
 * <p>Entries are tracked by expiration time and expired by a single thread.
 *
 * <p>Expiration listeners are called synchronously as entries are expired and block write
 * operations to the map until they completed. Asynchronous expiration listeners are called on a
 * separate thread pool and do not block map operations.
 *
 * <p>When variable expiration is disabled (default), put/remove operations have a time complexity
 * <i>O(1)</i>. When variable expiration is enabled, put/remove operations have time complexity of
 * <i>O(log n)</i>.
 *
 * <p>Example usages:
 *
 * <pre>{@code
 * Map<String, Integer> map = ExpiringMap.create();
 * Map<String, Integer> map = ExpiringMap.builder().expiration(30, TimeUnit.SECONDS).build();
 * Map<String, Connection> map = ExpiringMap.builder()
 *   .expiration(10, TimeUnit.MINUTES)
 *   .entryLoader(new EntryLoader<String, Connection>() {
 *     public Connection load(String address) {
 *       return new Connection(address);
 *     }
 *   })
 *   .expirationListener(new ExpirationListener<String, Connection>() {
 *     public void expired(String key, Connection connection) {
 *       connection.close();
 *     }
 *   })
 *   .build();
 * }</pre>
 *
 * @author Jonathan Halterman
 * @param <K> Key type
 * @param <V> Value type
 */
public class ExpiringMap<K, V> implements ConcurrentMap<K, V> {
    static volatile ScheduledExecutorService EXPIRER;
    static volatile ThreadPoolExecutor LISTENER_SERVICE;
    static ThreadFactory THREAD_FACTORY;

    List<ExpirationListener<K, V>> expirationListeners;
    List<ExpirationListener<K, V>> asyncExpirationListeners;
    private AtomicLong expirationNanos;
    private int maxSize;
    private final AtomicReference<ExpirationPolicy> expirationPolicy;
    private final EntryLoader<? super K, ? extends V> entryLoader;
    private final ExpiringEntryLoader<? super K, ? extends V> expiringEntryLoader;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    /** Guarded by "readWriteLock" */
    private final EntryMap<K, V> entries;

    private final boolean variableExpiration;

    /**
     * Sets the {@link ThreadFactory} that is used to create expiration and listener callback
     * threads for all ExpiringMap instances.
     *
     * @param threadFactory
     * @throws NullPointerException if {@code threadFactory} is null
     */
    public static void setThreadFactory(ThreadFactory threadFactory) {
        THREAD_FACTORY = (ThreadFactory) Checks.nonNull(threadFactory, "threadFactory");
    }

    /**
     * Creates a new instance of ExpiringMap.
     *
     * @param builder The map builder
     */
    private ExpiringMap(final Builder<K, V> builder) {
        if (EXPIRER == null) {
            synchronized (ExpiringMap.class) {
                if (EXPIRER == null) {
                    EXPIRER =
                            Executors.newSingleThreadScheduledExecutor(
                                    THREAD_FACTORY == null
                                            ? new NamedThreadFactory("ExpiringMap-Expirer")
                                            : THREAD_FACTORY);
                }
            }
        }

        if (LISTENER_SERVICE == null && builder.asyncExpirationListeners != null)
            initListenerService();

        variableExpiration = builder.variableExpiration;
        entries =
                variableExpiration ? new EntryTreeHashMap<K, V>() : new EntryLinkedHashMap<K, V>();
        if (builder.expirationListeners != null)
            expirationListeners =
                    new CopyOnWriteArrayList<ExpirationListener<K, V>>(builder.expirationListeners);
        if (builder.asyncExpirationListeners != null)
            asyncExpirationListeners =
                    new CopyOnWriteArrayList<ExpirationListener<K, V>>(
                            builder.asyncExpirationListeners);
        expirationPolicy = new AtomicReference<ExpirationPolicy>(builder.expirationPolicy);
        expirationNanos =
                new AtomicLong(TimeUnit.NANOSECONDS.convert(builder.duration, builder.timeUnit));
        maxSize = builder.maxSize;
        entryLoader = builder.entryLoader;
        expiringEntryLoader = builder.expiringEntryLoader;
    }

    /**
     * Builds ExpiringMap instances. Defaults to ExpirationPolicy.CREATED, expiration of 60
     * TimeUnit.SECONDS and a maxSize of Integer.MAX_VALUE.
     */
    public static final class Builder<K, V> {
        private ExpirationPolicy expirationPolicy = ExpirationPolicy.CREATED;
        private List<ExpirationListener<K, V>> expirationListeners;
        private List<ExpirationListener<K, V>> asyncExpirationListeners;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        private boolean variableExpiration;
        private long duration = 60;
        private int maxSize = Integer.MAX_VALUE;
        private EntryLoader<K, V> entryLoader;
        private ExpiringEntryLoader<K, V> expiringEntryLoader;

        /** Creates a new Builder object. */
        private Builder() {}

        /**
         * Builds and returns an expiring map.
         *
         * @param <K1> Key type
         * @param <V1> Value type
         */
        @SuppressWarnings("unchecked")
        public <K1 extends K, V1 extends V> ExpiringMap<K1, V1> build() {
            return new ExpiringMap<K1, V1>((Builder<K1, V1>) this);
        }

        /**
         * Sets the default map entry expiration.
         *
         * @param duration the length of time after an entry is created that it should be removed
         * @param timeUnit the unit that {@code duration} is expressed in
         * @throws NullPointerException if {@code timeUnit} is null
         */
        public Builder<K, V> expiration(long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = (TimeUnit) Checks.nonNull(timeUnit, "timeUnit");
            return this;
        }

        /**
         * Sets the maximum size of the map. Once this size has been reached, adding an additional
         * entry will expire the first entry in line for expiration based on the expiration policy.
         *
         * @param maxSize The maximum size of the map.
         */
        public Builder<K, V> maxSize(int maxSize) {
            Checks.check(maxSize > 0, "maxSize");
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Sets the EntryLoader to use when loading entries. Either an EntryLoader or
         * ExpiringEntryLoader may be set, not both.
         *
         * @param entryLoader the EntryLoader to use
         * @throws NullPointerException if {@code entryLoader} is null
         */
        public Builder<K, V> entryLoader(EntryLoader<K, V> entryLoader) {
            Checks.nonNull(entryLoader, "entryLoader");
            Checks.isNull(expiringEntryLoader, "Expiring entry loader already set");
            this.entryLoader = entryLoader;
            return this;
        }

        /**
         * Sets the ExpiringEntryLoader to use when loading entries. Either an EntryLoader or
         * ExpiringEntryLoader may be set, not both.
         *
         * @param expiringEntryLoader the ExpiringEntryLoader to use
         * @throws NullPointerException if {@code expiringEntryLoader} is null
         */
        public Builder<K, V> expiringEntryLoader(ExpiringEntryLoader<K, V> expiringEntryLoader) {
            Checks.nonNull(expiringEntryLoader, "expiringEntryLoader");
            Checks.isNull(entryLoader, "Entry loader already set");
            this.expiringEntryLoader = expiringEntryLoader;
            return this;
        }

        /**
         * Sets the expiration policy for the map.
         *
         * @param expirationPolicy the expiration policy to use
         * @throws NullPointerException if {@code expirationPolicy} is null
         */
        public Builder<K, V> expirationPolicy(ExpirationPolicy expirationPolicy) {
            this.expirationPolicy =
                    (ExpirationPolicy) Checks.nonNull(expirationPolicy, "expirationPolicy");
            return this;
        }

        /**
         * Enables variable entry expiration. When enabled, each entry can have its own expiration
         * duration and policy.
         */
        public Builder<K, V> variableExpiration() {
            this.variableExpiration = true;
            return this;
        }

        /**
         * Adds an expiration listener.
         *
         * @param listener the expiration listener to add
         * @throws NullPointerException if {@code listener} is null
         */
        public Builder<K, V> expirationListener(ExpirationListener<K, V> listener) {
            Checks.nonNull(listener, "listener");
            if (expirationListeners == null) expirationListeners = new ArrayList<>();
            expirationListeners.add(listener);
            return this;
        }

        /**
         * Adds an asynchronous expiration listener.
         *
         * @param listener the expiration listener to add
         * @throws NullPointerException if {@code listener} is null
         */
        public Builder<K, V> asyncExpirationListener(ExpirationListener<K, V> listener) {
            Checks.nonNull(listener, "listener");
            if (asyncExpirationListeners == null) asyncExpirationListeners = new ArrayList<>();
            asyncExpirationListeners.add(listener);
            return this;
        }
    }

    /**
     * Creates a new ExpiringMap with default settings.
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return new map instance
     */
    public static <K, V> ExpiringMap<K, V> create() {
        return new Builder<K, V>().build();
    }

    /**
     * Returns a new ExpiringMap builder.
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return Builder
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<K, V>();
    }

    /**
     * Returns the expected expiration duration, in milliseconds from the current time, for the entry
     * corresponding to the given {@code key}.
     *
     * @param key the key to get the expected expiration for
     * @return the expected expiration duration in milliseconds
     * @throws NullPointerException if {@code key} is null
     * @throws NoSuchElementException if no entry exists for the given key
     */
    public long getExpectedExpiration(K key) {
        Checks.nonNull(key, "key");
        ExpiringEntry<K, V> entry = getEntry(key);
        Checks.element(entry, key);
        return TimeUnit.NANOSECONDS.toMillis(entry.expectedExpiration.get() - System.nanoTime());
    }

    /**
     * Gets the ExpirationPolicy for the entry corresponding to the given {@code key}.
     *
     * @param key the key to get the ExpirationPolicy for
     * @return the ExpirationPolicy for the key
     * @throws NullPointerException if {@code key} is null
     * @throws NoSuchElementException if no entry exists for the given key
     */
    public ExpirationPolicy getExpirationPolicy(K key) {
        Checks.nonNull(key, "key");
        ExpiringEntry<K, V> entry = getEntry(key);
        Checks.element(entry, key);
        return entry.expirationPolicy.get();
    }

    /**
     * Gets the expected expiration, in milliseconds from the current time, for the entry
     * corresponding to the given {@code key}.
     *
     * @param key the key to get the expected expiration for
     * @return the expiration duration in milliseconds
     * @throws NullPointerException if {@code key} is null
     * @throws NoSuchElementException if no entry exists for the given key
     */
    public long getExpectedExpiration(K key) {
        Checks.nonNull(key, "key");
        ExpiringEntry<K, V> entry = getEntry(key);
        Checks.element(entry, key);
        return TimeUnit.NANOSECONDS.toMillis(entry.expectedExpiration.get() - System.nanoTime());
    }

    /**
     * Gets the maximum size of the map. Once this size has been reached, adding an additional entry
     * will expire the first entry in line for expiration based on the expiration policy.
     *
     * @return The maximum size of the map.
     */
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return entries.hashCode();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return entries.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public void clear() {
                ExpiringMap.this.clear();
            }

            @Override
            public boolean contains(Object key) {
                return containsKey(key);
            }

            @Override
            public Iterator<K> iterator() {
                return (entries instanceof EntryLinkedHashMap)
                        ? ((EntryLinkedHashMap<K, V>) entries).new KeyIterator()
                        : ((EntryTreeHashMap<K, V>) entries).new KeyIterator();
            }

            @Override
            public boolean remove(Object value) {
                return ExpiringMap.this.remove(value) != null;
            }

            @Override
            public int size() {
                return ExpiringMap.this.size();
            }
        };
    }

    /**
     * Puts {@code value} in the map for {@code key}. Resets the entry's expiration unless an entry
     * already exists for the same {@code key} and {@code value}.
     *
     * @param key the key to put value for
     * @param value the value to put for key
     * @return the old value
     * @throws NullPointerException if {@code key} is null
     */
    @Override
    public V put(K key, V value) {
        Checks.nonNull(key, "key");
        return putInternal(key, value, expirationPolicy.get(), expirationNanos.get());
    }

    /**
     * @see #put(Object, Object, ExpirationPolicy, long, TimeUnit)
     */
    public V put(K key, V value, ExpirationPolicy expirationPolicy) {
        return put(key, value, expirationPolicy, expirationNanos.get(), TimeUnit.NANOSECONDS);
    }

    /**
     * @see #put(Object, Object, ExpirationPolicy, long, TimeUnit)
     */
    public V put(K key, V value, long duration, TimeUnit timeUnit) {
        return put(key, value, expirationPolicy.get(), duration, timeUnit);
    }

    /**
     * Puts {@code value} in the map for {@code key}. Resets the entry's expiration unless an entry
     * already exists for the same {@code key} and {@code value}. Requires that variable expiration
     * be enabled.
     *
     * @param key the key to put value for
     * @param value the value to put for key
     * @param expirationPolicy the expiration policy to use
     * @param duration the length of time after an entry is created that it should be removed
     * @param timeUnit the unit that {@code duration} is expressed in
     * @return the old value
     * @throws UnsupportedOperationException if variable expiration is not enabled
     * @throws NullPointerException if {@code key}, {@code expirationPolicy} or {@code timeUnit} are
     *     null
     */
    public V put(
            K key, V value, ExpirationPolicy expirationPolicy, long duration, TimeUnit timeUnit) {
        Checks.nonNull(key, "key");
        Checks.nonNull(expirationPolicy, "expirationPolicy");
        Checks.nonNull(timeUnit, "timeUnit");
        Checks.check(variableExpiration, "Variable expiration is not enabled");
        return putInternal(
                key, value, expirationPolicy, TimeUnit.NANOSECONDS.convert(duration, timeUnit));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        Checks.nonNull(map, "map");
        long expiration = expirationNanos.get();
        ExpirationPolicy expirationPolicy = this.expirationPolicy.get();
        writeLock.lock();
        try {
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
                putInternal(entry.getKey(), entry.getValue(), expirationPolicy, expiration);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Checks.nonNull(key, "key");
        writeLock.lock();
        try {
            if (!entries.containsKey(key))
                return putInternal(key, value, expirationPolicy.get(), expirationNanos.get());
            else return entries.get(key).getValue();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        Checks.nonNull(key, "key");
        writeLock.lock();
        try {
            ExpiringEntry<K, V> entry = entries.remove(key);
            if (entry == null) return null;
            if (entry.cancel()) scheduleEntry(entries.first());
            return entry.getValue();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        Checks.nonNull(key, "key");
        writeLock.lock();
        try {
            ExpiringEntry<K, V> entry = entries.get(key);
            if (entry != null && entry.getValue().equals(value)) {
                entries.remove(key);
                if (entry.cancel()) scheduleEntry(entries.first());
                return true;
            } else return false;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V replace(K key, V value) {
        Checks.nonNull(key, "key");
        writeLock.lock();
        try {
            if (entries.containsKey(key)) {
                return putInternal(key, value, expirationPolicy.get(), expirationNanos.get());
            } else return null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Checks.nonNull(key, "key");
        writeLock.lock();
        try {
            ExpiringEntry<K, V> entry = entries.get(key);
            if (entry != null && entry.getValue().equals(oldValue)) {
                putInternal(key, newValue, expirationPolicy.get(), expirationNanos.get());
                return true;
            } else return false;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sets the expiration for the entry corresponding to the given {@code key} to the specified
     * {@code duration} and {@code timeUnit}.
     *
     * @param key the key to set expiration for
     * @param duration the length of time after an entry is created that it should be removed
     * @param timeUnit the unit that {@code duration} is expressed in
     * @return true if the expiration was set for the key, false if no entry exists for the key
     * @throws UnsupportedOperationException if variable expiration is not enabled
     * @throws NullPointerException if {@code key} or {@code timeUnit} are null
     */
    public boolean setExpiration(K key, long duration, TimeUnit timeUnit) {
        Checks.nonNull(key, "key");
        Checks.nonNull(timeUnit, "timeUnit");
        Checks.check(variableExpiration, "Variable expiration is not enabled");

        long nanos = TimeUnit.NANOSECONDS.convert(duration, timeUnit);
        ExpiringEntry<K, V> entry = entries.get(key);
        if (entry != null) {
            entry.setExpirationNanos(nanos);
            return true;
        }
        return false;
    }

    /**
     * Sets the expiration policy for the entry corresponding to the given {@code key}.
     *
     * @param key the key to set expiration policy for
     * @param expirationPolicy the expiration policy to use
     * @return true if the expiration policy was set for the key, false if no entry exists for the
     *     key
     * @throws NullPointerException if {@code key} or {@code expirationPolicy} are null
     */
    public boolean setExpirationPolicy(K key, ExpirationPolicy expirationPolicy) {
        Checks.nonNull(key, "key");
        Checks.nonNull(expirationPolicy, "expirationPolicy");

        ExpiringEntry<K, V> entry = entries.get(key);
        if (entry != null) {
            entry.setExpirationPolicy(expirationPolicy);
            return true;
        }
        return false;
    }

    /**
     * Sets the maximum size of the map. Once this size has been reached, adding an additional entry
     * will expire the first entry in line for expiration based on the expiration policy.
     *
     * @param maxSize The maximum size of the map.
     */
    public void setMaxSize(int maxSize) {
        Checks.check(maxSize > 0, "maxSize");
        this.maxSize = maxSize;
        writeLock.lock();
        try {
            while (entries.size() > maxSize) {
                ExpiringEntry<K, V> entry = entries.first();
                if (entry == null) break;
                entries.remove(entry.getKey());
                entry.cancel();
                scheduleEntry(entries.first());
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return entries.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public void clear() {
                ExpiringMap.this.clear();
            }

            @Override
            public boolean contains(Object value) {
                return containsValue(value);
            }

            @Override
            public Iterator<V> iterator() {
                return (entries instanceof EntryLinkedHashMap)
                        ? ((EntryLinkedHashMap<K, V>) entries).new ValueIterator()
                        : ((EntryTreeHashMap<K, V>) entries).new ValueIterator();
            }

            @Override
            public int size() {
                return ExpiringMap.this.size();
            }
        };
    }

    /**
     * Schedules entry for expiration at its expected expiration time.
     */
    private void scheduleEntry(ExpiringEntry<K, V> entry) {
        if (entry != null && expirationPolicy.get() != ExpirationPolicy.NONE) {
            long expectedDuration = entry.expectedExpiration.get() - System.nanoTime();
            if (expectedDuration > 0)
                entry.expirationFuture =
                        EXPIRER.schedule(
                                new ExpirationRunnable(entry),
                                TimeUnit.NANOSECONDS.toMillis(expectedDuration),
                                TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Internal put method that handles expiration, callbacks and returns the old value.
     */
    private V putInternal(
            final K key,
            final V value,
            final ExpirationPolicy expirationPolicy,
            final long expirationNanos) {
        ExpiringEntry<K, V> newEntry = new ExpiringEntry<K, V>(key, value, expirationPolicy);
        writeLock.lock();
        try {
            ExpiringEntry<K, V> oldEntry = entries.put(key, newEntry);
            if (oldEntry != null) {
                if (oldEntry.equals(newEntry)) return oldEntry.getValue();
                oldEntry.cancel();
            }
            if (entries.size() > maxSize) {
                ExpiringEntry<K, V> entry = entries.first();
                if (entry != null) {
                    entries.remove(entry.getKey());
                    entry.cancel();
                }
            }
            scheduleEntry(newEntry);
            notifyExpirationListeners(key, value);
            return (oldEntry == null) ? null : oldEntry.getValue();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Initializes the async listener service for the first ExpiringMap instance.
     */
    private synchronized void initListenerService() {
        if (LISTENER_SERVICE == null) {
            LISTENER_SERVICE =
                    new ThreadPoolExecutor(
                            1,
                            1,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(),
                            THREAD_FACTORY == null
                                    ? new NamedThreadFactory("ExpiringMap-Listener")
                                    : THREAD_FACTORY);
        }
    }

    /**
     * Notifies expiration listeners.
     */
    private void notifyExpirationListeners(final K key, final V value) {
        if (expirationListeners != null) {
            for (final ExpirationListener<K, V> listener : expirationListeners) {
                if (listener != null) listener.expired(key, value);
            }
        }
        if (asyncExpirationListeners != null) {
            for (final ExpirationListener<K, V> listener : asyncExpirationListeners) {
                if (listener != null) {
                    LISTENER_SERVICE.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    listener.expired(key, value);
                                }
                            });
                }
            }
        }
    }

    private ExpiringEntry<K, V> getEntry(Object key) {
        readLock.lock();
        try {
            return entries.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /** Map entry with expiration and loading support */
    private class ExpiringEntry<K, V> extends WeakReference<K> implements Entry<K, V> {
        final AtomicReference<ExpirationPolicy> expirationPolicy =
                new AtomicReference<ExpirationPolicy>(ExpiringMap.this.expirationPolicy.get());
        final AtomicLong expectedExpiration;
        volatile Future<?> expirationFuture;
        final AtomicReference<V> valueRef = new AtomicReference<V>();

        ExpiringEntry(K key, V value, ExpirationPolicy expirationPolicy) {
            super(key, expirationQueue);
            this.valueRef.set(value);
            this.expirationPolicy.set(expirationPolicy);
            this.expectedExpiration =
                    new AtomicLong(
                            (expirationPolicy == ExpirationPolicy.ACCESSED)
                                    ? System.nanoTime() + expirationNanos.get()
                                    : Long.MAX_VALUE);
        }

        void setExpirationNanos(long nanos) {
            expirationPolicy.set(ExpirationPolicy.CREATED);
            expectedExpiration.set(System.nanoTime() + nanos);
        }

        void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
            expectedExpiration.set(System.nanoTime() + expirationNanos.get());
            this.expirationPolicy.set(expirationPolicy);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof Entry) {
                Entry<?, ?> other = (Entry<?, ?>) obj;
                Object value = getValue();
                return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
                        && (value == null ? other.getValue() == null : value.equals(other.getValue()));
            } else return false;
        }

        @Override
        public K getKey() {
            return get();
        }

        @Override
        public V getValue() {
            return valueRef.get();
        }

        @Override
        public int hashCode() {
            K key = get();
            V value = getValue();
            return ((key == null) ? 0 : key.hashCode())
                    ^ ((value == null) ? 0 : value.hashCode());
        }

        @Override
        public V setValue(V value) {
            V oldValue = valueRef.get();
            valueRef.set(value);
            return oldValue;
        }

        /**
         * Cancels the expiration future and prevents removal of the entry.
         *
         * @return true if the expiration future was cancelled, false if it has already occurred
         */
        boolean cancel() {
            Future<?> expirationFuture = this.expirationFuture;
            return (expirationFuture != null && expirationFuture.cancel(false));
        }

        @Override
        public String toString() {
            K key = get();
            V value = getValue();
            return ((key == null) ? "null" : key.toString())
                    + "="
                    + ((value == null) ? "null" : value.toString());
        }
    }

    private class ExpirationRunnable implements Runnable {
        private final ExpiringEntry<K, V> entry;

        ExpirationRunnable(ExpiringEntry<K, V> entry) {
            this.entry = entry;
        }

        @Override
        public void run() {
            writeLock.lock();
            try {
                if (entry.cancel()) {
                    entries.remove(entry.getKey());
                    notifyExpirationListeners(entry.getKey(), entry.getValue());
                    if (expirationPolicy.get() != ExpirationPolicy.NONE) {
                        scheduleEntry(entries.first());
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    /** Expiration listener thread factory */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger counter = new AtomicInteger();

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, namePrefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
