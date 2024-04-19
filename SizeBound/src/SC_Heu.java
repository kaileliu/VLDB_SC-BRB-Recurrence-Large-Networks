import javafx.util.Pair;

import java.util.*;

public class SC_Heu {
    private HashMap<Integer, HashSet<Integer>> graph = new HashMap<Integer, HashSet<Integer>>();// 用邻接表来表示原图
    private int queryId;// 需要查询这个点的Size Bound Community
    private int l, h;// 社区的下界和上界
    private HashSet<Integer> H;// 初始的H
    private int k;// H构成的导出子图的k
    private boolean resultJudge;

    public SC_Heu(HashMap<Integer, HashSet<Integer>> graph, int l, int h) {
        this.graph = graph;
        this.l = l;
        this.h = h;
        this.k = 0;
        this.resultJudge = true;
    }

    public HashSet<Integer> getH() {
        return this.H;
    }

    public int getK() {
        return this.k;
    }

    public boolean isCommunityExit() {
        if(this.H.isEmpty() || this.H.contains(this.queryId)) this.resultJudge = true;
        return this.resultJudge;
    }

    public HashSet<Integer> query(int q){
        this.queryId = q;
        HashMap<Integer, HashSet<Integer>> S = new HashMap<Integer, HashSet<Integer>>();
        if(this.graph.get(this.queryId).size() >= this.h-1){
            getEgoNetWork(this.graph, this.queryId, S);
            while(S.size() >= this.l){
                Pair<Integer, Integer> mindegreePair = findMindDegree(S);
                if(S.size() <= h && mindegreePair.getValue() > this.k){
                    this.k = mindegreePair.getValue();
                    this.H = new HashSet<Integer>(S.keySet());
                }
                deleteOnePointInS(S, mindegreePair.getKey());
            }
        }else{
            // 初始化
            HashMap<Integer, HashSet<Integer>> RS = new HashMap<Integer, HashSet<Integer>>();
            HashMap<Integer, HashSet<Integer>> R = new HashMap<Integer, HashSet<Integer>>();
            initial(S, RS, R);
            while(S.size() < h){
                int v = getScoreP(S, RS);
                if(v == -1){
                    return this.H;
                }
                addPointToSFromR(S, RS, R, v);
                if(S.size() >= l){
                    Pair<Integer, Integer> mindegreePair = findMindDegree(S);
                    if(mindegreePair.getValue() > this.k){
                        this.k = mindegreePair.getValue();
                        this.H = new HashSet<Integer>(S.keySet());
                    }
                }
            }
        }
        return this.H;
    }

    private void initial(HashMap<Integer, HashSet<Integer>> S, HashMap<Integer, HashSet<Integer>> RS, HashMap<Integer, HashSet<Integer>> R){
        // 初始化S
        S.put(this.queryId, new HashSet<Integer>());
        // 初始化RS
        HashSet<Integer> pnbSetInR = this.graph.get(this.queryId);
        if(!pnbSetInR.isEmpty()){
            for(int i : pnbSetInR){
                RS.put(i, new HashSet<Integer>());
                RS.get(i).add(this.queryId);
            }
        }
        // 初始化R
        for(int i : this.graph.keySet()){
            R.put(i, new HashSet<Integer>(this.graph.get(i)));
        }
        // 对于queryId的邻居，更新它们的邻接表
        for(int pnb : R.get(this.queryId)){
            R.get(pnb).remove(this.queryId);
        }
        R.remove(this.queryId);// 取出queryId
    }

    private void addPointToSFromR(HashMap<Integer, HashSet<Integer>> S, HashMap<Integer, HashSet<Integer>> RS, HashMap<Integer, HashSet<Integer>> R, int p){
        // 更新R
        HashSet<Integer> pnbSetInR = R.get(p);
        if(!pnbSetInR.isEmpty()){
            for(int pnbInR : pnbSetInR){
                R.get(pnbInR).remove(p);
            }
        }
        R.remove(p);
        // 更新S
        S.put(p, new HashSet<Integer>());
        if(RS.containsKey(p)){
            HashSet<Integer> pnbSetInS = RS.get(p);
            for(int pnbInS : pnbSetInS){
                S.get(pnbInS).add(p);
            }
            S.get(p).addAll(pnbSetInS);
        }
        // 更新RS
        if(RS.containsKey(p)){
            RS.get(p).clear();
            RS.remove(p);
        }
        if (!pnbSetInR.isEmpty()) {
            for (int pnbInR : pnbSetInR) {
                if (!RS.containsKey(pnbInR)) {
                    RS.put(pnbInR, new HashSet<Integer>());
                }
                RS.get(pnbInR).add(p);
            }
        }
    }

    private int getScoreP(HashMap<Integer, HashSet<Integer>> S, HashMap<Integer, HashSet<Integer>> RS){
        double maxScore = -1;
        int p = -1;
        for(Map.Entry<Integer, HashSet<Integer>> entry : RS.entrySet()){
            double score = 0;
            HashSet<Integer> pnbSetInS = entry.getValue();
            for(int pnbInS : pnbSetInS){
                int degree = S.get(pnbInS).size();
                // 特殊情况，S中只有一个点
                if(degree == 0){
                    degree = 1;
                }
                score += 1.0/degree;
            }
            if(score > maxScore){
                maxScore = score;
                p = entry.getKey();
            }
        }
        return p;
    }

    // 查找一个图中的最小度
    private Pair<Integer, Integer> findMindDegree(HashMap<Integer, HashSet<Integer>> g){
        List<Map.Entry<Integer, HashSet<Integer>>> list = new ArrayList<Map.Entry<Integer, HashSet<Integer>>>(g.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, HashSet<Integer>>>() {
            @Override
            public int compare(Map.Entry<Integer, HashSet<Integer>> m1, Map.Entry<Integer, HashSet<Integer>> m2) {
                return m1.getValue().size()-m2.getValue().size(); // 升序排序
            }
        });
        Map.Entry<Integer, HashSet<Integer>> minDegree = list.get(0);
        return new Pair<Integer, Integer>(minDegree.getKey(), minDegree.getValue().size());
    }

    private void deleteOnePointInS(HashMap<Integer, HashSet<Integer>> S, int p){
        HashSet<Integer> pnbSet = S.get(p);
        for(int pnb : pnbSet){
            S.get(pnb).remove(p);
            // 删除孤立点
            if(S.get(pnb).isEmpty()){
                S.remove(pnb);
            }
        }
        S.remove(p);
    }

    // S是最终需要得到的egonetwork
    private void getEgoNetWork(HashMap<Integer, HashSet<Integer>> graph, int q, HashMap<Integer, HashSet<Integer>> S){
        HashSet<Integer> pnbSet = this.graph.get(q);
        S.put(q, new HashSet<Integer>(pnbSet));
        if(!pnbSet.isEmpty()){
            for(int pnb : pnbSet){
                if(!S.containsKey(pnb)){
                    S.put(pnb, new HashSet<Integer>());
                }
                S.get(pnb).add(q);
                HashSet<Integer> pnbSetOfPnb = this.graph.get(pnb);
                if(pnbSetOfPnb.isEmpty()) continue;
                // 如果它的邻居在q点的邻居集合里面，那么就添加这个点到NetWrok里面
                for(int pnbOfPnb : pnbSetOfPnb){
                    if(pnbSet.contains(pnbOfPnb)){
                        S.get(pnb).add(pnbOfPnb);
                    }
                }
            }
        }
    }
}