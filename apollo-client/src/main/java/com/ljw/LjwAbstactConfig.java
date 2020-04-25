package com.ljw;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.ctrip.framework.apollo.util.function.Functions;
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author lujiawei
 * @Description
 * @Date 2020/4/19
 **/
public abstract class LjwAbstactConfig implements Config {

    private static final ExecutorService m_executorService;

    private final List<ConfigChangeListener> m_listeners = Lists.newCopyOnWriteArrayList();

    private final Map<ConfigChangeListener, Set<String>> m_interestedKeys = Maps.newHashMap();

    private final Map<ConfigChangeListener, Set<String>> getM_interestedKeyPrefixes = Maps.newConcurrentMap();

    private final ConfigUtil m_configUtil;

    private volatile Cache<String, Integer> m_integerCache;
    private volatile Cache<String, Long> m_longCache;
    private volatile Cache<String, Short> m_shortCache;
    private volatile Cache<String, Float> m_floatCache;
    private volatile Cache<String, Double> m_doubleCache;
    private volatile Cache<String, Byte> m_byteCache;
    private volatile Cache<String, Boolean> m_booleanCache;
    private volatile Cache<String, Date> m_dateCache;
    private volatile Cache<String, Long> m_durationCache;
    private final Map<String, Cache<String, String[]>> m_arrayCache;
    private final List<Cache> allCaches;
    private final AtomicLong m_configVersion;

    protected PropertiesFactory propertiesFactory;

    static{
        m_executorService = Executors.newCachedThreadPool(ApolloThreadFactory.create("Config", true));
    }

    public LjwAbstactConfig(){
        m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        m_configVersion = new AtomicLong();
        m_arrayCache = Maps.newConcurrentMap();
        allCaches = Lists.newArrayList();
        propertiesFactory = ApolloInjector.getInstance(PropertiesFactory.class);
    }


    @Override
    public void addChangeListener(ConfigChangeListener listener) {
        addChangeListener(listener, null);
    }

    @Override
    public void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys) {
        addChangeListener(listener, interestedKeys, null);
    }

    @Override
    public void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys, Set<String> interestedKeyPrefixes) {
        if(!m_listeners.contains(listener)){
            m_listeners.add(listener);
            if(interestedKeys != null && !interestedKeys.isEmpty()){
                m_interestedKeys.put(listener, Sets.newHashSet(interestedKeys));
            }
            if(interestedKeyPrefixes != null && !interestedKeyPrefixes.isEmpty()){
                getM_interestedKeyPrefixes.put(listener, Sets.newHashSet(interestedKeyPrefixes));
            }
        }
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue){
        try{
            if(m_integerCache == null){
                synchronized (this){
                    if(m_integerCache == null){
                        m_integerCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_INT_FUNCTION, m_integerCache, defaultValue);
        }catch (Throwable ex){

        }
        return defaultValue;
    }

    private Integer getValueFromCache(String key, Function<String, Integer> toIntFunction, Cache<String, Integer> m_integerCache, Integer defaultValue) {

        Integer result = m_integerCache.getIfPresent(key);
        if(result != null){
            return result;
        }

        return getValueAndStoreToCache(key, toIntFunction, m_integerCache, defaultValue);


    }

    private Integer getValueAndStoreToCache(String key, Function<String, Integer> toIntFunction, Cache<String, Integer> m_integerCache, Integer defaultValue) {
            Long currentVersion = m_configVersion.get();
            String value = getProperty(key, null);
            if(value != null){
                Integer result = toIntFunction.apply(value);
                synchronized (this){
                    if(m_configVersion.get() == currentVersion){
                        m_integerCache.put(key, toIntFunction.apply(value));
                    }
                }
                return result;
            }
            return defaultValue;
    }

    private <T> Cache<String, T> newCache() {
        Cache<String, T> cache = CacheBuilder.newBuilder().maximumSize(m_configUtil.getMaxConfigCacheSize()).expireAfterAccess(m_configUtil.getConfigCacheExpireTime(), m_configUtil.getRefreshIntervalTimeUnit()).build();
        allCaches.add(cache);
        return cache;
    }

    protected void fireConfigChange(final ConfigChangeEvent changeEvent) {
        for(final ConfigChangeListener listener : m_listeners){
            if(! isConfigChangeListenerInterested(listener, changeEvent)){
                continue;
            }

            m_executorService.submit(new Runnable() {
                @Override
                public void run() {
                    listener.onChange(changeEvent);
                }
            });
        }

    }

    private boolean isConfigChangeListenerInterested(ConfigChangeListener listener, ConfigChangeEvent changeEvent) {
        Set<String> interestedKeys = m_interestedKeys.get(listener);
        Set<String> interestedKeyPrefixes = getM_interestedKeyPrefixes.get(listener);

        if((interestedKeys == null || interestedKeys.isEmpty()) &&( interestedKeyPrefixes == null || interestedKeyPrefixes.isEmpty())  ){
            return true;
        }

        if(interestedKeys != null){
            for(String key : interestedKeys){
                if(changeEvent.isChanged(key)){
                    return true;
                }
            }
        }

        if(interestedKeyPrefixes != null){
            for(String prefix : interestedKeyPrefixes){
                for(String change : changeEvent.changedKeys()){
                    if(change.startsWith(prefix)){
                        return true;
                    }
                }
            }
        }

        return false;
    }


}
