/*
 * GNU GENERAL PUBLIC LICENSE
 Version 2, June 1991

 */
package cororok.btree;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * simple class using {@link java.util.concurrent.locks.ReentrantReadWriteLock}
 * to support concurrent multiple reads and single write.
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class ConcurrentMap<K, V> implements Map<K, V> {

	Map<K, V> map;
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	Lock read = lock.readLock();
	Lock write = lock.writeLock();

	ConcurrentMap(Map<K, V> map) {
		this.map = map;
	}

	@Override
	public int size() {
		read.lock();
		try {
			return map.size();
		} finally {
			read.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		read.lock();
		try {
			return map.isEmpty();
		} finally {
			read.unlock();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		read.lock();
		try {
			return map.containsKey(key);
		} finally {
			read.unlock();
		}
	}

	@Override
	public boolean containsValue(Object value) {
		read.lock();
		try {
			return map.containsValue(value);
		} finally {
			read.unlock();
		}
	}

	@Override
	public V get(Object key) {
		read.lock();
		try {
			return map.get(key);
		} finally {
			read.unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		write.lock();
		try {
			return map.put(key, value);
		} finally {
			write.unlock();
		}
	}

	@Override
	public V remove(Object key) {
		write.lock();
		try {
			return map.remove(key);
		} finally {
			write.unlock();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		write.lock();
		try {
			map.putAll(m);
		} finally {
			write.unlock();
		}
	}

	@Override
	public void clear() {
		write.lock();
		try {
			map.clear();
		} finally {
			write.unlock();
		}
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

}
