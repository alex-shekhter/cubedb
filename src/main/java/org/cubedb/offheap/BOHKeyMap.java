package org.cubedb.offheap;

import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.cubedb.api.KeyMap;
import org.cubedb.core.Constants;
import org.cubedb.core.beans.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cfelde.bohmap.BOHMap;
import com.cfelde.bohmap.Binary;

public class BOHKeyMap implements KeyMap {

	private BOHMap map;
	private Binary valueBinary;
	private int numPartitions;
	private static final Logger log = LoggerFactory.getLogger(BOHKeyMap.class);

	public BOHKeyMap() {
		this.createMap(0, 1);

	}

	public BOHKeyMap(int size, int fieldsLength) {
		this.createMap(size, fieldsLength);
	}

	protected void createMap(int curSize, int fieldsLength) {

		int startSize = Integer.max(curSize, Constants.START_TINY_SIZE);
		// log.info("Creating a new map with size of {}", startSize);
		this.numPartitions = startSize * 2;
		map = new BOHMap(this.numPartitions);
		this.valueBinary = new Binary(new byte[] { 0, 0, 0, 0 });
	}

	@Override
	public Integer get(byte[] b) {
		return BinaryToInt(this.map.get(new Binary(b)));
	}

	// This is totally not thread safe
	@Override
	public void put(byte[] k, int v) {
		Binary b = IntToBinary(v);
		this.valueBinary.setValue(k);
		Binary prev = this.map.put(this.valueBinary, b);
		if (prev == null && this.map.size() >= this.numPartitions * 3) {
			// Our map is now overgrown!
			int newSize = this.numPartitions * 5;
			long t0 = System.nanoTime();
			BOHMap oldMap = this.map;
			createMap(newSize, 0);
			log.debug("Re-sizing map to {}", newSize);
			for (Entry<Binary, Binary> e : oldMap.entrySet()) {

				this.map.put(e.getKey(), e.getValue());
			}
			oldMap.clear();
			log.debug("Resizing done in {} mks", (System.nanoTime() - t0) / 1000);
		}
	}

	@Override
	public int size() {
		return this.map.size();
	}

	protected static Integer BinaryToInt(Binary b) {
		if (b == null)
			return null;
		ByteBuffer buf = ByteBuffer.wrap(b.getValue());
		return (int)buf.getInt();
	}

	protected static Binary IntToBinary(int v) {
		ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
		buf.putInt( v);
		return new Binary(buf.array());
	}

	@Override
	public Stream<Entry<byte[], Integer>> entrySet() {
		return this.map.entrySet().stream().map((e) -> new Pair<byte[], Integer>(e.getKey().getValue(), BinaryToInt(e.getValue())));
	}

}