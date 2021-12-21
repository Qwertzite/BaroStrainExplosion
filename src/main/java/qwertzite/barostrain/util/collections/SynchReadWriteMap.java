package qwertzite.barostrain.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @see Collections#synchronizedMap(Map)
 * @author 17952
 * @date 2021/12/15
 * @param <K>
 * @param <V>
 */
public class SynchReadWriteMap<K, V> implements Map<K, V> { // IMPL: implement this class

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
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<K> keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<V> values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

}
