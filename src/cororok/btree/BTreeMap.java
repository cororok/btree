/*
 * GNU GENERAL PUBLIC LICENSE
 Version 2, June 1991

 */
package cororok.btree;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * It is compatible with standard {@link java.util.Map}. It uses a Set using B-Tree.
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class BTreeMap<K extends Comparable<K>, V> extends AbstractMap<K, V> {
	BTreeSet<KVEntity<K, V>> set;
	KVEntity<K, V> reuseEntity = new KVEntity<K, V>();

	public BTreeMap(int maxKey) {
		this.set = new BTreeSet<KVEntity<K, V>>(maxKey);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public V put(K key, V value) {
		KVEntity<K, V> newEntity = new KVEntity<K, V>(key, value);
		KVEntity<K, V> oldEntity = set.returnExistingKeyOrAdd(newEntity);

		if (oldEntity == null)
			return null;
		else {
			V oldValue = oldEntity.value;
			oldEntity.value = value;
			++set.changed;
			return oldValue;
		}
	}

	@Override
	public V get(Object key) {
		reuseEntity.key = (K) key;
		KVEntity<K, V> oldEntity = set.get(reuseEntity);
		return oldEntity == null ? null : oldEntity.value;
	}

	@Override
	public V remove(Object key) {
		reuseEntity.key = (K) key;
		KVEntity<K, V> oldEntity = set.get(reuseEntity);

		if (oldEntity != null && set.remove(reuseEntity))
			return oldEntity.value;

		return null;
	}

	class KVEntity<K extends Comparable<K>, V> implements Comparable<KVEntity<K, V>>, java.util.Map.Entry<K, V> {
		K key;
		V value;

		KVEntity() {
		}

		KVEntity(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int compareTo(KVEntity<K, V> obj) {
			return key.compareTo(obj.key);
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			this.value = value;
			return value;
		}

		public boolean equals(Object obj) {
			if (obj == null)
				return false;

			KVEntity<K, V> other = (KVEntity<K, V>) obj;
			return compareTo(other) == 1;
		}
	}

	class EntrySet extends AbstractSet<java.util.Map.Entry<K, V>> {
		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			return new EntryIterator(set.iterator());
		}

		@Override
		public int size() {
			return set.size();
		}
	}

	class EntryIterator implements Iterator<java.util.Map.Entry<K, V>> {
		Iterator<KVEntity<K, V>> itr;

		EntryIterator(Iterator<KVEntity<K, V>> itr) {
			this.itr = itr;
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public java.util.Map.Entry<K, V> next() {
			return itr.next();
		}

		@Override
		public void remove() {
			itr.remove();
		}
	}

	public int height() {
		return set.height();
	}
}
