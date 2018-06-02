# 灰度发布

#### 1. 目的

灰度发布是为了在升级过程中减少对于现有业务的影响，通过升级一台Executor先验证是否没有问题后，再逐步按照批次升级。

同时在升级过程中，不要影响原有其它Executor上作业的运行。

灰度发布要求域下至少有2台Executor。

#### 2. 什么时候需要灰度发布

- 变更了现有的作业代码
- 增加了新的作业

#### 3. 流程

1. 登录Saturn console，将域下的所有作业的“优先executor”设置成其中一台executor，假设是**executor_B**。

2. 在恰当时机（例如，通过saturn console 判断所有作业处于complete状态，或者作业都在executor_B执行），将**executor_A**下线。

3. 升级**executor_A。**包括替换作业包，和Executor的包（如果需要升级execuotor的话），并重启。在Console观察**executor_A**上线。

   *注：当前版本不支持作业热部署，所以必须要先停机，然后替换包。*

4. 在Saturn console，将域下的所有作业的“优先executor”改成新**executor_A**

5. 验证**executor_A**的运行结果。

6. 如果一切正常，升级**executor_B**。包括替换作业包，和Executor的包（如果需要升级execuotor的话），并重启。

7. 在Saturn console，将域下的所有作业的“优先executor”选项取消，让所有作业分片均衡分配到所有executor。

