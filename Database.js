const { Pool } = require('pg');

const pool = new Pool({
    connectionString: process.env.DATABASE_URL ||  'postgresql://neondb_owner:npg_jXaJ6wECTm9U@ep-dry-recipe-ae52vkjl-pooler.c-2.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require',
    ssl: {
        require: true,
    }
});

module.exports = pool;