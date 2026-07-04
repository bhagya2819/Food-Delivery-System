import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { cartApi, orderApi, type Cart } from '../api/client';

export default function Checkout() {
  const navigate = useNavigate();
  const [checkout, setCheckout] = useState<{ cart: Cart; taxAmount: number; finalTotal: number; canPlaceOrder: boolean; message: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);
  const [couponCode, setCouponCode] = useState('');

  useEffect(() => {
    cartApi.checkout().then(res => {
      setCheckout(res.data.data);
    }).finally(() => setLoading(false));
  }, []);

  const applyCoupon = async () => {
    if (!couponCode.trim()) return;
    try {
      await cartApi.applyCoupon(couponCode.trim().toUpperCase());
      const res = await cartApi.checkout();
      setCheckout(res.data.data);
      setCouponCode('');
    } catch {
      alert('Invalid coupon code');
    }
  };

  const removeCoupon = async () => {
    try {
      await cartApi.removeCoupon();
      const res = await cartApi.checkout();
      setCheckout(res.data.data);
    } catch {}
  };

  const placeOrder = async () => {
    setPlacing(true);
    try {
      const res = await orderApi.placeOrder({
        deliveryAddressId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      });
      navigate(`/orders/${res.data.data.id}`);
    } catch (e) {
      alert('Failed to place order');
    } finally {
      setPlacing(false);
    }
  };

  if (loading) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-500">Loading checkout...</p></div>;
  if (!checkout?.canPlaceOrder) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-500 text-lg">{checkout?.message || 'Cannot place order'}</p></div>;

  const { cart, taxAmount, finalTotal } = checkout;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="mx-auto max-w-7xl px-4 py-4">
          <button onClick={() => navigate(-1)} className="text-orange-500 hover:text-orange-600 text-sm">&larr; Back</button>
          <h1 className="text-2xl font-bold text-gray-900">Checkout</h1>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8">
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-4">Order Summary</h2>
          <div className="space-y-3">
            {cart.items.map(item => (
              <div key={item.itemId} className="flex justify-between">
                <span className="text-sm">{item.itemName} x{item.quantity}</span>
                <span className="text-sm">₹{item.totalPrice.toFixed(2)}</span>
              </div>
            ))}
          </div>
          <div className="border-t mt-4 pt-4 space-y-2">
            <div className="flex justify-between text-sm"><span>Subtotal</span><span>₹{cart.subtotal.toFixed(2)}</span></div>
            {cart.discount > 0 && <div className="flex justify-between text-sm text-green-600"><span>Discount</span><span>−₹{cart.discount.toFixed(2)}</span></div>}
            <div className="flex justify-between text-sm"><span>Delivery Fee</span><span>₹{cart.deliveryFee.toFixed(2)}</span></div>
            <div className="flex justify-between text-sm"><span>Tax (5%)</span><span>₹{taxAmount.toFixed(2)}</span></div>
            <div className="flex justify-between font-bold text-lg border-t pt-2"><span>Total</span><span>₹{finalTotal.toFixed(2)}</span></div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-3">Coupon</h2>
          {cart.couponCode ? (
            <div className="flex items-center justify-between">
              <span className="text-green-600 font-medium">{cart.couponCode}</span>
              <button onClick={removeCoupon} className="text-red-500 text-sm hover:underline">Remove</button>
            </div>
          ) : (
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Enter coupon code"
                value={couponCode}
                onChange={e => setCouponCode(e.target.value)}
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm"
              />
              <button onClick={applyCoupon} className="bg-gray-800 text-white px-4 py-2 rounded-lg text-sm hover:bg-gray-900">Apply</button>
            </div>
          )}
        </div>

        <button
          onClick={placeOrder}
          disabled={placing}
          className="w-full bg-orange-500 text-white py-3 rounded-lg text-lg font-semibold hover:bg-orange-600 disabled:opacity-50 transition-colors"
        >
          {placing ? 'Placing Order...' : `Place Order — ₹${finalTotal.toFixed(2)}`}
        </button>
      </main>
    </div>
  );
}
