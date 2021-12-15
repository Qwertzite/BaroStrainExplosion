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
public class SynchReadWriteMap<K, V> implements Map<K, V> { // FIXME: implement this class

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

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
