CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密码',
  nickname VARCHAR(64) NOT NULL COMMENT '昵称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识库ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  name VARCHAR(128) NOT NULL COMMENT '知识库名称',
  description TEXT COMMENT '知识库描述',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_name (user_id, name),
  INDEX idx_user_id (user_id),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  kb_id BIGINT NOT NULL COMMENT '知识库ID',
  session_name VARCHAR(128) NOT NULL COMMENT '会话名称',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
  summary_text TEXT NOT NULL COMMENT '会话摘要',
  summary_updated_at TIMESTAMP NULL COMMENT '摘要更新时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (user_id),
  INDEX idx_kb_id (kb_id),
  INDEX idx_created_at (created_at),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(32) NOT NULL COMMENT '角色：user/assistant',
  message_content TEXT NOT NULL COMMENT '消息内容',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_session_id (session_id),
  INDEX idx_user_id (user_id),
  INDEX idx_created_at (created_at),
  FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS chunk_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档块ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  kb_id BIGINT NOT NULL COMMENT '知识库ID',
  doc_id VARCHAR(128) NOT NULL COMMENT '文档ID',
  chunk_index INT NOT NULL COMMENT '分块索引',
  content TEXT NOT NULL COMMENT '分块内容',
  embedding_json TEXT NOT NULL COMMENT '向量JSON',
  content_hash VARCHAR(64) NOT NULL COMMENT '内容哈希',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后访问时间',
  UNIQUE KEY uk_user_kb_hash (user_id, kb_id, content_hash),
  INDEX idx_user_id (user_id),
  INDEX idx_kb_id (kb_id),
  INDEX idx_doc_id (doc_id),
  INDEX idx_content_hash (content_hash),
  INDEX idx_last_accessed_at (last_accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档块表';

CREATE TABLE IF NOT EXISTS chat_bubble_state (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '气泡状态ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  pair_id VARCHAR(128) NOT NULL COMMENT '问答对ID',
  pos_x DOUBLE NOT NULL DEFAULT 0 COMMENT 'X偏移',
  pos_y DOUBLE NOT NULL DEFAULT 0 COMMENT 'Y偏移',
  bubble_width DOUBLE NOT NULL DEFAULT 560 COMMENT '气泡宽度',
  hidden TINYINT NOT NULL DEFAULT 0 COMMENT '关闭状态：0-显示，1-关闭',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_session_pair (user_id, session_id, pair_id),
  INDEX idx_user_session (user_id, session_id),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话气泡状态表';
