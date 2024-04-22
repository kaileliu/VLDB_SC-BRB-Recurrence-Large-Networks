一.总结文章总体的逻辑：
给出一个解决最大最小度问题的传统近似算法，作为一个参考解，并且给出exact算法的baseline作为优化的对象
给出三个维度的解决策略去优化这个exact算法的搜索空间：
1.reducing方法：（1）删除R中不好的点 （2）加入R中好的点
2.上界预估：（1）Ud 加入C最最小度的点，较为粗暴 （2）Unr 将R中和C相连边最多的点加入C （3）Udc 将R中和C中度数最小点相连的点迭代加入C
3.branching 策略：（1）dominated vertex batch处理  （2）connection Score 思路和上界Udc一致
二.代码说明：
根据论文独立复现2021 VLDB知识图谱方向论文《Efficient Size-Bounded Community Search over Large Networks》，从最基本的数据结构开始一步步搭建实现方案，包括搜索空间优化，剪枝，上界计算的实现。
代码复现实现了给定查询节点等参数，返回optimal community的要求，在facebook数据集上查询时间低于500ms，适合在线查询使用。
