根据论文独立复现2021 VLDB知识图谱方向论文《Efficient Size-Bounded Community Search over Large Networks》，从最基本的数据结构开始一步步搭建实现方案，包括搜索空间优化，剪枝，上界计算的实现。
代码复现实现了给定查询节点等参数，返回optimal community的要求，在facebook数据集上查询时间低于500ms，适合在线查询使用。
