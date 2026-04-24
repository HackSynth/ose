import { PrismaClient, ExamSession, QuestionType, CaseAnswerType } from "@prisma/client";

const prisma = new PrismaClient();

const tree = [
  { name: "计算机组成与体系结构", children: ["数据表示", "计算机结构", "指令系统", "存储系统", "输入输出", "总线"] },
  { name: "操作系统", children: ["进程管理", "存储管理", "文件管理", "设备管理", "作业管理"] },
  { name: "数据结构与算法", children: ["线性表", "栈和队列", "树与二叉树", "图", "查找", "排序", "算法设计策略"] },
  { name: "软件工程", children: ["软件过程模型", "需求分析", "软件设计", "软件测试", "软件维护", "项目管理"] },
  { name: "数据库系统", children: ["关系数据库", "SQL语言", "数据库设计", "规范化理论", "事务与并发控制"] },
  { name: "计算机网络", children: ["OSI模型", "TCP/IP协议", "网络设备", "网络安全", "Internet应用"] },
  { name: "程序设计语言", children: ["编译与解释", "文法与语言", "程序设计范型"] },
  { name: "面向对象技术", children: ["面向对象基础", "UML", "设计模式"] },
  { name: "知识产权与标准化", children: ["知识产权基础", "软件保护", "标准化"] },
  { name: "数据流图与系统分析", children: ["数据流图绘制", "系统分析方法"] },
];

type SeedQuestion = {
  content: string;
  topic: string;
  difficulty: number;
  explanation: string;
  options: [string, string, string, string];
  answer: "A" | "B" | "C" | "D";
};

const questions: SeedQuestion[] = [
  { topic: "数据表示", difficulty: 2, content: "若机器字长为 8 位，采用补码表示，则十进制数 -1 的机器码为（ ）。", options: ["10000001", "11111111", "00000001", "01111111"], answer: "B", explanation: "补码中 -1 等于 0 减 1，8 位结果为 11111111。10000001 是原码形式，不能作为补码答案。" },
  { topic: "数据表示", difficulty: 3, content: "浮点数表示中，阶码的主要作用是（ ）。", options: ["表示数值的符号", "决定小数点的位置", "保存有效数字", "提高运算精度"], answer: "B", explanation: "浮点数由阶码和尾数组成，阶码决定小数点移动位置，也就是数的范围；尾数决定有效数字精度。" },
  { topic: "存储系统", difficulty: 2, content: "Cache 的设置主要是为了解决（ ）。", options: ["主存容量不足", "CPU 与主存速度不匹配", "外存速度过快", "总线宽度不足"], answer: "B", explanation: "Cache 位于 CPU 和主存之间，用高速小容量存储缓解 CPU 快、主存慢的速度差。" },
  { topic: "指令系统", difficulty: 3, content: "在指令格式中，操作码字段用于指出（ ）。", options: ["操作数地址", "寻址方式", "执行何种操作", "下一条指令地址"], answer: "C", explanation: "操作码说明指令要完成的操作，如加法、转移、读写等；地址码字段才用于指出操作数地址。" },
  { topic: "进程管理", difficulty: 3, content: "进程从运行态转为就绪态，通常是因为（ ）。", options: ["等待 I/O 完成", "时间片用完", "申请资源失败", "程序执行结束"], answer: "B", explanation: "时间片用完时进程并未阻塞，只是让出 CPU 回到就绪队列；等待 I/O 会进入阻塞态。" },
  { topic: "进程管理", difficulty: 4, content: "产生死锁的必要条件不包括（ ）。", options: ["互斥条件", "请求和保持", "可剥夺条件", "循环等待"], answer: "C", explanation: "死锁四个必要条件是互斥、请求保持、不可剥夺、循环等待。选项中的可剥夺条件恰好会破坏死锁。" },
  { topic: "存储管理", difficulty: 3, content: "分页存储管理中，逻辑地址通常由（ ）组成。", options: ["段号和段内地址", "页号和页内位移", "块号和磁道号", "文件号和记录号"], answer: "B", explanation: "分页把逻辑空间划分为页，地址分为页号与页内位移；分段地址才是段号和段内地址。" },
  { topic: "文件管理", difficulty: 2, content: "文件目录的主要作用是（ ）。", options: ["提高 CPU 运算速度", "实现文件名到物理位置的映射", "扩大内存容量", "保证进程互斥"], answer: "B", explanation: "目录项保存文件名、属性及存放位置等信息，使系统能根据文件名找到文件数据。" },
  { topic: "线性表", difficulty: 2, content: "在顺序表中访问第 i 个元素的时间复杂度为（ ）。", options: ["O(1)", "O(log n)", "O(n)", "O(n²)"], answer: "A", explanation: "顺序表连续存储，可通过首地址和下标直接计算元素地址，因此随机访问是常数时间。" },
  { topic: "栈和队列", difficulty: 2, content: "栈的插入和删除操作通常发生在（ ）。", options: ["栈底", "栈顶", "任意位置", "队头"], answer: "B", explanation: "栈是后进先出结构，入栈和出栈都在栈顶进行，这是其核心特征。" },
  { topic: "树与二叉树", difficulty: 3, content: "一棵具有 n 个结点的完全二叉树，其顺序存储中编号为 i 的结点左孩子编号为（ ）（若存在）。", options: ["i+1", "2i", "2i+1", "i/2"], answer: "B", explanation: "完全二叉树按层编号时，结点 i 的左孩子为 2i，右孩子为 2i+1，父结点为 floor(i/2)。" },
  { topic: "排序", difficulty: 3, content: "下列排序算法中，平均时间复杂度为 O(n log n) 且稳定的是（ ）。", options: ["快速排序", "堆排序", "归并排序", "简单选择排序"], answer: "C", explanation: "归并排序平均和最坏都是 O(n log n)，且在标准实现中稳定；快速排序和堆排序通常不稳定。" },
  { topic: "查找", difficulty: 2, content: "对有序顺序表进行折半查找，成功查找的时间复杂度为（ ）。", options: ["O(1)", "O(log n)", "O(n)", "O(n log n)"], answer: "B", explanation: "折半查找每次排除约一半元素，因此比较次数随 log₂n 增长。" },
  { topic: "算法设计策略", difficulty: 4, content: "用动态规划求解问题的关键条件通常是（ ）。", options: ["问题规模必须很小", "具有最优子结构和重叠子问题", "只能使用递归", "所有输入必须有序"], answer: "B", explanation: "动态规划通过保存子问题结果避免重复计算，适用于最优子结构和重叠子问题明显的场景。" },
  { topic: "软件过程模型", difficulty: 2, content: "瀑布模型最适合用于（ ）的软件项目。", options: ["需求稳定且明确", "需求频繁变化", "无法做文档", "只做原型不交付"], answer: "A", explanation: "瀑布模型强调阶段顺序和文档，适合需求明确、变化较少的项目；需求变化大时迭代模型更合适。" },
  { topic: "需求分析", difficulty: 3, content: "数据流图（DFD）主要用于描述系统的（ ）。", options: ["模块调用关系", "数据在系统中的流动和处理", "类之间继承关系", "数据库物理存储"], answer: "B", explanation: "DFD 关注外部实体、加工、数据流和数据存储，用于表达数据如何被处理和流转。" },
  { topic: "软件测试", difficulty: 3, content: "黑盒测试主要依据（ ）设计测试用例。", options: ["程序内部结构", "源代码语句覆盖", "需求规格说明", "编译器优化结果"], answer: "C", explanation: "黑盒测试不关心内部实现，主要根据功能需求、输入输出和业务规则设计用例。" },
  { topic: "软件设计", difficulty: 3, content: "高内聚、低耦合的软件设计目标有助于（ ）。", options: ["增加模块间依赖", "降低可维护性", "提高模块独立性", "减少功能数量"], answer: "C", explanation: "高内聚让模块职责集中，低耦合减少模块依赖，从而提高复用性、可测试性和可维护性。" },
  { topic: "关系数据库", difficulty: 2, content: "关系数据库中，表的一行通常称为（ ）。", options: ["属性", "元组", "域", "关键字"], answer: "B", explanation: "关系模型中，一行是一个元组，一列是属性，域是属性取值范围。" },
  { topic: "SQL语言", difficulty: 3, content: "SQL 中用于分组后筛选聚合结果的子句是（ ）。", options: ["WHERE", "GROUP BY", "HAVING", "ORDER BY"], answer: "C", explanation: "WHERE 在分组前筛选行，HAVING 在 GROUP BY 之后筛选分组聚合结果。" },
  { topic: "规范化理论", difficulty: 4, content: "若关系模式 R 中每个非主属性都完全函数依赖于候选键，则 R 至少满足（ ）。", options: ["1NF", "2NF", "3NF", "BCNF"], answer: "B", explanation: "2NF 要求在 1NF 基础上消除非主属性对候选键的部分函数依赖，即完全依赖于候选键。" },
  { topic: "事务与并发控制", difficulty: 3, content: "事务的 ACID 特性中，Isolation 指的是（ ）。", options: ["原子性", "一致性", "隔离性", "持久性"], answer: "C", explanation: "Isolation 是隔离性，强调并发事务之间互不干扰；Atomicity、Consistency、Durability 分别是原子性、一致性、持久性。" },
  { topic: "OSI模型", difficulty: 2, content: "OSI 七层模型中，路由选择主要发生在（ ）。", options: ["物理层", "数据链路层", "网络层", "应用层"], answer: "C", explanation: "网络层负责逻辑寻址与路由选择，典型协议包括 IP；数据链路层关注相邻节点帧传输。" },
  { topic: "TCP/IP协议", difficulty: 3, content: "TCP 协议提供的是（ ）传输服务。", options: ["无连接、不可靠", "面向连接、可靠", "广播式", "只支持单工"], answer: "B", explanation: "TCP 通过连接建立、确认、重传、流量控制等机制提供面向连接的可靠字节流服务。" },
  { topic: "网络安全", difficulty: 3, content: "数字签名主要用于实现（ ）。", options: ["数据压缩", "身份认证和不可否认", "提高传输速度", "隐藏文件大小"], answer: "B", explanation: "数字签名用私钥签名、公钥验证，可证明发送者身份并防止事后否认，同时能发现内容篡改。" },
  { topic: "编译与解释", difficulty: 2, content: "编译程序的主要任务是（ ）。", options: ["把高级语言程序翻译成目标程序", "管理数据库事务", "调度操作系统进程", "绘制数据流图"], answer: "A", explanation: "编译器将源程序翻译为目标代码，通常包括词法、语法、语义分析和代码生成等阶段。" },
  { topic: "UML", difficulty: 3, content: "UML 用例图主要描述（ ）。", options: ["对象间消息时序", "系统功能与参与者关系", "类的属性和方法", "数据库表结构"], answer: "B", explanation: "用例图从用户视角表达参与者与系统功能的关系，适合需求分析阶段沟通范围。" },
  { topic: "设计模式", difficulty: 4, content: "单例模式的主要意图是（ ）。", options: ["为对象动态添加职责", "保证一个类仅有一个实例并提供全局访问点", "将请求封装为对象", "定义对象间一对多依赖"], answer: "B", explanation: "单例模式控制实例创建，确保系统中只有一个实例，并提供统一访问入口。" },
  { topic: "知识产权基础", difficulty: 2, content: "软件著作权保护的核心对象通常是（ ）。", options: ["软件思想本身", "程序及其有关文档的表达", "硬件电路板", "算法的数学公式本身"], answer: "B", explanation: "著作权保护表达形式，如程序代码和文档，不保护抽象思想、方法或数学公式本身。" },
  { topic: "数据流图绘制", difficulty: 3, content: "在数据流图中，表示对数据进行加工处理的符号通常是（ ）。", options: ["外部实体", "数据存储", "加工", "数据流"], answer: "C", explanation: "DFD 的基本元素包括外部实体、加工、数据流和数据存储。加工表示输入数据被转换为输出数据的处理过程。" },
];


type SeedCaseQuestion = {
  number: number;
  title: string;
  topic: string;
  difficulty: number;
  background: string;
  figures?: unknown;
  subQuestions: Array<{
    subNumber: number;
    content: string;
    answerType: CaseAnswerType;
    referenceAnswer: string;
    score: number;
    explanation: string;
  }>;
};

const caseQuestions: SeedCaseQuestion[] = [
  {
    number: 1,
    title: "图书管理系统数据流图分析",
    topic: "数据流图绘制",
    difficulty: 3,
    background: "某高校图书馆拟建设图书管理系统。读者可通过系统查询馆藏图书、提交借书和还书请求，并查看个人借阅记录。馆员负责办理借还书、维护图书信息、处理逾期罚款。系统需要保存读者信息、图书信息、借阅记录和罚款记录。当读者提交借书请求时，系统检查读者状态、图书库存和是否存在逾期未还；条件满足则生成借阅记录并更新库存。还书时系统计算是否逾期，必要时生成罚款记录。管理员可定期统计热门图书和逾期情况。",
    figures: { description: "顶层 DFD 包含读者、馆员、管理员三个外部实体，以及借还书处理、馆藏维护、统计分析等加工。" },
    subQuestions: [
      { subNumber: 1, score: 3, answerType: CaseAnswerType.FILL_BLANK, content: "指出该系统的主要外部实体。", referenceAnswer: "读者、馆员、管理员", explanation: "外部实体是系统边界之外与系统交换数据的人或组织，本题包括读者、馆员和管理员。" },
      { subNumber: 2, score: 3, answerType: CaseAnswerType.FILL_BLANK, content: "指出至少三个数据存储。", referenceAnswer: "读者信息、图书信息、借阅记录、罚款记录", explanation: "数据存储对应系统长期保存的数据文件或表，如读者、图书、借阅和罚款记录。" },
      { subNumber: 3, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "补充借书处理中的关键数据流。", referenceAnswer: "借书请求、读者状态、图书库存、借阅记录、库存更新、借书结果", explanation: "借书加工需要输入请求并读取读者状态和库存，输出借阅记录、库存更新和办理结果。" },
      { subNumber: 4, score: 5, answerType: CaseAnswerType.DIAGRAM_FILL, content: "描述“办理还书”加工的子图分解。", referenceAnswer: "接收还书请求；读取借阅记录；计算逾期；更新借阅记录；生成罚款记录；返回还书结果", explanation: "子图分解应保持父图输入输出平衡，并体现还书、逾期判断、记录更新和罚款生成。" },
    ],
  },
  {
    number: 2,
    title: "电商订单管理数据库设计与 SQL",
    topic: "数据库设计",
    difficulty: 4,
    background: "某电商平台需要管理用户、商品、订单和支付信息。用户可以创建多个订单，一个订单包含多个商品条目，每个条目记录购买数量和成交单价。商品属于某个分类，商品库存随订单支付成功而减少。订单包含订单号、下单时间、状态、收货地址等属性；支付记录包含支付流水号、支付方式、金额和支付时间。平台希望支持查询某用户最近一个月的订单总金额，并统计各分类商品销量。",
    figures: { description: "ER 图包含用户、订单、订单明细、商品、分类、支付记录等实体。" },
    subQuestions: [
      { subNumber: 1, score: 3, answerType: CaseAnswerType.SHORT_ANSWER, content: "补充 ER 图中的主要联系及联系类型。", referenceAnswer: "用户与订单是一对多；订单与商品通过订单明细形成多对多；分类与商品是一对多；订单与支付记录是一对一或一对多", explanation: "联系类型由业务规则决定，订单明细用于拆解订单和商品之间的多对多关系。" },
      { subNumber: 2, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "给出核心关系模式并标注主键外键。", referenceAnswer: "User(userId PK); Product(productId PK, categoryId FK); Orders(orderId PK, userId FK); OrderItem(orderId FK, productId FK, quantity, price, PK(orderId, productId)); Payment(paymentId PK, orderId FK)", explanation: "关系模式应体现实体主键和联系外键，订单明细通常采用联合主键或独立明细编号。" },
      { subNumber: 3, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "写出查询某用户最近一个月已支付订单总金额的 SQL。", referenceAnswer: "SELECT SUM(amount) FROM Payment p JOIN Orders o ON p.orderId=o.orderId WHERE o.userId=? AND o.status='PAID' AND p.payTime>=date('now','-1 month');", explanation: "查询需要连接订单和支付记录，按用户、支付状态和时间范围筛选后聚合金额。" },
      { subNumber: 4, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "说明如何将订单明细关系规范化到 3NF。", referenceAnswer: "消除非主属性对非键属性的传递依赖；商品名称、分类名称不放在订单明细中，只保存 productId、quantity、price 等依赖于键的属性", explanation: "3NF 要求非主属性只依赖候选键，不依赖其他非主属性，冗余商品或分类信息应拆到独立表。" },
    ],
  },
  {
    number: 3,
    title: "在线教育平台 UML 建模",
    topic: "UML",
    difficulty: 3,
    background: "某在线教育平台支持学生选课、观看课程视频、提交作业和参加测验。教师可以创建课程、发布章节、布置作业、批改作业并查看学习数据。管理员负责审核教师资质和维护课程分类。系统需要记录学生学习进度，课程由多个章节组成，章节下包含视频和测验。学生提交作业后，教师给出成绩和评语。平台希望通过 UML 对需求、类结构和关键交互进行建模。",
    figures: { description: "用例图缺少部分参与者和用例；类图缺少 Course、Lesson、Assignment、Submission 之间的关系；序列图描述学生提交作业流程。" },
    subQuestions: [
      { subNumber: 1, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "补充用例图中的参与者和主要用例。", referenceAnswer: "参与者包括学生、教师、管理员；用例包括选课、观看视频、提交作业、参加测验、创建课程、发布章节、批改作业、审核教师", explanation: "用例图从参与者视角描述系统功能，需覆盖三类用户的核心目标。" },
      { subNumber: 2, score: 5, answerType: CaseAnswerType.DIAGRAM_FILL, content: "补充类图中的类和关系。", referenceAnswer: "Course 组合多个 Lesson；Lesson 包含 Video 和 Quiz；Teacher 创建 Course；Student 通过 Enrollment 选课；Assignment 产生 Submission；Submission 关联 Student 并包含 score 和 comment", explanation: "课程和章节是整体-部分关系，学生选课和作业提交适合抽象为关联类或独立实体。" },
      { subNumber: 3, score: 3, answerType: CaseAnswerType.SHORT_ANSWER, content: "补充学生提交作业序列图中的关键消息。", referenceAnswer: "Student -> UI 提交作业；UI -> AssignmentService 保存提交；AssignmentService -> Repository 写入 Submission；系统通知 Teacher；返回提交成功", explanation: "序列图应体现对象间按时间顺序传递的调用消息和返回结果。" },
      { subNumber: 4, score: 3, answerType: CaseAnswerType.FILL_BLANK, content: "类图中课程与章节更适合使用哪种关系？", referenceAnswer: "组合关系", explanation: "章节通常依附于课程生命周期，课程删除时章节也随之删除，适合组合。" },
    ],
  },
  {
    number: 4,
    title: "活动选择问题算法设计与分析",
    topic: "算法设计策略",
    difficulty: 4,
    background: "某会议中心一天内有 n 个活动申请，每个活动给出开始时间 s[i] 和结束时间 f[i]。同一会议室任意时刻只能安排一个活动，希望选择尽可能多的互不冲突活动。已知活动可按结束时间升序排序。现在需要设计一个算法输出最多可安排的活动集合，并分析复杂度。给定样例活动：(1,4)、(3,5)、(0,6)、(5,7)、(8,9)、(5,9)。",
    figures: { description: "伪代码包含 SortByFinishTime、lastFinish、selected 三个关键变量，部分条件留空。" },
    subQuestions: [
      { subNumber: 1, score: 5, answerType: CaseAnswerType.DIAGRAM_FILL, content: "补全贪心算法伪代码中的选择条件。", referenceAnswer: "按结束时间升序排序；若 s[i] >= lastFinish 则选择活动 i，并令 lastFinish = f[i]", explanation: "活动选择问题的经典贪心策略是每次选择结束最早且与已选活动兼容的活动。" },
      { subNumber: 2, score: 3, answerType: CaseAnswerType.FILL_BLANK, content: "给出算法时间复杂度。", referenceAnswer: "排序 O(n log n)，扫描 O(n)，总时间复杂度 O(n log n)", explanation: "主要开销在排序；排序后只需线性扫描一次。" },
      { subNumber: 3, score: 4, answerType: CaseAnswerType.SHORT_ANSWER, content: "用给定样例追踪算法选择过程。", referenceAnswer: "按结束时间排序后选择 (1,4)，跳过 (3,5)、(0,6)，选择 (5,7)，选择 (8,9)，共 3 个活动", explanation: "每次比较开始时间是否不早于上一个已选活动结束时间，满足才加入结果。" },
      { subNumber: 4, score: 3, answerType: CaseAnswerType.SHORT_ANSWER, content: "说明该贪心策略为什么可行。", referenceAnswer: "选择结束最早的兼容活动会给后续活动留下最多时间，可通过交换论证证明存在包含该选择的最优解", explanation: "贪心正确性通常通过贪心选择性质和最优子结构说明。" },
    ],
  },
  {
    number: 5,
    title: "报表导出功能的面向对象设计",
    topic: "设计模式",
    difficulty: 4,
    background: "某企业管理系统需要支持将销售报表导出为 PDF、Excel 和 HTML。不同格式的导出流程大致相同：读取报表数据、生成文档头、生成表体、生成页脚并输出文件，但每种格式的具体生成方式不同。系统希望后续可以方便增加 Word 格式，并尽量避免修改已有客户端代码。设计人员计划使用一种创建型或行为型设计模式来封装稳定流程和可变步骤。",
    figures: { description: "类图中有 AbstractReportExporter、PdfExporter、ExcelExporter、HtmlExporter 和 export() 模板方法。" },
    subQuestions: [
      { subNumber: 1, score: 3, answerType: CaseAnswerType.FILL_BLANK, content: "识别该场景最适合使用的设计模式。", referenceAnswer: "模板方法模式", explanation: "导出流程固定而部分步骤由子类实现，符合模板方法模式定义算法骨架、延迟步骤到子类的特点。" },
      { subNumber: 2, score: 4, answerType: CaseAnswerType.DIAGRAM_FILL, content: "补全类图中的关键类和继承关系。", referenceAnswer: "AbstractReportExporter 定义 export 模板方法和抽象步骤；PdfExporter、ExcelExporter、HtmlExporter 继承它并实现 generateHeader、generateBody、generateFooter", explanation: "抽象父类固定流程，具体子类实现格式差异。" },
      { subNumber: 3, score: 5, answerType: CaseAnswerType.SHORT_ANSWER, content: "填写 export() 方法的关键伪代码。", referenceAnswer: "data=loadData(); generateHeader(data); generateBody(data); generateFooter(data); writeFile();", explanation: "模板方法按固定顺序调用公共步骤和抽象步骤，客户端只调用统一的 export。" },
      { subNumber: 4, score: 3, answerType: CaseAnswerType.SHORT_ANSWER, content: "说明新增 Word 格式时如何扩展。", referenceAnswer: "新增 WordExporter 继承 AbstractReportExporter，实现各生成步骤；客户端通过工厂或配置选择新类，尽量不修改原有导出类", explanation: "模板方法配合多态符合开闭原则，新增格式主要通过新增子类完成。" },
    ],
  },
];

async function main() {
  console.log("Seeding knowledge points...");
  const topicByName = new Map<string, string>();

  for (const [parentIndex, parent] of tree.entries()) {
    const parentRecord = await prisma.knowledgePoint.upsert({
      where: { id: `kp-${parentIndex + 1}` },
      update: { name: parent.name, parentId: null, sortOrder: parentIndex + 1 },
      create: { id: `kp-${parentIndex + 1}`, name: parent.name, sortOrder: parentIndex + 1, description: `${parent.name}相关考点` },
    });
    topicByName.set(parent.name, parentRecord.id);

    for (const [childIndex, childName] of parent.children.entries()) {
      const child = await prisma.knowledgePoint.upsert({
        where: { id: `kp-${parentIndex + 1}-${childIndex + 1}` },
        update: { name: childName, parentId: parentRecord.id, sortOrder: childIndex + 1 },
        create: { id: `kp-${parentIndex + 1}-${childIndex + 1}`, name: childName, parentId: parentRecord.id, sortOrder: childIndex + 1, description: `${parent.name} · ${childName}` },
      });
      topicByName.set(childName, child.id);
    }
  }

  console.log("Seeding questions...");
  for (const [index, question] of questions.entries()) {
    const knowledgePointId = topicByName.get(question.topic);
    if (!knowledgePointId) throw new Error(`Missing topic: ${question.topic}`);
    const number = index + 1;
    const savedQuestion = await prisma.question.upsert({
      where: { year_session_questionNumber: { year: 2023, session: ExamSession.AM, questionNumber: number } },
      update: {
        content: question.content,
        type: QuestionType.CHOICE,
        difficulty: question.difficulty,
        explanation: question.explanation,
        knowledgePointId,
      },
      create: {
        content: question.content,
        type: QuestionType.CHOICE,
        difficulty: question.difficulty,
        year: 2023,
        session: ExamSession.AM,
        questionNumber: number,
        explanation: question.explanation,
        knowledgePointId,
      },
    });

    for (const [optionIndex, content] of question.options.entries()) {
      const label = String.fromCharCode(65 + optionIndex);
      await prisma.questionOption.upsert({
        where: { questionId_label: { questionId: savedQuestion.id, label } },
        update: { content, isCorrect: label === question.answer },
        create: { questionId: savedQuestion.id, label, content, isCorrect: label === question.answer },
      });
    }
  }
  console.log("Seeding case analysis questions...");
  for (const caseQuestion of caseQuestions) {
    const knowledgePointId = topicByName.get(caseQuestion.topic);
    if (!knowledgePointId) throw new Error(`Missing case topic: ${caseQuestion.topic}`);
    const question = await prisma.question.upsert({
      where: { year_session_questionNumber: { year: 2023, session: ExamSession.PM, questionNumber: caseQuestion.number } },
      update: {
        content: caseQuestion.title,
        type: QuestionType.CASE_ANALYSIS,
        difficulty: caseQuestion.difficulty,
        explanation: `案例分析题：${caseQuestion.title}`,
        knowledgePointId,
        options: { deleteMany: {} },
      },
      create: {
        content: caseQuestion.title,
        type: QuestionType.CASE_ANALYSIS,
        difficulty: caseQuestion.difficulty,
        year: 2023,
        session: ExamSession.PM,
        questionNumber: caseQuestion.number,
        explanation: `案例分析题：${caseQuestion.title}`,
        knowledgePointId,
      },
    });

    await prisma.caseScenario.deleteMany({ where: { questionId: question.id } });
    await prisma.caseScenario.create({
      data: {
        questionId: question.id,
        background: caseQuestion.background,
        figures: caseQuestion.figures ?? undefined,
        subQuestions: {
          create: caseQuestion.subQuestions.map((subQuestion) => ({
            subNumber: subQuestion.subNumber,
            content: subQuestion.content,
            answerType: subQuestion.answerType,
            referenceAnswer: subQuestion.referenceAnswer,
            score: subQuestion.score,
            explanation: subQuestion.explanation,
          })),
        },
      },
    });
  }


  console.log("Seeding mock exams...");
  const choiceExamQuestions = await prisma.question.findMany({
    where: { type: QuestionType.CHOICE },
    orderBy: [{ knowledgePointId: "asc" }, { questionNumber: "asc" }],
    take: 25,
    select: { id: true },
  });
  const caseExamQuestions = await prisma.question.findMany({
    where: { type: QuestionType.CASE_ANALYSIS },
    orderBy: { questionNumber: "asc" },
    take: 4,
    select: { id: true },
  });
  const examOne = await prisma.exam.upsert({
    where: { id: "exam-mock-am-1" },
    update: { title: "模拟卷一·上午选择题", type: "MOCK", session: "AM", timeLimit: 150, totalScore: 25 },
    create: { id: "exam-mock-am-1", title: "模拟卷一·上午选择题", type: "MOCK", session: "AM", timeLimit: 150, totalScore: 25 },
  });
  await prisma.examQuestion.deleteMany({ where: { examId: examOne.id } });
  await prisma.examQuestion.createMany({ data: choiceExamQuestions.map((question, index) => ({ examId: examOne.id, questionId: question.id, orderNumber: index + 1 })) });

  const examTwo = await prisma.exam.upsert({
    where: { id: "exam-mock-pm-1" },
    update: { title: "模拟卷一·下午案例分析", type: "MOCK", session: "PM", timeLimit: 150, totalScore: 60 },
    create: { id: "exam-mock-pm-1", title: "模拟卷一·下午案例分析", type: "MOCK", session: "PM", timeLimit: 150, totalScore: 60 },
  });
  await prisma.examQuestion.deleteMany({ where: { examId: examTwo.id } });
  await prisma.examQuestion.createMany({ data: caseExamQuestions.map((question, index) => ({ examId: examTwo.id, questionId: question.id, orderNumber: index + 1 })) });
  console.log(`Seeded ${tree.length} root topics, ${questions.length} choice questions, ${caseQuestions.length} case questions and 2 mock exams.`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });




