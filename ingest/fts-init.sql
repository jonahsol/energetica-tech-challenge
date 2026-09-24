USE enron;

ALTER TABLE message ADD FULLTEXT INDEX idx_message_fulltext (subject, body);