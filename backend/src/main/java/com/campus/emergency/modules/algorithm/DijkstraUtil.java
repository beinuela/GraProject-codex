package com.campus.emergency.modules.algorithm;

import java.util.*;

public class DijkstraUtil {
    private static final Map<String, Map<String, Double>> CAMPUS_GRAPH = new HashMap<>();

    static {
        // 科学校区, 东风校区, 龙子湖校区, 中心物资周转区, 附属医院急救仓
        addEdge("科学校区", "东风校区", 12.5);
        addEdge("科学校区", "龙子湖校区", 26.0);
        addEdge("科学校区", "中心物资周转区", 5.0);
        addEdge("东风校区", "中心物资周转区", 8.5);
        addEdge("东风校区", "附属医院急救仓", 3.2);
        addEdge("龙子湖校区", "中心物资周转区", 22.0);
        addEdge("龙子湖校区", "附属医院急救仓", 15.0);
        // 让自身到自身等于0（在计算中会处理，但防呆）
        for (String node : List.of("科学校区", "东风校区", "龙子湖校区", "中心物资周转区", "附属医院急救仓")) {
            addEdge(node, node, 0.0);
        }
    }

    private static void addEdge(String u, String v, Double weight) {
        CAMPUS_GRAPH.computeIfAbsent(u, k -> new HashMap<>()).put(v, weight);
        CAMPUS_GRAPH.computeIfAbsent(v, k -> new HashMap<>()).put(u, weight);
    }

    public static Map<String, Double> calculateShortestPaths(String sourceId) {
        Map<String, Double> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (String nodeId : CAMPUS_GRAPH.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        
        if (!CAMPUS_GRAPH.containsKey(sourceId)) {
            distances.put(sourceId, 0.0);
            return distances; 
        }

        distances.put(sourceId, 0.0);
        pq.offer(new Node(sourceId, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.distance > distances.get(current.id)) continue;

            Map<String, Double> neighbors = CAMPUS_GRAPH.get(current.id);
            if (neighbors != null) {
                for (Map.Entry<String, Double> edge : neighbors.entrySet()) {
                    String neighborId = edge.getKey();
                    Double weight = edge.getValue();
                    Double newDist = current.distance + weight;

                    if (newDist < distances.getOrDefault(neighborId, Double.MAX_VALUE)) {
                        distances.put(neighborId, newDist);
                        pq.offer(new Node(neighborId, newDist));
                    }
                }
            }
        }
        return distances;
    }

    static class Node {
        String id;
        Double distance;
        public Node(String id, Double distance) { this.id = id; this.distance = distance; }
    }
}
