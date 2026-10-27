CREATE TABLE decisions (
 id VARCHAR(36) PRIMARY KEY,
 revision BIGINT NOT NULL,
 document CLOB NOT NULL,
 updated_at VARCHAR(40) NOT NULL
);
