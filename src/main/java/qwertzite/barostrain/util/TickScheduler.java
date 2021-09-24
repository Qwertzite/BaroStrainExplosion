package qwertzite.barostrain.util;

import java.util.PriorityQueue;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import qwertzite.barostrain.util.function.Action;

public class TickScheduler {
	
	private final PriorityQueue<Entry> queue = new PriorityQueue<>((e1, e2) -> Long.compare(e1.index, e2.index));
	private long index = Long.MIN_VALUE;
	
	@SubscribeEvent
	public void onTick(TickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			while (!queue.isEmpty() && queue.peek().index <= index) {
				queue.poll().action.execute();
			}
			this.index++;
		}
	}
	
	public void add(long ellapse, Action action) {
		if (ellapse < 0) {
			BsModLog.warn("Illegal waiting time! Must be zero or positive. {}", ellapse);
			ellapse = 0;
		}
		if (Long.MAX_VALUE - ellapse <= index) {
			this.relocate();
		}
		this.queue.add(new Entry(action, ellapse+index));
	}
	
	private void relocate() {
		for (Entry e : queue) {
			e.index -= this.index;
		}
		this.index = Long.MIN_VALUE;
	}
	
	public static class Entry {
		public final Action action;
		public long index;
		public Entry(Action action, long index) {
			this.action = action;
			this.index = index;
		}
	}
}
