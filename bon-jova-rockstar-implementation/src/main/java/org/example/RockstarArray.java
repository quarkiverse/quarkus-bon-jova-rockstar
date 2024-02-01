package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RockstarArray {

    // Exposed for ease of testing
    Map<Object, Object> map;
    public List<Object> list;

    public RockstarArray() {
        map = new HashMap<>();
        list = new ArrayList<>();
    }

    public void add(Object thing) {
        list.add(thing);
    }

    public Object get(double i) {

        if (i < list.size()) {
            return list.get((int) i);
        } else {
            return null;
        }
    }

    public Object get(Object key) {
        // Rockstar only has a small number of types, so we don't need to check every Java type
        if (key instanceof Double) {
            return get(((Double) key).doubleValue());
        } else if (key instanceof String) {
            try {
                int num = Integer.parseInt((String) key);
                return list.get(num);
            } catch (NumberFormatException e) {
                // Not a problem, carry on
                return map.get(key);
            }
        } else {
            return map.get(key);
        }
    }

    private void ensureCapacity(int target) {
        for (int i = list.size(); i < target; i++) {
            // TODO it would be nice to use nothings
            list.add(null);
        }
    }

    public void add(double d, Object thing) {
        int i = (int) d;
        ensureCapacity(i);
        list.add(i, thing);
    }

    public void add(Object key, Object thing) {
        // Rockstar only has a small number of types, so we don't need to check every Java type
        if (key instanceof Double) {
            // Use our method so we can ensure capacity
            add(((Double) key).doubleValue(), thing);
        } else if (key instanceof String) {
            try {
                double num = Double.parseDouble((String) key);
                if (Math.round(num) == num) {
                    add(num, thing);
                } else {
                    map.put(key, thing);
                }
            } catch (NumberFormatException e) {
                // Not a problem, carry on
                map.put(key, thing);
            }
        } else {
            map.put(key, thing);
        }
    }

    public double size() {
        return list.size();
    }

    public Object pop() {
        if (list.size() > 0) {
            return list.remove(0);
        } else {
            return null;
        }
    }

    public void addAll(List inList) {
        list.addAll(inList);
    }

    public String join() {
        return join("");
    }

    public String join(String delimiter) {
        return list.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }
}
