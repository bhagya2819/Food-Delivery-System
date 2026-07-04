CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS restaurant_index (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cuisine_type VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    rating DOUBLE PRECISION DEFAULT 0.0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_restaurant_index_location ON restaurant_index USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_restaurant_index_cuisine ON restaurant_index(cuisine_type);
CREATE INDEX IF NOT EXISTS idx_restaurant_index_city ON restaurant_index(city);
CREATE INDEX IF NOT EXISTS idx_restaurant_index_name ON restaurant_index USING GIN(to_tsvector('english', name));

CREATE TABLE IF NOT EXISTS menu_item_index (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurant_index(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    is_vegetarian BOOLEAN NOT NULL DEFAULT false,
    is_available BOOLEAN NOT NULL DEFAULT true,
    dietary_tags VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_menu_item_index_restaurant ON menu_item_index(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_index_search ON menu_item_index USING GIN(to_tsvector('english', name));
