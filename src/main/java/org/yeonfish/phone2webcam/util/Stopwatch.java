package org.yeonfish.phone2webcam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stopwatch {

	private int i;
	private Map<String, Long> points;

	public Stopwatch(){
		this.i = 0;
		this.points = new HashMap<>();
	}
	
	public long Flag() {
		long current = System.currentTimeMillis();
		points.put(String.valueOf(i), current);
		i++;
		return current;
	}

	public long Flag(String label) {
		long current = System.currentTimeMillis();
		points.put(label, current);
		i++;
		return current;
	}
	
	public void clear() {
		points.clear();
	}
	
	public long getDuration(String start, String end) {
		long duration = points.getOrDefault(end, 0L)-points.getOrDefault(start, 0L);
		return duration < 0 ? duration*-1 : duration;
	}

	public void printProfile() {
		List<String> keySet = new ArrayList<>(points.keySet());
		keySet.sort((o1, o2) -> points.get(o1).compareTo(points.get(o2)));

		String tmp = null;
		Long total = 0L;
		List<Long> timeLine = new ArrayList<>();
		for (String key : keySet) {
			if (tmp == null){
				tmp = key;
				continue;
			}

			Long timeSpent = points.get(key)-points.get(tmp);
			timeLine.add(timeSpent);
			total = total + timeSpent;
			tmp = key;
		}

		double multiplier = (double)100 / (double)total;

		Map<String, String> colors = new HashMap<>();
		for (int i=0;i<timeLine.size();i++) {
			colors.put(keySet.get(i), Colors.R_COLOR());
			System.out.print(colors.get(keySet.get(i)));

			int barCount = (int) Math.round((double)timeLine.get(i)*multiplier);
			for (int j=0;j<barCount;j++) {
				System.out.print("|");
			}

			System.out.print(Colors.END);
		}
		colors.put(keySet.get(timeLine.size()), Colors.R_COLOR());

		System.out.println();

		for (String key : keySet) {
			System.out.print(colors.get(key)+key+Colors.END+((keySet.get(keySet.size()-1)).equals(key) ? "" : " | "));
		}
		System.out.println();

		int tmpChar = 0;
		for (String key : keySet) {
			if (keySet.indexOf(key) >= timeLine.size()) continue;
			for (int i=0;i<key.length()/2;i++) System.out.print(" ");
			System.out.print(timeLine.get(keySet.indexOf(key))+"ms");
			for (int i=0;i<key.length()/2;i++) System.out.print(" ");
		}
		System.out.println();
	}
}
