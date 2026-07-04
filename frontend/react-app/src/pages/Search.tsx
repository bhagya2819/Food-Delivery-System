import { useState, useEffect, useCallback } from 'react';
import { searchApi, type Restaurant } from '../api/client';

export default function Search() {
  const [query, setQuery] = useState('');
  const [cuisine, setCuisine] = useState('');
  const [city, setCity] = useState('');
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const search = useCallback(async () => {
    setLoading(true);
    setSearched(true);
    try {
      const res = await searchApi.restaurants({ q: query || undefined, cuisine: cuisine || undefined, city: city || undefined });
      setRestaurants(res.data.data || []);
    } catch {
      setRestaurants([]);
    } finally {
      setLoading(false);
    }
  }, [query, cuisine, city]);

  useEffect(() => {
    search();
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') search();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="mx-auto max-w-7xl px-4 py-6 flex items-center justify-between">
          <h1 className="text-3xl font-bold text-gray-900">Food Delivery</h1>
          <button
            onClick={() => { localStorage.removeItem('token'); window.location.href = '/login'; }}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Logout
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8">
        <div className="mb-8 bg-white rounded-lg shadow p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <input
              type="text"
              placeholder="Search restaurants..."
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-orange-500"
            />
            <input
              type="text"
              placeholder="Cuisine (e.g. Italian, Indian)"
              value={cuisine}
              onChange={e => setCuisine(e.target.value)}
              onKeyDown={handleKeyDown}
              className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-orange-500"
            />
            <input
              type="text"
              placeholder="City"
              value={city}
              onChange={e => setCity(e.target.value)}
              onKeyDown={handleKeyDown}
              className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-orange-500"
            />
          </div>
          <button
            onClick={search}
            className="mt-4 bg-orange-500 text-white px-6 py-2 rounded-lg hover:bg-orange-600 transition-colors"
          >
            Search
          </button>
        </div>

        {loading && <p className="text-gray-500">Searching...</p>}

        {!loading && restaurants.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {restaurants.map((r) => (
              <div
                key={r.id}
                onClick={() => window.location.href = `/restaurant/${r.id}`}
                className="bg-white rounded-lg shadow hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
              >
                <div className="p-6">
                  <h3 className="text-xl font-semibold text-gray-900">{r.name}</h3>
                  <p className="text-sm text-orange-600 mt-1">{r.cuisineType}</p>
                  <div className="flex items-center justify-between mt-3">
                    <span className="text-sm text-gray-500">{r.city}, {r.state}</span>
                    <span className="text-sm font-medium text-yellow-600">★ {r.rating?.toFixed(1) || 'N/A'}</span>
                  </div>
                  {r.distanceKm != null && (
                    <p className="text-xs text-gray-400 mt-2">{r.distanceKm.toFixed(1)} km away</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && searched && restaurants.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">No restaurants found.</p>
            <p className="text-gray-400 mt-1">Try a different search term or cuisine.</p>
          </div>
        )}
      </main>
    </div>
  );
}
