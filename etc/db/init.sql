CREATE TABLE recipes (
                         recipe_hash VARCHAR(64) PRIMARY KEY,
                         item_name VARCHAR(255) NOT NULL,
                         resource_name VARCHAR(255) NOT NULL,
                         hunger FLOAT NOT NULL,
                         energy INT NOT NULL
);

CREATE TABLE ingredients (
                             id SERIAL PRIMARY KEY,
                             recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE,  -- Внешний ключ
                             name VARCHAR(255) NOT NULL,
                             percentage FLOAT NOT NULL
);

CREATE TABLE feps (
                      id SERIAL PRIMARY KEY,
                      recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE,  -- Внешний ключ
                      name VARCHAR(255) NOT NULL,
                      value FLOAT NOT NULL
);