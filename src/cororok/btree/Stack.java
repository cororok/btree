/*
 * GNU GENERAL PUBLIC LICENSE
 Version 2, June 1991

 */

package cororok.btree;

/**
 * For performance purpose it does not remove Element when it pops.
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class Stack<T> {
	int point = 0;
	T[] array;

	public Stack() {
		this(10);
	}

	public Stack(int size) {
		if (size < 2)
			throw new RuntimeException("must be larger than 1");
		array = (T[]) new Object[size];
	}

	public void add(final T t) {
		array[point++] = t;
		checkSize();
	}

	private void checkSize() {
		if (point == array.length) {
			// 50% up
			T[] temp = (T[]) new Object[array.length + array.length / 2];
			System.arraycopy(array, 0, temp, 0, array.length);
			array = temp;
		}
	}

	/**
	 * It does not remove the Element, it does not even set Null.
	 * 
	 * @return
	 */
	public T pop() {
		if (point == 0)
			return null;

		return array[--point];
	}

	public int size() {
		return point;
	}

	/**
	 * does not reset Elements
	 */
	public void reset() {
		point = 0;
	}

	public void addAll(Stack<T> collection) {
		for (int i = 0; i < collection.size(); i++) {
			add(collection.array[i]);
		}
	}

}
