// export_all_collections.js
db = db.getSiblingDB('MayHotelDB');

const result = {};

db.getCollectionNames().forEach(c => {
    // Exclude database_sequences if needed, or include all
    const sampleDoc = db[c].findOne({}, { _class: 0 });
    if (sampleDoc) {
        result[c] = sampleDoc;
    }
});

print(JSON.stringify(result, null, 2));
