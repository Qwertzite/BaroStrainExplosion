package qwertzite.barostrain.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @see Collections#synchronizedMap(Map)
 * @author 17952
 * @date 2021/12/15
 * @param <K>
 * @param <V>
 */
public class SynchReadWriteMap<K, V> implements Map<K, V> { // IMPL: implement this class.
	// NOTE: Implement default methods (merge, computeIfAbsent, etc...).
	// NOTE: Make wrapped version. So that unmodifiable values can be updated without writing to the backing map.

	private final Map<K, V> backingMap;
	private final ReadWriteLock lock;
	
	public SynchReadWriteMap(Map<K, V> backingMap) {
		this.backingMap = backingMap;
		this.lock = new ReentrantReadWriteLock();
	}
	
	@Override
	public int size() {
		this.lock.readLock().lock();
		int size = this.backingMap.size();
		this.lock.readLock().unlock();
		return size;
	}

	@Override
	public boolean isEmpty() {
		this.lock.readLock().lock();
		boolean empty = this.backingMap.isEmpty();
		this.lock.writeLock().unlock();
		return empty;
	}

	@Override
	public boolean containsKey(Object key) {
		this.lock.readLock().lock();
		boolean contains = this.backingMap.containsKey(key);
		this.lock.readLock().unlock();
		return contains;
	}

	@Override
	public boolean containsValue(Object value) {
		this.lock.readLock().lock();
		boolean contains = this.backingMap.containsValue(value);
		this.lock.readLock().unlock();
		return contains;
	}

	@Override
	public V get(Object key) {
		this.lock.readLock().lock();
		V v = this.backingMap.get(key);
		this.lock.readLock().unlock();
		return v;
	}

	/**
	 * == heavy creation, modifiable object ==
	 * read lock
	 *   check that the key is absent
	 * read unlock
	 *   create value
	 * write lock
	 *   check that the key is absent
	 * write unlock
	 * synch (object)
	 *   object.modify
	 * 
	 * == heavy creation, unmodifyable object ==
	 * read lock
	 *   check that the key is absent
	 * read unlock
	 *   create value
	 * write lock
	 *   check that the key is absent
	 *   set new value.
	 * write unlock
	 * 
	 */
	@Override
	public V put(K key, V value) {
		this.lock.writeLock().lock();
		V v = this.backingMap.put(key, value);
		this.lock.writeLock().unlock();
		return v;
	}

	@Override
	public V remove(Object key) {
		this.lock.writeLock().lock();
		V v = this.backingMap.remove(key);
		this.lock.writeLock().unlock();
		return v;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		this.lock.writeLock().lock();
		this.backingMap.putAll(m);
		this.lock.writeLock().unlock();
	}

	@Override
	public void clear() {
		this.lock.writeLock().lock();
		this.backingMap.clear();
		this.lock.writeLock().unlock();
	}

	@Override
	public Set<K> keySet() {
		this.lock.readLock().lock();
		Set<K> set = this.backingMap.keySet();
		this.lock.readLock().unlock();
		return set;
	}

	@Override
	public Collection<V> values() {
		this.lock.readLock().lock();
		Collection<V> collection = this.backingMap.values();
		this.lock.readLock().unlock();
		return collection;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		this.lock.readLock().lock();
		Set<Entry<K, V>> collection = this.backingMap.entrySet();
		this.lock.readLock().unlock();
		return collection;
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		this.lock.readLock().lock();
		V v = this.backingMap.get(key);
		boolean flag = v != null || containsKey(key);
		this.lock.readLock().unlock();
		return flag ? v : defaultValue;
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		this.lock.readLock().lock();
		this.backingMap.forEach(action);
		this.lock.readLock().unlock();
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		this.lock.readLock().lock();
		this.backingMap.replaceAll(function);
		this.lock.readLock().unlock();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		this.lock.readLock().lock();
		V v = this.backingMap.get(key);
		if (v == null) {
			this.lock.readLock().unlock();
			this.lock.writeLock().lock();
			v = this.backingMap.get(key);
			if (v == null) {
				v = this.backingMap.put(key, value);
			}
			this.lock.writeLock().unlock();
		} else this.lock.readLock().unlock();
		return v;
	}

	/**
	 * Assumes that remove operation rarely occurs.<br>
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object key, Object value) {
		this.lock.readLock().lock();
		Object curValue = this.backingMap.get(key);
		if (!Objects.equals(curValue, value) || (curValue == null && !this.backingMap.containsKey(key))) {
			this.lock.readLock().unlock();
			return false;
		}
		this.lock.readLock().unlock();
		
		this.lock.writeLock().lock();
		curValue = this.backingMap.get(key);
		if (!Objects.equals(curValue, value) || (curValue == null && !this.backingMap.containsKey(key))) {
			this.lock.writeLock().unlock();
			return false;
		}
		this.backingMap.remove(key);
		this.lock.writeLock().unlock();
		return true;
	}

	/**
	 * Assumes that old value rarely matches.<br>
	 * {@inheritDoc}
	 */
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		this.lock.readLock().lock();
		Object curValue = this.backingMap.get(key);
		if (!Objects.equals(curValue, oldValue) || (curValue == null && !this.backingMap.containsKey(key))) {
			this.lock.readLock().unlock();
			return false;
		}
		this.lock.readLock().unlock();
		
		this.lock.writeLock().lock();
		curValue = this.backingMap.get(key);
		if (!Objects.equals(curValue, oldValue) || (curValue == null && !this.backingMap.containsKey(key))) {
			this.lock.writeLock().unlock();
			return false;
		}
		this.backingMap.put(key, newValue);
		this.lock.writeLock().unlock();
		return true;
	}

	/**
	 * Assumes that replacement rarely occurs.<br>
	 * {@inheritDoc}
	 */
	@Override
	public V replace(K key, V value) {
		this.lock.readLock().lock();
		V curValue = this.backingMap.get(key);
		if (((curValue) != null) || this.backingMap.containsKey(key)) {
			this.lock.readLock().unlock();
			this.lock.writeLock().lock();
			curValue = this.backingMap.get(key);
			if (((curValue) != null) || this.backingMap.containsKey(key)) {
				curValue = this.backingMap.put(key, value);
			}
			this.lock.writeLock().unlock();
		} else this.lock.readLock().unlock();
		return curValue;
	}
	
	/**
	 * Assume that the value is rarely empty. Computation of the mapping
	 * function may take place in multiple threads at a time. But it is assumed
	 * that the key collisions rarely occur and the computation is long enough
	 * that it is useful to release the locks during the computation.<br>
	 * To avoid wasting CPU resources, using singleton key will 
	 * {@inheritDoc}
	 */
	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		Objects.requireNonNull(mappingFunction);
		V v;
		this.lock.readLock().lock();
		v = this.backingMap.get(key);
		if (v == null) {
			this.lock.readLock().unlock();
			V newValue;
			 newValue = mappingFunction.apply(key);
			if (v == null) {
				if ((newValue) != null) {
					this.backingMap.put(key, newValue);
					return newValue;
				}
			}
		} else this.lock.readLock().unlock();
		return v;
	}

	@Deprecated
	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		Objects.requireNonNull(remappingFunction);
		V oldValue = this.backingMap.get(key);
		if (oldValue != null) {
			V newValue = remappingFunction.apply(key, oldValue);
			if (newValue != null) {
				this.lock.writeLock().lock();
				V oldValue2 = this.backingMap.get(key);
				if (!oldValue2.equals(oldValue)) newValue =
				this.backingMap.put(key, newValue);
				return newValue;
			} else {
				this.backingMap.remove(key);
				return null;
			}
		} else {
			return null;
		}
	}

    @Override
    public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);

        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            // delete mapping
            if (oldValue != null || containsKey(key)) {
                // something to remove
                remove(key);
                return null;
            } else {
                // nothing to do. Leave things as they were.
                return null;
            }
        } else {
            // add or replace old mapping
            put(key, newValue);
            return newValue;
        }
    }
    
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value :
                   remappingFunction.apply(oldValue, value);
        if(newValue == null) {
            remove(key);
        } else {
            put(key, newValue);
        }
        return newValue;
    }
}
