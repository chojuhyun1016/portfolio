INSERT INTO lect (lecturer, location, max_appl, time, content)
          VALUES ('조주현', '상암 유플러스 사옥 13층', 500, DATE_SUB(NOW(), INTERVAL 5 DAY), '스프링부트');

INSERT INTO lect (lecturer, location, max_appl, time, content)
          VALUES ('김기남', '마곡 유플러스 사옥 10층', 250, DATE_SUB(NOW(), INTERVAL 10 DAY), 'AWS');

INSERT INTO lect (lecturer, location, max_appl, time, content)
          VALUES ('이상협', '용산 유플러스 사옥 19층', 100, DATE_ADD(NOW(), INTERVAL 1 DAY), '쿠버네티스');

INSERT INTO lect (lecturer, location, max_appl, time, content)
          VALUES ('조민정', '평촌 유플러스 사옥 6층', 1000, DATE_ADD(NOW(), INTERVAL 2 DAY), 'C++');