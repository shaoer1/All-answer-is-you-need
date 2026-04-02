CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL DEFAULT '',
  nickname VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  kb_id VARCHAR(128) NOT NULL,
  session_name VARCHAR(128) NOT NULL,
  is_delete TINYINT NOT NULL DEFAULT 0,
  summary_text TEXT NOT NULL,
  summary_updated_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_session_user (user_id),
  CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  message_content LONGTEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_msg_session (session_id),
  KEY idx_msg_user (user_id),
  CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id),
  CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chunk_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  kb_id VARCHAR(128) NOT NULL,
  doc_id VARCHAR(128) NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  embedding_json LONGTEXT NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  last_accessed_at TIMESTAMP NOT NULL,
  KEY idx_chunk_scope (user_id, kb_id),
  UNIQUE KEY uq_chunk_hash (user_id, kb_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
