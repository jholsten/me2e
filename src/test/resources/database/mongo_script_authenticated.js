conn = new Mongo("mongodb://user:123@localhost:27017");
db = conn.getDB("testdb")

db.company.insertMany([
    {
        id: NumberInt(1),
        name: 'Company A'
    },
    {
        id: NumberInt(2),
        name: 'Company B'
    }
])

db.employee.insertMany([
    {
        id: NumberInt(1),
        name: 'Employee A.1',
        company_id: NumberInt(1)
    },
    {
        id: NumberInt(2),
        name: 'Employee A.2',
        company_id: NumberInt(1)
    },
    {
        id: NumberInt(3),
        name: 'Employee B.1',
        company_id: NumberInt(2)
    },
])
