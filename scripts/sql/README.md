# SQL 初始化脚本

对应《数据库设计.md》v0.2。双库分工：MySQL 管事实，pgvector 管语义。

## MySQL（先执行）

```bash
mysql -uroot -p < mysql_init.sql
```

包含：全部 18 张业务表（公共字段 id/deleted/created_at/updated_at）+ 初始数据（admin 账号 + 6 条默认 sys_config）。

**枚举字段**：状态列一律存英文小写编码值（如 `ongoing` / `parsing` / `pending`），列宽统一 `varchar(32)`；Java 侧由各模块 `enums` 包的枚举经 `@EnumValue` 自动装载，两端对齐关系见《数据库设计.md》§0 全字段枚举清单。布尔语义字段（`deleted`/`enabled`/`top1_hit` 等）保持 tinyint 0/1。

⚠️ admin 初始密码为示例 BCrypt 值（明文 `123456`），上线前请重新生成替换。

## PostgreSQL + pgvector（后执行）

```bash
psql -U postgres -c "CREATE DATABASE guide_vec;"
psql -U postgres -d guide_vec -f pgvector_init.sql
```

包含：`kb_chunk_vec`（RAG 向量召回）与 `cluster_bucket_vec`（聚类锚点，feedback 直写），HNSW + 余弦距离，embedding 维度 1024 对应阿里 text-embedding-v3——**换 embedding 模型需同步修改维度并重建向量**。

连接参数写入 `backend/admin/src/main/resources/application-local.yml`（不提交）。
