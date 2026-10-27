-- Test data for the Postman API tests (postman/jasaks.postman_collection.json).
-- The API has no endpoints for creating places or bookers, so they are inserted here.
-- Safe to run more than once.

INSERT INTO places (place_id, place_name, time_zone, open_time, close_time, refund_hours, refund_percent)
VALUES (1000, 'Postman Test Library', 'America/New_York', '08:00', '22:00', 24, 80)
ON CONFLICT DO NOTHING;

INSERT INTO bookers (booker_id, name)
VALUES ('postman-booker-1', 'Postman Booker One'),
       ('postman-booker-2', 'Postman Booker Two')
ON CONFLICT DO NOTHING;
