CREATE TABLE company
(
    id   INT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE employee
(
    id         INT PRIMARY KEY,
    name       VARCHAR(255),
    company_id INT,
    FOREIGN KEY (company_id) REFERENCES company ON DELETE CASCADE
);

INSERT INTO company(id, name)
VALUES (1, 'Company A'),
       (2, 'Company B');

INSERT INTO employee(id, name, company_id)
VALUES (1, 'Employee A.1', 1),
       (2, 'Employee A.2', 1),
       (3, 'Employee B.1', 2);
