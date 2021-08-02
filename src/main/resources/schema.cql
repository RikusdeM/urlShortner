-- CREATE KEYSPACE urls WITH REPLICATION = {
--     'class' : 'NetworkTopologyStrategy',
--     'datacenter1' : 1
--     };

CREATE TABLE urls.url
(
    shortened_url text PRIMARY KEY,
    original_url  text
);
INSERT INTO urls.url(shortened_url, original_url)
VALUES ('http://AcwMh6H3', 'www.google.com');
