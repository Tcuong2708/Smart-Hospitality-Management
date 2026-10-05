// remove_class_fields.js
db = db.getSiblingDB('MayHotelDB');

db.getCollectionNames().forEach(c => {
    const res = db[c].updateMany({ _class: { $exists: true } }, { $unset: { _class: "" } });
    print(c + ": đã xóa _class khỏi " + res.modifiedCount + " documents");
});

// Re-export all_collections_schema.json
const cols = db.getCollectionNames();
const res = {};
cols.forEach(c => {
    const d = db[c].findOne({}, { _class: 0 });
    if (d) {
        delete d._class;
        res[c] = d;
    }
});

fs.writeFileSync('all_collections_schema.json', JSON.stringify(res, null, 2));
print("=== CẬP NHẬT HOÀN TẤT ===");
