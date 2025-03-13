CREATE TABLE recipes (
    id SERIAL PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    resource_name VARCHAR(255) NOT NULL,
    hunger FLOAT NOT NULL,
    energy INT NOT NULL
);

CREATE TABLE ingredients (
    id SERIAL PRIMARY KEY,
    recipe_id INT REFERENCES recipes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    percentage FLOAT NOT NULL
);

CREATE TABLE feps (
    id SERIAL PRIMARY KEY,
    recipe_id INT REFERENCES recipes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    value FLOAT NOT NULL
);