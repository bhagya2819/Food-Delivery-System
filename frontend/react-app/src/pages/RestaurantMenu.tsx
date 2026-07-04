import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { searchApi, cartApi, type MenuItem, type Cart } from '../api/client';

export default function RestaurantMenu() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [items, setItems] = useState<MenuItem[]>([]);
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [vegetarianOnly, setVegetarianOnly] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      searchApi.menu(id),
      cartApi.get().catch(() => ({ data: { data: null } })),
    ]).then(([menuRes, cartRes]) => {
      setItems(menuRes.data.data || []);
      setCart(cartRes.data.data);
    }).finally(() => setLoading(false));
  }, [id]);

  const addToCart = async (item: MenuItem) => {
    try {
      const res = await cartApi.addItem({
        itemId: item.id,
        restaurantId: id!,
        itemName: item.name,
        unitPrice: item.price,
        quantity: 1,
      });
      setCart(res.data.data);
    } catch (e) {
      console.error('Add to cart failed', e);
    }
  };

  const removeFromCart = async (itemId: string) => {
    try {
      const res = await cartApi.removeItem(itemId);
      setCart(res.data.data);
    } catch (e) {
      console.error('Remove from cart failed', e);
    }
  };

  const displayedItems = vegetarianOnly ? items.filter(i => i.vegetarian) : items;

  if (loading) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-500">Loading menu...</p></div>;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="mx-auto max-w-7xl px-4 py-4 flex items-center justify-between">
          <div>
            <button onClick={() => navigate('/search')} className="text-orange-500 hover:text-orange-600 text-sm">&larr; Back to Search</button>
            <h1 className="text-2xl font-bold text-gray-900">Menu</h1>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-7xl px-4 py-6 flex gap-6">
        <main className="flex-1">
          <div className="flex items-center justify-between mb-4">
            <p className="text-gray-600">{displayedItems.length} items</p>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={vegetarianOnly}
                onChange={e => setVegetarianOnly(e.target.checked)}
                className="rounded text-green-600"
              />
              Vegetarian only
            </label>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {displayedItems.map(item => {
              const inCart = cart?.items?.find(ci => ci.itemId === item.id);
              return (
                <div key={item.id} className="bg-white rounded-lg shadow p-4 flex justify-between items-start">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-gray-900">{item.name}</h3>
                      {item.vegetarian && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Veg</span>}
                    </div>
                    {item.description && <p className="text-sm text-gray-500 mt-1">{item.description}</p>}
                    <p className="text-orange-600 font-semibold mt-2">₹{item.price.toFixed(2)}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {inCart ? (
                      <>
                        <button onClick={() => removeFromCart(item.id)} className="bg-red-100 text-red-600 px-2 py-1 rounded text-sm">−</button>
                        <span className="text-sm font-medium">{inCart.quantity}</span>
                        <button onClick={() => addToCart(item)} className="bg-green-100 text-green-600 px-2 py-1 rounded text-sm">+</button>
                      </>
                    ) : (
                      <button onClick={() => addToCart(item)} className="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm hover:bg-orange-600">
                        Add
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </main>

        {cart && cart.items && cart.items.length > 0 && (
          <aside className="w-80 bg-white rounded-lg shadow p-4 h-fit sticky top-4">
            <h2 className="font-semibold text-gray-900 mb-3">Your Cart</h2>
            <div className="space-y-2 mb-4">
              {cart.items.map(item => (
                <div key={item.itemId} className="flex justify-between text-sm">
                  <span>{item.itemName} x{item.quantity}</span>
                  <span>₹{item.totalPrice.toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div className="border-t pt-2 space-y-1 text-sm">
              <div className="flex justify-between"><span>Subtotal</span><span>₹{cart.subtotal.toFixed(2)}</span></div>
              {cart.discount > 0 && <div className="flex justify-between text-green-600"><span>Discount</span><span>−₹{cart.discount.toFixed(2)}</span></div>}
              <div className="flex justify-between"><span>Delivery Fee</span><span>₹{cart.deliveryFee.toFixed(2)}</span></div>
              <div className="flex justify-between font-semibold border-t pt-2 mt-2"><span>Total</span><span>₹{cart.total.toFixed(2)}</span></div>
            </div>
            <button
              onClick={() => navigate('/checkout')}
              className="w-full mt-4 bg-orange-500 text-white py-2 rounded-lg hover:bg-orange-600 transition-colors"
            >
              Proceed to Checkout
            </button>
          </aside>
        )}
      </div>
    </div>
  );
}
