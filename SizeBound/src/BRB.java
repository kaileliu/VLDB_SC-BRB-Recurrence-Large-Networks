import java.util.*;
import java.io.*;
public class BRB {
    private int kDown, kUpper, k;// 分别表示k的下界，k的上界，最终需要得到的k
    private int l, h;// 这个社区中下界和上界
    private HashSet<Integer> H;// 表示的是最终得到的图
    private HashMap<Integer, HashSet<Integer>> C;// C图,HashMap<点,点集>
    private HashMap<Integer, HashSet<Integer>> R;// R图,HashMap<点,点集>
    private HashMap<Integer, HashSet<Integer>> RC;// R中的点在C中的邻居
    private HashMap<Integer, HashSet<Integer>> CR;// C中的点在R中的邻居
    private HashMap<Integer, HashSet<Integer>> gragh;// 用邻接表来表示原图
    private int queryId; // 需要查找的点的社区
    private int corenumberOfq;// queryId的度数
    private HashMap<Integer, HashSet<Integer>> coreNumber;// 里面存的是corenumber小于cn(queryId)的点
    private int count;
    private boolean resultJudge;

    public BRB(HashMap<Integer, HashSet<Integer>> gragh, int l, int h){
        this.gragh = gragh;
        this.k = 0;
        this.l = l;
        this.h = h;
        this.C = new HashMap<Integer, HashSet<Integer>>(); this.R = new HashMap<Integer, HashSet<Integer>>();
        this.RC = new HashMap<Integer, HashSet<Integer>>();this.CR = new HashMap<Integer, HashSet<Integer>>();
        this.coreNumber = new HashMap<Integer, HashSet<Integer>>();// HashMap<度数，对应点集>
        this.resultJudge = true;
        this.count = 1;
        this.H = new HashSet<Integer>();
    }

    public HashSet<Integer> getH(){
        return this.H;
    }

    // 得到k
    public int getK() {
        return this.k;
    }

    // 社区是否存在
    public boolean isCommunityExit() {
        return this.resultJudge;
    }

    public int query(int q){
        this.queryId = q;
        SC_Heu sc_heu = new SC_Heu(this.gragh, this.l, this.h);
        HashSet<Integer> tempH = sc_heu.query(this.queryId);
        this.resultJudge = sc_heu.isCommunityExit();
        if(!this.resultJudge) return -1;
        this.kDown = sc_heu.getK();
        findkCore();
        this.kUpper = this.corenumberOfq<(h-1)?this.corenumberOfq:(h-1);
        if(this.kDown == this.kUpper){
            this.H = tempH;
            writeToFile(H);
            this.k = this.kDown;
            return this.k;
        }
        if(this.kDown < this.kUpper){
            deletelessK(this.kDown);
            initial();// C、R、CR、RC进行初始化
            brb();
        }
        if(this.kDown > this.kUpper) return -1;
        this.k = this.kDown;
        if(this.H.isEmpty()) this.H = tempH;
        writeToFile(H);
        return this.k;
    }

    private void brb(){
        System.out.println("C:"+C+" kDown"+this.kDown+" C.size():"+this.C.size()+" H.size():"+this.H.size());
        if(H!=null) return;
        HashSet<Integer> arr[] = reductionRules();// res[0]表示在R中删除的点,res[1]表示在C中加入的点
        int size = C.size();
        if(size <= this.h && size >= this.l){
            // 得到C中的最小度
            List<Map.Entry<Integer, HashSet<Integer>>> entryList = new ArrayList<Map.Entry<Integer, HashSet<Integer>>>(C.entrySet());
            // 对List自定义排序，寻找最小度
            Collections.sort(entryList, new Comparator<Map.Entry<Integer, HashSet<Integer>>>() {
                @Override
                public int compare(Map.Entry<Integer, HashSet<Integer>> m1, Map.Entry<Integer, HashSet<Integer>> m2) {
                    return m1.getValue().size()-m2.getValue().size(); // 升序排序
                }
            });
            Map.Entry<Integer, HashSet<Integer>> minDegree = entryList.get(0);
            // 再根据最小度进行判断
            if(minDegree.getValue().size() > this.kDown) {
                this.kDown = minDegree.getValue().size();
                this.H = new HashSet<Integer>();
                this.H.addAll(C.keySet());
            }
        }

        if(this.C.size() < this.h && !R.isEmpty() && UB() > this.kDown){
            int v = getScoreP();// 从R中得到一点v
            // 如果v为-1表示从R中找不到可以与v相连的边
            if(v == -1) {
                // 在执行完reductionRules后恢复C和R
                recoverInC(arr);
                recoverR(arr);
                return;
            }
            HashSet<Integer> dominateP = findDominated(v);// 在R中寻找被v主导的的点
            addOnePointToCFromR(v);
            if(!dominateP.isEmpty()) {
                for (int i : dominateP) {
                    addOnePointToCFromR(i);
                    brb();
                    recoverOnePointToRFromC(i);// R中恢复一点,C中删除一点
                }
            }
            brb();
            removeOnePointInC(v);// C中删除一个点
            brb();
            recoverOnePointInR(v);// R中恢复一个点
        }
        // 在执行完reductionRules后恢复C和R
        recoverInC(arr);
        recoverR(arr);
    }

    //函数用于找到domin序列
    private HashSet<Integer> findDominated(int p){
        HashSet<Integer> dominated = new HashSet<Integer>();// 被p点主导的点构成的点集
        HashSet<Integer> pnbSetOfp = new HashSet<Integer>(this.R.get(p));// 加入在R中的邻居，这里要进行拷贝一下
        // 加入p点在C集合中的邻居
        if(this.RC.containsKey(p)){
            pnbSetOfp.addAll(this.RC.get(p));
        }
        pnbSetOfp.add(p);// 加入p点
        for(Map.Entry<Integer, HashSet<Integer>> pInR : this.R.entrySet()){
            int q = pInR.getKey();
            if(q == p) continue;// 如果是p点就直接跳过
            HashSet<Integer> pnbSetOfq = new HashSet<Integer>(pInR.getValue());// 加入在R中的邻居
            if(this.RC.containsKey(q)){
                pnbSetOfq.addAll(this.RC.get(q));
            }
            // 判断q的邻居集合是否是p的邻居集合
            if(pnbSetOfp.containsAll(pnbSetOfq)){
                dominated.add(q);
            }
        }
        return dominated;
    }

    // 在R中得到connection score最大的点
    private int getScoreP(){
        int p = -1;// 最终找的connection-score最大的的点
        double maxSocre = -1;
        // 在RC邻接表中寻找
        for(Map.Entry<Integer, HashSet<Integer>> pInR : RC.entrySet()){
            double score = 0;
            HashSet<Integer> pnbSetInC = pInR.getValue();// p在C中的邻居
            for(int pInC : pnbSetInC){
                int degree = this.C.get(pInC).size();
                if(degree == 0){
                    score = Integer.MAX_VALUE;
                    break;
                }
                score += 1.0/degree;
            }
            if(score > maxSocre){
                maxSocre = score;
                p = pInR.getKey();
            }
        }
        return p;
    }

    private int UB(){
        // 三个UB
        int Ud = Integer.MAX_VALUE, Unr = Integer.MAX_VALUE, Udc = Integer.MAX_VALUE;// 定义最大值
        // 求Ud
        for(Map.Entry<Integer, HashSet<Integer>> entry : this.C.entrySet()) {
            int p = entry.getKey();
            int degreeInC = entry.getValue().size();
            int degreeInR = 0;
            if(this.CR.containsKey(p)) degreeInR = this.CR.get(p).size();
            int d1 = degreeInC + degreeInR;
            int d2 = degreeInC + h - this.C.size();
            int temp = d1<d2?d1:d2;
            Ud = temp<Ud?temp:Ud;
        }
        // 求Udc和Unr
        // 首先要求C的最小度和最大度
        // 对List自定义排序，实现C中的点按都进行升序排序
        List<Map.Entry<Integer, HashSet<Integer>>> entryListOfC = new ArrayList<Map.Entry<Integer, HashSet<Integer>>>(this.C.entrySet());
        ascendingSort1(entryListOfC);
        int mindegreeInC = entryListOfC.get(0).getValue().size();// 得到最小度
        int  maxdegreeInC = entryListOfC.get(entryListOfC.size()-1).getValue().size();// 得到最大度
        // 对List进行自定义排序，对于R中的点在C中的邻居数进行降序排序
        List<Map.Entry<Integer, HashSet<Integer>>> entryListOfRC = new ArrayList<Map.Entry<Integer, HashSet<Integer>>>(this.RC.entrySet());
        descendingSort1(entryListOfRC);
        // 当C中只有一个顶点时，即只有queryId这一个点时，mindegreeInC和maxdegreeInC都将设置为1
        if(mindegreeInC == 0 && maxdegreeInC == 0) {
            mindegreeInC = 1;
            maxdegreeInC = 1;
        }
        Unr = restruct(entryListOfC, entryListOfRC, maxdegreeInC);
        int tempUnr;
        for(int i = mindegreeInC; i <= maxdegreeInC; i++) {
            tempUnr = restruct(entryListOfC, entryListOfRC, i);
            Udc = tempUnr < Udc?tempUnr:Udc;
            if(i == maxdegreeInC) {
                Unr = tempUnr;
            }
            if(Udc <= i+1) {
                break;
            }
        }
        // 返回找三个上界的最下值
        int res = (Ud<Unr?Ud:Unr);
        res = res<Udc?res:Udc;
        return res;
    }

    private int restruct(List<Map.Entry<Integer, HashSet<Integer>>> entryListOfC, List<Map.Entry<Integer, HashSet<Integer>>> entryListOfRC, int i){
        // 在C中找到度数小于i的点
        HashMap<Integer, Integer> tempC = new HashMap<Integer, Integer>();// TreeMap<点,度数>,
        for (Map.Entry<Integer, HashSet<Integer>> entry : entryListOfC) {
            int d = entry.getValue().size();
            int p = entry.getKey();
            if (d <= i) {
                tempC.put(p, d);
            } else {
                break;
            }
        }
        // 针对初始的时候C中只有一个点，扶她的度数为1
        if(tempC.size() == 1) {
            HashSet<Integer> Set = new HashSet<Integer>(tempC.keySet());
            for(int p : Set) {
                if(p == this.queryId) {
                    tempC.put(p, 1);
                }
            }
        }
        // 在R中寻找h-|C|个度数度数的所有点
        HashMap <Integer, Integer> tempR = new HashMap<Integer, Integer>();// TreeMap<点,度数>
        int number = this.h-C.size(), count = 0;
        for(Map.Entry<Integer, HashSet<Integer>> entry : entryListOfRC) {
            int d = entry.getValue().size();
            int p = entry.getKey();
            if(d > i){
                continue;
            }
            else if(count < number && d <= i){
                tempR.put(p, d);
                count++;
            }else if(count == number){
                break;
            }
        }
        List<Map.Entry<Integer, Integer>> sortInC = new ArrayList<Map.Entry<Integer, Integer>>(tempC.entrySet());// 浅拷贝
        ascendingSort2(sortInC);
        // 进行重构
        for(Map.Entry<Integer, Integer> pInR : tempR.entrySet()) {
            // 在C中取pInR.getValue()个点
            Iterator<Map.Entry<Integer,Integer>> it = sortInC.iterator();
            int getedPInC = 0;// 从C中取出点的数量
            while(it.hasNext()) {
                if(getedPInC == pInR.getValue()) {
                    break;
                }
                Map.Entry<Integer, Integer> pInC = (Map.Entry<Integer, Integer>) it.next();
                // 取出得点得度数加1
                tempC.put(pInC.getKey(), pInC.getValue()+1);// 对sortInC同时有影响
                getedPInC++;
            }
            ascendingSort2(sortInC);// 再次进行排序
        }
        return sortInC.get(0).getValue();
    }

    // 对List<Map.Entry<Integer, Integer>>结构根据entry.getValue()来进行升序排序
    private void ascendingSort2(List<Map.Entry<Integer, Integer>> list){
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>(){
            @Override
            public int compare(Map.Entry<Integer, Integer> m1, Map.Entry<Integer, Integer> m2) {
                return m1.getValue()-m2.getValue(); // 升序排序
            }
        });
    }

    // 对List<Map.Entry<Integer, HashSet<Integer>>>结构根据HashSet的大小进行降序排序
    private void descendingSort1(List<Map.Entry<Integer, HashSet<Integer>>> list){
        Collections.sort(list, new Comparator<Map.Entry<Integer, HashSet<Integer>>>() {
            @Override
            public int compare(Map.Entry<Integer, HashSet<Integer>> m1, Map.Entry<Integer, HashSet<Integer>> m2) {
                return m2.getValue().size()-m1.getValue().size(); // 降序排序
            }
        });
    }

    // 对List<Map.Entry<Integer, HashSet<Integer>>>结构根据HashSet的大小进行升序排序
    private void ascendingSort1(List<Map.Entry<Integer, HashSet<Integer>>> list){
        Collections.sort(list, new Comparator<Map.Entry<Integer, HashSet<Integer>>>() {
            @Override
            public int compare(Map.Entry<Integer, HashSet<Integer>> m1, Map.Entry<Integer, HashSet<Integer>> m2) {
                return m1.getValue().size()-m2.getValue().size(); // 升序排序
            }
        });
    }

    private void recoverInC(HashSet<Integer> arr[]){
        // res[0]表示在R中删除的点,res[1]表示在C中加入的点
        HashSet<Integer> pnbSetInC = arr[1];
        if(pnbSetInC.isEmpty()) return;
        for(int pnbInC : pnbSetInC){
            recoverOnePointToRFromC(pnbInC);
        }
    }

    private void recoverR(HashSet<Integer> arr[]){
        // res[0]表示在R中删除的点,res[1]表示在C中加入的点
        HashSet<Integer> pnbSetInR = arr[0];
        if(pnbSetInR.isEmpty()) return;
        for(int pnbInR : pnbSetInR){
            recoverOnePointInR(pnbInR);
        }
    }

    private void recoverOnePointInC(int p){
        // 更新C
        HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.gragh.get(p));
        pnbSetInC.retainAll(this.C.keySet());
        this.C.put(p, new HashSet<Integer>(pnbSetInC));
        // 更新CR
        HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.gragh.get(p));
        pnbSetInR.retainAll(this.R.keySet());
        if(!pnbSetInR.isEmpty()){
            this.CR.put(p, new HashSet<Integer>(pnbSetInR));
        }
        // 更新RC
        if(!pnbSetInR.isEmpty()){
            for(int pnbInR : pnbSetInR){
                if(!this.RC.containsKey(pnbInR)){
                    RC.put(pnbInR, new HashSet<Integer>());
                }
                RC.get(pnbInR).add(p);
            }
        }
    }

    // 从C中移除一个点
    private void removeOnePointInC(int p){
        // 更新C
        HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.C.get(p));
        if(!pnbSetInC.isEmpty()){
            for(int pnb : pnbSetInC){
                this.C.get(pnb).remove(p);
            }
        }
        this.C.get(p).clear();
        this.C.remove(p);
        // 更新RC
        if(this.CR.containsKey(p)){
            HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.CR.get(p));
            for(int pnbInR : pnbSetInR){
                this.RC.get(pnbInR).remove(p);
                if(this.RC.get(pnbInR).isEmpty()){
                    this.RC.remove(pnbInR);
                }
            }
        }
        // 更新CR
        if(this.CR.containsKey(p)){
            this.CR.get(p).clear();
            this.CR.remove(p);
        }

    }

    // 从R中添加一个点
    private void recoverOnePointInR(int p){
        // 更新R
        HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.gragh.get(p));
        pnbSetInR.retainAll(this.R.keySet());
        this.R.put(p, new HashSet<Integer>(pnbSetInR));
        // 交换
        if(!pnbSetInR.isEmpty()){
            for(int pnbInR : pnbSetInR){
                this.R.get(pnbInR).add(p);
            }
        }
        // 更新RC
        HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.gragh.get(p));
        pnbSetInC.retainAll(this.C.keySet());
        if(!pnbSetInC.isEmpty()){
            this.RC.put(p, new HashSet<Integer>(pnbSetInC));
        }
        // 更新CR
        if(!pnbSetInC.isEmpty()){
            for(int pnb : pnbSetInC){
                if(!this.CR.containsKey(pnb)){
                    this.CR.put(pnb, new HashSet<Integer>());
                }
                this.CR.get(pnb).add(p);
            }
        }
    }

    // 从R中取一点添加到C中
    private void addOnePointToCFromR(int p){
        // 更新R
        HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.R.get(p));
        if(!pnbSetInR.isEmpty()) {
            for (int pnb : pnbSetInR) {
                this.R.get(pnb).remove(p);
            }
        }
        this.R.get(p).clear();
        this.R.remove(p);
        // 更新C
        this.C.put(p, new HashSet<Integer>());
        if(this.RC.containsKey(p)){
            HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.RC.get(p));
            this.C.get(p).addAll(pnbSetInC);
            // 交换
            for(int pnb : pnbSetInC){
                this.C.get(pnb).add(p);
            }
        }
        // 更新CR
        if(!pnbSetInR.isEmpty()){
            this.CR.put(p, new HashSet<Integer>(pnbSetInR));
        }
        if(RC.containsKey(p)){
            HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.RC.get(p));
            for(int pnbInC : pnbSetInC){
                CR.get(pnbInC).remove(p);
                if(CR.get(pnbInC).isEmpty()){
                    this.CR.remove(pnbInC);
                }
            }
        }
        // 更新RC
        if(this.RC.containsKey(p)) {
            this.RC.get(p).clear();
            this.RC.remove(p);
        }
        if (!pnbSetInR.isEmpty()) {
            for (int pnbInR : pnbSetInR) {
                if (!this.RC.containsKey(pnbInR)) {
                    this.RC.put(pnbInR, new HashSet<Integer>());
                }
                this.RC.get(pnbInR).add(p);
            }
        }
    }

    // 从C中取除一点加入到R中
    private void recoverOnePointToRFromC(int p){
        // 更新C
        HashSet<Integer> pnbSetInC = new HashSet<Integer>(this.C.get(p));
        if(!pnbSetInC.isEmpty()) {
            for (int pnb : pnbSetInC) {
                this.C.get(pnb).remove(p);
            }
        }
        this.C.get(p).clear();
        this.C.remove(p);
        // 更新R
        this.R.put(p, new HashSet<Integer>());
        if(this.CR.containsKey(p)){
            HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.CR.get(p));
            this.R.get(p).addAll(pnbSetInR);
            // 交换
            for(int pnbInR : pnbSetInR){
                this.R.get(pnbInR).add(p);
            }
        }

        // 更新RC
        if(!pnbSetInC.isEmpty()){
            this.RC.put(p, new HashSet<Integer>(pnbSetInC));
        }
        // 更新p点在C中的邻居
        if(this.CR.containsKey(p)){
            HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.CR.get(p));
            for(int pnbInR : pnbSetInR){
                RC.get(pnbInR).remove(p);
                if(RC.get(pnbInR).isEmpty()){
                    RC.remove(pnbInR);
                }
            }
        }

        // 更新CR
        if(CR.containsKey(p)){
            this.CR.get(p).clear();
            this.CR.remove(p);
        }
        // 更新p点在R中的邻居
        if(!pnbSetInC.isEmpty()){
            for(int pnbInC : pnbSetInC){
                if(!this.CR.containsKey(pnbInC)){
                    this.CR.put(pnbInC, new HashSet<Integer>());
                }
                this.CR.get(pnbInC).add(p);
            }
        }

    }

    // R中删除一个点
    private void removeOnePointInR(int p){
        // 对R更新
        HashSet<Integer> pnbSetInR = new HashSet<Integer>(this.R.get(p));
        if(!pnbSetInR.isEmpty()){
            for(int pnbInR : pnbSetInR){
                this.R.get(pnbInR).remove(p);
            }
        }
        this.R.get(p).clear();
        this.R.remove(p);
        // 对CR进行更新
        if(this.RC.containsKey(p)){
            for(int pnbInC : this.RC.get(p)){
                this.CR.get(pnbInC).remove(p);
            }
        }
        // 对RC进行更新
        if(RC.containsKey(p)){
            RC.get(p).clear();
            RC.remove(p);
        }
    }

    // 执行reductionRules规则
    private HashSet<Integer>[] reductionRules(){
        HashSet<Integer> res[] = new HashSet[2];// res[0]表示在R中删除的点,res[1]表示从R中向C中添加点
        HashSet<Integer> removedPointsFromR = new HashSet<Integer>();
        // 规则1：求R中所有点在C和R并集的度数，和在C中的度数
        HashSet<Integer> keySet = new HashSet<>(this.R.keySet());
        for(int p : keySet){
            int degreeInR = this.R.get(p).size();
            int degreeInC = 0;
            if(this.RC.containsKey(p)){
                degreeInC = this.RC.get(p).size();
            }
            int degree1 = degreeInR+degreeInC;
            int degree2 = degreeInC+h-this.C.size()-1;
            if(degree1 <= this.kDown || degree2 <= this.kDown){
                removedPointsFromR.add(p);
                removeOnePointInR(p);
            }
        }
        keySet.clear();
        keySet = new HashSet<Integer>(this.R.keySet());
        // 规则2：求一个点到C中的点的最长距离
        for(int p : keySet){
            if(!RC.containsKey(p)) continue;
            HashSet<Integer> anchorSet = this.RC.get(p);
            // 将点分为两个集合,pnbSet和unvisited
            HashSet<Integer> temp = new HashSet<Integer>();
            temp.addAll(anchorSet);
            HashSet<Integer> unvisited = new HashSet<Integer>();
            unvisited.addAll(this.C.keySet());
            unvisited.removeAll(temp);
            HashSet<Integer> pnbSet = new HashSet<Integer>();
            // 初始化pnbSet
            pnbSet.addAll(temp);
            for(int i : temp){
                pnbSet.addAll(this.C.get(i));
            }
            // 寻找最长距离
            int dist = 1;
            temp.clear();
//            System.out.println("551this.RC.get(p):"+this.RC.get(p)+"p:"+p);
//            System.out.println("553this.C.get(p):"+this.C.get(p));
            while(!unvisited.isEmpty()){
                // 检擦没有访问过的点和被访问的点之间是否有边相连
                Iterator <Integer> pInUnVisSet = unvisited.iterator();
                while(pInUnVisSet.hasNext()){
                    int pInUnVis = pInUnVisSet.next();
                    if(pnbSet.contains(pInUnVis)){
                        temp.add(pInUnVis);
                        pInUnVisSet.remove();
                    }
                    // 清空孤立点
                    if(this.C.get(pInUnVis).isEmpty()) {
                        pInUnVisSet.remove();
                        dist = Integer.MAX_VALUE;
                        break;
                    }
                }
                dist++;
                for(int anchor : temp){
                    pnbSet.addAll(this.C.get(anchor));
                }
                temp.clear();
            }
            // 求n(k, D)
            int K = this.kDown+1, n;
            if((dist >= 1 && dist <= 2) || K ==1) {
                n = K + dist;
            }else {
                n = K + dist + 1 + (dist/3)*(K-2);
            }
            if(n > h){
                removedPointsFromR.add(p);
                removeOnePointInR(p);
            }
        }
        // 规则3:在C中加入点
        HashSet<Integer> addedPointsInC = new HashSet<Integer>();
        // 遍历C中每个点的度数
        HashSet<Integer> pSetInC = new HashSet<Integer>(this.C.keySet());
        for(int p : pSetInC){
            int degreeInC = this.C.get(p).size();
            int degreeInR = 0;
            if(CR.containsKey(p)){
                degreeInR = this.CR.get(p).size();
            }
            if(degreeInR+degreeInC == this.kDown+1){
                if(CR.containsKey(p)){
                    Iterator pnbSetInR = this.CR.get(p).iterator();
                    while(pnbSetInR.hasNext()){
                        int pnbInR = (int)pnbSetInR.next();
                        addedPointsInC.add(pnbInR);
                    }
                }
            }
        }
        // 向C中加点
        for(int p : addedPointsInC){
            addOnePointToCFromR(p);
        }
        res[0] = removedPointsFromR;res[1] = addedPointsInC;
        return res;
    }

    private void initial(){
        // 对C进行初始化
        this.C.put(this.queryId, new HashSet<Integer>());
        // 对R进行初始化
        for(int i : this.gragh.keySet()){
            R.put(i, new HashSet<Integer>(this.gragh.get(i)));
        }
        // 对于queryId的邻居，更新它们的邻接表
        for(int pnb : this.R.get(this.queryId)){
            R.get(pnb).remove(this.queryId);
        }
        R.remove(this.queryId);// 取出queryId
        // 对CR进行初始化
        this.CR.put(queryId, new HashSet<Integer>(this.gragh.get(this.queryId)));
        // 对RC进行初始化
        HashSet<Integer> pnbSet = this.gragh.get(this.queryId);
        // 对于queryId在原图中的每个邻居都建立键值对
        for(int  i : pnbSet){
            if(!RC.containsKey(i)){
                RC.put(i, new HashSet<Integer>());
            }
            RC.get(i).add(this.queryId);
        }
    }

    private void findkCore(){
        HashMap<Integer, HashSet<Integer>> copyG = new HashMap<Integer, HashSet<Integer>>();
        // 对原图进行深拷贝
        for(Map.Entry<Integer, HashSet<Integer>> entry : this.gragh.entrySet()) {
            copyG.put(entry.getKey(), new HashSet<Integer>(entry.getValue()));
        }
        Queue<Integer> deleteQ = new LinkedList<Integer>();// 待删除队列
        int k = 1;// k从1开始
        // 删除度数小于k的所有点，k不断的加1
        for(; !copyG.isEmpty();k++) {
            // 遍历每个点，观察每个点的度数是否小于k,小于k则加入待删除队列中
            for(int i : copyG.keySet()) {
                if(copyG.get(i).size() < k) {
                    deleteQ.add(i);
                }
            }
            // 更新待删除队列中的点的邻居的邻居
            while(!deleteQ.isEmpty()) {
                int p = (int) deleteQ.poll();
                // 限制条件：如果p点已经被删除了，在待删除队列中删除这个节点，直接进入下一个循环
                if(!copyG.keySet().contains(p)) {
                    copyG.remove(p);
                    continue;
                }
                // 更新邻居
                for(int pnbId : copyG.get(p)) {
                    copyG.get(pnbId).remove(p);
                    // 如果删除一个点后，它的度数小于k，将他加入到待删除队列中
                    if(copyG.get(pnbId).size() < k) {
                        deleteQ.add(pnbId);
                    }
                }
                // 如果要删除的点就是queryId，那么就记录此时queryId的corenumber
                if(p == this.queryId) {
                    this.corenumberOfq= k-1;
                }
                // 再向copyG中删除p点，同时向coreNumber中添加相应的corenumber对应的点
                copyG.remove(p);// copyG中删除p点
                if(!this.coreNumber.containsKey(k-1)) {
                    this.coreNumber.put(k-1, new HashSet<Integer>());
                }
                this.coreNumber.get(k-1).add(p);// coreNumber中添加corenumber对应的点
            }
        }
    }

    private void writeToFile(HashSet<Integer> H) {
        String filename = new String(".\\SizeBoundResult\\BRB-result\\querId"+this.queryId+"下界"+this.l+"上界"+this.h+"k为"+this.kDown+"\\第"+this.count+"个社区.txt");
        File fw = new File(filename);
        File fileParent = fw.getParentFile();
        if(!fileParent.exists()){
            fileParent.mkdirs();
        }
        try {
            fw.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fw));
            for(int i : H) {
                bos.write(String.valueOf(i).getBytes());
                bos.write(' ');
            }
            bos.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    // 从G中删除度数小于等于k的点
    private void deletelessK(int k){
        for(int corenumber : this.coreNumber.keySet()){
            if(corenumber <= k){
                // 找打需要删除的点
                for(int p : this.coreNumber.get(corenumber)){
                    // 首先更新原图中p的邻居的邻居
                    for(int pnbId : this.gragh.get(p)){
                        this.gragh.get(pnbId).remove(p);
                    }
                    // 然后原图中删除p点
                    this.gragh.get(p).clear();
                    this.gragh.remove(p);
                }
            }
        }
    }
}