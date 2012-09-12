package net.sf.mardao.api.dao;

import net.sf.mardao.api.CursorPage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sf.mardao.api.Filter;
import net.sf.mardao.api.domain.CreatedUpdatedEntity;
import net.sf.mardao.api.geo.DLocation;
import net.sf.mardao.api.geo.GeoModel;
import net.sf.mardao.api.geo.Geobox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the base class for all implementations of the Dao Bean.
 * 
 * @author os
 * 
 * @param <T>
 *            domain object type
 * @param <ID>
 *            domain object simple key type
 * @param <P>
 *            domain object parent key type
 * @param <CT>
 *            cursor type
 * @param <E>
 *            database core entity type
 * @param <C>
 *            database core key type
 */
public abstract class DaoImpl<T extends CreatedUpdatedEntity<ID>, ID extends Serializable, 
        P extends Serializable, CT extends Object,
        E extends Serializable, C extends Serializable>
        implements Dao<T, ID> {
    
    public static final String PRINCIPAL_NAME_ANONYMOUS = "[ANONYMOUS]";
    
    /** Default name of the geoboxes column is "geoboxes" */
    public static final String COLUMN_NAME_GEOBOXES_DEFAULT = "geoboxes";

    /** Using slf4j logging */
    protected static final Logger   LOG = LoggerFactory.getLogger(DaoImpl.class);
    
    private Collection<Integer> boxBits = Arrays.asList(
        Geobox.BITS_12_10km, Geobox.BITS_15_1224m, Geobox.BITS_18_154m
    );

    /** set this, to have createdBy and updatedBy set */
    private static final ThreadLocal<String> principalName = new ThreadLocal<String>();
    
    /** mostly for logging */
    protected final Class<T> persistentClass;
    
    /** 
     * Set this to true in DaoBean constructor, to enable
     * the all-domains memCache
     */
    protected boolean memCacheAll = false;

    /** 
     * Set this to true in DaoBean constructor, to enable
     * the primaryKey-to-domain memCache
     */
    protected boolean memCacheEntities = false;

    protected DaoImpl(Class<T> type) {
        this.persistentClass = type;
    }

    public String getTableName() {
        return persistentClass.getSimpleName();
    }

    // --- BEGIN persistence-type beans must implement these ---
    
    /**
     * Implement / Override this in TypeDaoImpl. This method does not have to
     * worry about invalidating the cache, that is done in delete(parentKey, simpleKeys)
     * @param parentKey
     * @param simpleKeys
     * @return number of rows deleted (optional)
     */
    protected abstract int doDelete(Object parentKey, Iterable<ID> simpleKeys);
    /**
     * Implement / Override this in TypeDaoImpl. This method does not have to
     * worry about invalidating the cache, that is done in delete(domains)
     * @param domains
     * @return number of rows deleted (optional)
     */
    protected abstract int doDelete(Iterable<T> domains);
    
    protected abstract T doFindByPrimaryKey(Object parentKey, ID simpleKeys);
    protected abstract Iterable<T> doQueryByPrimaryKeys(Object parentKey, Iterable<ID> simpleKeys);

    protected abstract T findUniqueBy(Filter... filters);
    
    /** Implemented in TypeDaoImpl */
    protected abstract Collection<C> persistCore(Iterable<E> itrbl);
    
    /** Implemented in TypeDaoImpl */
    protected abstract CursorPage<T, ID> queryPage(boolean keysOnly, int pageSize,
            C ancestorKey, C simpleKey,
            String primaryOrderBy, boolean primaryIsAscending,
            String secondaryOrderBy, boolean secondaryIsAscending,
            Serializable cursorString,
            Filter... filters);

    /** Implemented in TypeDaoImpl */
    protected abstract Iterable<T> queryIterable(boolean keysOnly, 
            int offset, int limit,
            P ancestorKey, C simpleKey,
            String primaryOrderBy, boolean primaryIsAscending,
            String secondaryOrderBy, boolean secondaryIsAscending,
            Filter... filters);

    /** Implemented in TypeDaoImpl */
    protected abstract Iterable<ID> queryIterableKeys(int offset, int limit,
            P ancestorKey, C simpleKey,
            String primaryOrderBy, boolean primaryIsAscending,
            String secondaryOrderBy, boolean secondaryIsAscending,
            Filter... filters);

    /** Implemented in TypeDaoImpl */
    protected abstract ID coreToSimpleKey(E core);
    /** Implemented in TypeDaoImpl */
    protected abstract ID coreKeyToSimpleKey(C core);
    /** Implemented in TypeDaoImpl */
    protected abstract P coreToParentKey(E core);
    
    
    protected abstract E createCore(Object parentKey, ID simpleKey);

    /** Implemented in TypeDaoImpl */
    protected abstract Object getCoreProperty(E core, String name);
    /** Implemented in TypeDaoImpl */
    protected abstract void setCoreProperty(E core, String name, Object value);
    
    protected abstract Filter createEqualsFilter(String columnName, Object value);
    
    // --- END persistence-type beans must implement these ---
    
    public T coreToDomain(E core) {
        if (null == core) {
            return null;
        }
        
        final ID simpleKey = coreToSimpleKey(core);
        final P parentKey = coreToParentKey(core);
        
        try {
            final T domain = createDomain(parentKey, simpleKey);

            // created, updated
            copyCorePropertyToDomain(domain._getNameCreatedBy(), core, domain);
            copyCorePropertyToDomain(domain._getNameCreatedDate(), core, domain);
            copyCorePropertyToDomain(domain._getNameUpdatedBy(), core, domain);
            copyCorePropertyToDomain(domain._getNameUpdatedDate(), core, domain);

            // Domain Entity-specific properties
            for (String name : getColumnNames()) {
                copyCorePropertyToDomain(name, core, domain);
            }

            return domain;
        }
        catch (IllegalAccessException shouldNeverHappen) {
            LOG.error(getTableName(), shouldNeverHappen);
        }
        catch (InstantiationException shouldNeverHappen) {
            LOG.error(getTableName(), shouldNeverHappen);
        }
        return null;
    }
    
    protected Object copyCorePropertyToDomain(String name, E core, T domain) {
        if (null == name) {
            return null;
        }
        
        Object value = getCoreProperty(core, name);
        setDomainProperty(domain, name, value);
        
        return value;
    }
    
    protected Object copyDomainPropertyToCore(String name, T domain, E core) {
        if (null == name) {
            return null;
        }
        
        Object value = getDomainProperty(domain, name);
        setCoreProperty(core, name, value);
        
        return value;
    }
    
    protected T createDomain() throws InstantiationException, IllegalAccessException {
        return createDomain(null, null);
    }

    protected T createDomain(Object parentKey, ID simpleKey) throws InstantiationException, IllegalAccessException {
        final T domain = persistentClass.newInstance();
        
        domain.setParentKey(parentKey);
        domain.setSimpleKey(simpleKey);
        
        return domain;
    }
    
    public E domainToCore(T domain, final Date currentDate) {
        if (null == domain) {
            return null;
        }
        
        E core = createCore(domain.getParentKey(), domain.getSimpleKey());
        
        // created, updated
        String principalName = domain.getCreatedBy();
        if (null == principalName) {
            principalName = getPrincipalName();
            if (null == principalName) {
                principalName = PRINCIPAL_NAME_ANONYMOUS;
            }
            domain._setCreatedBy(principalName);
        }
        setCoreProperty(core, domain._getNameCreatedBy(), principalName);
        
        Date date = domain.getCreatedDate();
        if (null == date) {
            date = currentDate;
            domain._setCreatedDate(currentDate);
        }
        setCoreProperty(core, domain._getNameCreatedDate(), date);
        
        principalName = getPrincipalName();
        if (null == principalName) {
            principalName = PRINCIPAL_NAME_ANONYMOUS;
        }
        domain._setUpdatedBy(principalName);
        setCoreProperty(core, domain._getNameUpdatedBy(), principalName);
        
        domain._setUpdatedDate(currentDate);
        setCoreProperty(core, domain._getNameUpdatedDate(), currentDate);
        
        // Domain Entity-specific properties
        for (String name : getColumnNames()) {
            copyDomainPropertyToCore(name, domain, core);
//            copyCorePropertyToDomain(name, core, domain);
        }

        // geoboxes
        if (null != getLocationColumnName()) {
            updateGeoModel(domain, core);
        }

        return core;
    }
    
    public Collection<Serializable> domainsToPrimaryKeys(Iterable<T> domains) {
        final Collection<Serializable> keys = new ArrayList<Serializable>();
        Serializable pk;
        for (T d : domains) {
            pk = d.getPrimaryKey();
            keys.add(pk);
        }
        return keys;
    }

    public Collection<ID> coresToSimpleKeys(Iterable<E> cores) {
        final Collection<ID> ids = new ArrayList<ID>();
        ID id;
        for (E core : cores) {
            id = coreToSimpleKey(core);
            ids.add(id);
        }
        return ids;
    }

    /** Override in GeneratedDaoImpl */
    protected Object getDomainProperty(T domain, String name) {
        Object value;
        if (name.equals(domain._getNameCreatedBy())) {
            value = domain.getCreatedBy();
        }
        else if (name.equals(domain._getNameCreatedDate())) {
            value = domain.getCreatedDate();
        }
        else if (name.equals(domain._getNameUpdatedBy())) {
            value = domain.getUpdatedBy();
        }
        else if (name.equals(domain._getNameUpdatedDate())) {
            value = domain.getUpdatedDate();
        }
        else {
            throw new IllegalArgumentException(String.format("No such property %s for %s", name, getTableName()));
        }
        
        return value;
    }

    /** Default implementation returns null, override for your hierarchy */
    public String getParentKeyColumnName() {
        return null;
    }
    
    public static String getPrincipalName() {
        return principalName.get();
    }
    
    /**
     * Override to return your desired column name
     * @return COLUMN_NAME_GEOBOXES_DEFAULT, i.e. "geoboxes"
     */
    public String getGeoboxesColumnName() {
        return COLUMN_NAME_GEOBOXES_DEFAULT;
    }
    
    /** Override in GeneratedEntityDaoImpl */
    protected String getLocationColumnName() {
        return null;
    }

    /** geoboxes are needed to findGeo the nearest entities before sorting them by distance */
    protected void updateGeoModel(T domain, E core) throws IllegalArgumentException {
        if (domain instanceof GeoModel) {
            final GeoModel geoDomain = (GeoModel) domain;
            final DLocation location = geoDomain.getLocation();
            final Collection<Long> geoboxes = new ArrayList<Long>();
            
            // if entity has no location, simply set the domain field to empty collection
            if (null != location) {
                for (int bits : boxBits) {
                    geoboxes.addAll(Geobox.getTuple(location.getLatitude(), location.getLongitude(), bits));
                }
                setCoreProperty(core, getGeoboxesColumnName(), geoboxes);
            }
            
            geoDomain.setGeoboxes(geoboxes);
        }
        else {
            throw new IllegalArgumentException(getTableName() + " not instance of GeoModel");
        }
    }

    // --- BEGIN Dao methods ---
    
    public int delete(Object parentKey, Iterable<ID> simpleKeys) {
        final int count = doDelete(parentKey, simpleKeys);
        
        // TODO: invalidate cache
        
        return count;
    }

    public int delete(Iterable<T> domains) {
        final int count = doDelete(domains);
        
        // TODO: invalidate cache
        
        return count;
    }

    public boolean delete(Object parentKey, ID simpleKey) {
        final int count = delete(parentKey, Arrays.asList(simpleKey));
        return 1 == count;
    }
    
    public boolean delete(ID simpleKey) {
        final int count = delete(null, Arrays.asList(simpleKey));
        return 1 == count;
    }
    
    public boolean delete(T domain) {
        final int count = delete(domain.getParentKey(), Arrays.asList(domain.getSimpleKey()));
        return 1 == count;
    }
    
    public T findByPrimaryKey(Object parentKey, ID simpleKey) {
        // TODO: find in cache
        
        return doFindByPrimaryKey(parentKey, simpleKey);
    }
    
    public T findByPrimaryKey(ID simpleKey) {
        return findByPrimaryKey(null, simpleKey);
    }
    
    public Collection<ID> persist(Iterable<T> domains) {
        final Date currentDate = new Date();
        
        // convert to Core Entity:
        final Collection<E> itrbl = new ArrayList<E>();
        E core;
        for (T d : domains) {
            core = domainToCore(d, currentDate);
            itrbl.add(core);
        }
        
        // batch-persist:
        final Collection<C> keys = persistCore(itrbl);
        
        // collect IDs to return:
        final Collection<ID> ids = new ArrayList<ID>(itrbl.size());
        Iterator<T> ds = domains.iterator();
        T d;
        ID simpleKey;
        for (C c : keys) {
            simpleKey = coreKeyToSimpleKey(c);
            ids.add(simpleKey);
            
            // update domain with generated key?
            d = ds.next();
            if (null == d.getSimpleKey()) {
                d.setSimpleKey(simpleKey);
            }
        }
        
        return ids;
    }

    public ID persist(T domain) {
        final Iterable<ID> ids = persist(Arrays.asList(domain));
        final ID id = ids.iterator().hasNext() ? ids.iterator().next() : null;
        return id;
    }
    
    public Iterable<T> queryAll() {
        return queryIterable(false, 0, -1, null, null, null, false, null, false);
    }
    
    public Iterable<T> queryAll(Object parentKey) {
        return queryIterable(false, 0, -1, (P) parentKey, null, null, false, null, false);
    }
    
    public Iterable<ID> queryAllKeys() {
        return queryIterableKeys(0, -1, null, null, null, false, null, false);
    }
    
    public Iterable<ID> queryAllKeys(Object parentKey) {
        return queryIterableKeys(0, -1, (P) parentKey, null, null, false, null, false);
    }
    
    public Iterable<T> queryByPrimaryKeys(Object parentKey, Iterable<ID> simpleKeys) {
        // TODO: find in cache
        
        return doQueryByPrimaryKeys(parentKey, simpleKeys);
    }
    
    public CursorPage<T, ID> queryPage(int pageSize, Serializable cursorString) {
        return queryPage(false, pageSize, null, null,
                null, false, null, false,
                cursorString);
    }

    public CursorPage<T, ID> queryInGeobox(float lat, float lng, int bits, int pageSize, 
            String primaryOrderBy, boolean primaryIsAscending, String secondaryOrderBy, boolean secondaryIsAscending, 
            Serializable cursorString, Filter... filters) {
        if (!boxBits.contains(bits)) {
            throw new IllegalArgumentException("Unboxed resolution, hashed are " + boxBits);
        }
        
        final long box = Geobox.getHash(lat, lng, bits);
        
        final Filter geoFilters[] = Arrays.copyOf(filters, filters != null ? filters.length + 1 : 1, Filter[].class);
        geoFilters[geoFilters.length-1] = createEqualsFilter(getGeoboxesColumnName(), box);
        return queryPage(false, pageSize, null, null, primaryOrderBy, primaryIsAscending, secondaryOrderBy, secondaryIsAscending, 
                cursorString, geoFilters);
    }

    public Collection<T> findNearest(final float lat, final float lng, 
            String primaryOrderBy, boolean primaryIsAscending, String secondaryOrderBy, boolean secondaryIsAscending, 
            int offset, int limit, Filter... filters) {
        final DLocation p = new DLocation(lat, lng);
        int size = offset + (0 < limit ? limit : 10000);
        
        // sorting on distance has to be done outside datastore, i.e. here in application:
        Map<Double, T> orderedMap = new TreeMap<Double, T>();
        for (int bits : boxBits) {       
            final CursorPage<T, ID> subList = queryInGeobox(lat, lng, bits, limit,
                    primaryOrderBy, primaryIsAscending, secondaryOrderBy, secondaryIsAscending,
                    null, filters);
            for (T model : subList.getItems()) {
                double d = Geobox.distance(((GeoModel)model).getLocation(), p);
                orderedMap.put(d, model);
            }
            
            if (size <= orderedMap.size()) {
                break;
            }
        }
        // return with specified offset and limit
        final Collection<T> values = orderedMap.values();
        T[] page = (T[]) Arrays.copyOfRange(values.toArray(), 
                Math.min(offset, values.size()), Math.min(size, values.size()));
        return Arrays.asList(page);
    }

    /**
     * Implemented with a call to persist(domains). Feel free to override.
     * @param domains 
     */
    public void update(Iterable<T> domains) {
        // for this implementation, same as persist
        persist(domains);
    }

    /**
     * Implemented with a call to persist(domains). Feel free to override.
     * @param domain
     */
    public void update(T domain) {
        persist(Arrays.asList(domain));
    }
    
    // --- END Dao methods ---
    
    /** Override in GeneratedDaoImpl */
    protected void setDomainProperty(final T domain, final String name, final Object value) {
        if (name.equals(domain._getNameCreatedBy())) {
            domain._setCreatedBy((String) value);
        }
        else if (name.equals(domain._getNameCreatedDate())) {
            domain._setCreatedDate((Date) value);
        }
        else if (name.equals(domain._getNameUpdatedBy())) {
            domain._setUpdatedBy((String) value);
        }
        else if (name.equals(domain._getNameUpdatedDate())) {
            domain._setUpdatedDate((Date) value);
        }
        else {
            throw new IllegalArgumentException(String.format("No such property %s for %s", name, getTableName()));
        }
    }
    
    public void setBoxBits(Collection<Integer> boxBits) {
        this.boxBits = boxBits;
    }

    public static void setPrincipalName(String name) {
        principalName.set(name);
    }
    
//    public final Map<ID, T> findByPrimaryKeys(Key parentKey, Iterable<ID> primaryKeys) {
//        int entitiesCached = 0, entitiesQueried = 0;
//        final Map<ID, T> returnValue = new TreeMap<ID, T>();
//        
//        // convert to Keys
//        final List<Key> keys = new ArrayList<Key>();
//        Key key;
//        for(ID id : primaryKeys) {
//            key = createKey((Key) parentKey, id);
//            keys.add(key);
//        }
//        
//        ID id;
//        // check cache first
//        Map cached = null;
//        if (memCacheEntity) {
//            try {
//                cached = getMemCache().getAll(keys);
//                
//                // found entities should not be queried
//                keys.removeAll(cached.keySet());
//                
//                // add to returnValue
//                Set<Map.Entry<Key, T>> cachedEntries = cached.entrySet();
//                for (Map.Entry<Key, T> entry : cachedEntries) {
//                    id = convert(entry.getKey());
//                    returnValue.put(id, entry.getValue());
//                }
//                entitiesCached = cached.size();
//            } catch (CacheException ignore) {
//            }
//        }
//        
//        // cache miss?
//        if (!keys.isEmpty()) {
//            final DatastoreService datastore = getDatastoreService();
//            final Map<Key, Entity> entities = datastore.get(keys);
//            T domain;
//            final Map<Key, T> toCache = new HashMap<Key, T>(entities.size());
//            for(Map.Entry<Key, Entity> entry : entities.entrySet()) {
//                id = convert(entry.getKey());
//                domain = createDomain(entry.getValue());
//                returnValue.put(id, domain);
//                toCache.put(entry.getKey(), domain);
//            }
//
//            if (memCacheEntity) {
//                getMemCache().putAll(toCache);
//            }
//            entitiesQueried = entities.size();
//        }
//        
//        LOG.debug("cached:{}, queried:{}", entitiesCached, entitiesQueried);
//        return returnValue;
//    }
//

}
