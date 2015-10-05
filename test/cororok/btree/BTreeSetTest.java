package cororok.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * For performance purpose it does not remove Element when it pops.
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class BTreeSetTest {
	public static void main(String[] args) {
		BTreeSetTest test = new BTreeSetTest();

		for (int i = 0; i < 10; i++) {
			System.out.println("# test " + i);
			if (test.test(10000) == false) {
				System.out.println("Failed");
				return;
			}
		}

		System.out.println("all right, done");

	}

	public boolean test(int testSize) {
		Random random = new Random();
		BTreeSet<Integer> set = new BTreeSet<Integer>(4);

		ArrayList<Integer> list = new ArrayList<Integer>();
		int doubleTestSize = testSize * 2;
		for (int i = 0; i < testSize; i++) {
			Integer key = random.nextInt(doubleTestSize);

			if (!list.contains(key))
				list.add(key);

			set.add(key);
		}

		System.out.println("size=" + list.size());
		if (havsSameValue(list, set) == false) {
			System.out.println("different");
			return false;
		}

		System.out.println("search test");
		for (int i = 0; i < testSize; i++) {
			Integer key = random.nextInt(doubleTestSize);
			if (list.contains(key) != set.contains(key)) {
				System.out.println("wrong search, key=" + key);
				return false;
			}
		}

		System.out.println("delete test");
		int delete = list.size() / 2;
		while (list.size() > delete) {
			Integer key = random.nextInt(doubleTestSize);
			if (list.contains(key) != set.contains(key)) {
				System.out.println("wrong search, key=" + key);
				return false;
			}

			if (list.remove(key) != set.remove(key)) {
				System.out.println("wrong delete");
				return false;
			}

			if (list.size() != set.size()) {
				System.out.println("wrong size");
				return false;
			}

			if (list.contains(key) != set.contains(key)) {
				System.out.println("wrong search");
				return false;
			}

			if (list.size() != set.size()) {
				System.out.println("wrong size");
				return false;
			}

		}

		// after delete
		System.out.println("after delete size=" + list.size());
		if (havsSameValue(list, set) == false) {
			System.out.println("different");
			return false;
		}
		return true;
	}

	public boolean havsSameValue(List<Integer> list, Set<Integer> set) {
		// size
		if (list.size() != set.size()) {
			System.out.println("wrong size");
			return false;
		}
		Collections.sort(list);

		// iterator
		Iterator<Integer> listItr = list.iterator();
		Iterator<Integer> setItr = set.iterator();

		while (listItr.hasNext()) {
			setItr.hasNext();

			int intList = listItr.next();
			int intSet = setItr.next();
			if (intList != intSet) {
				System.out.println("wrong iterator " + intList + " vs " + intSet);
				return false;
			}
		}

		return true;
	}

}
