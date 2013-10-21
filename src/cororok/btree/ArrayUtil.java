/*
 * GNU GENERAL PUBLIC LICENSE
 Version 2, June 1991

 */
package cororok.btree;

/**
 * static utility methods for array shift operations.
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class ArrayUtil {
	/**
	 * overwrites left cell with right cell. If arr='01234', startIndex=1 and
	 * endIndex=3 then arr will be <12>234 (without <>).
	 * 
	 * @param arr
	 * @param startIndex
	 * @param endIndex
	 */
	public static void shiftLeft(Object[] arr, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			arr[i - 1] = arr[i];
		}
	}

	/**
	 * overwrites right cell with left cell. If arr='01234', startIndex=1 and
	 * endIndex=3 then arr will be 01<12>4 (without <>).
	 * 
	 * @param arr
	 * @param startIndex
	 * @param endIndex
	 */
	public static void shiftRight(Object[] arr, int startIndex, int endIndex) {
		for (int i = endIndex; i > startIndex; i--) {
			arr[i] = arr[i - 1];
		}
	}

	/**
	 * cuts and moves cells to target and overwrites target. If it calls
	 * ArrayUtil.moveTo(01234, abcde, 1, 3, 2) then src will be 0<NN>34 (
	 * N=null, without <>) and target will be ab<12>e (without <>)
	 * 
	 * @param src
	 * @param target
	 * @param startIndex
	 * @param endIndex
	 * @param newStartIndex
	 */
	public static void moveTo(Object[] src, Object[] target, int startIndex,
			int endIndex, int newStartIndex) {
		System.arraycopy(src, startIndex, target, newStartIndex, endIndex
				- startIndex);
		for (int i = startIndex; i < endIndex; i++) {
			src[i] = null;
		}
	}
}
