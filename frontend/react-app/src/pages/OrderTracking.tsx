import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { orderApi, type Order } from '../api/client';

const API_BASE = '/api';

export default function OrderTracking() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [tracking, setTracking] = useState<{ currentLatitude: number; currentLongitude: number; estimatedArrivalMinutes: number; partnerName: string } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const fetchOrder = () => {
      orderApi.getOrder(id).then(res => {
        setOrder(res.data.data);
      }).finally(() => setLoading(false));

      fetch(`${API_BASE}/delivery/orders/${id}/tracking`)
        .then(r => r.json())
        .then(d => { if (d.success) setTracking(d.data); })
        .catch(() => {});
    };
    fetchOrder();
    const interval = setInterval(fetchOrder, 5000);
    return () => clearInterval(interval);
  }, [id]);

  const cancelOrder = async () => {
    if (!id) return;
    try {
      const res = await orderApi.cancelOrder(id);
      setOrder(res.data.data);
    } catch {
      alert('Failed to cancel order');
    }
  };

  if (loading) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-500">Loading order...</p></div>;
  if (!order) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-500">Order not found</p></div>;

  const statusSteps = ['PLACED', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED'];
  const currentIdx = statusSteps.indexOf(order.status);
  const canCancel = !['PICKED_UP', 'DELIVERED', 'CANCELLED', 'REFUNDED'].includes(order.status);

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="mx-auto max-w-7xl px-4 py-4 flex items-center justify-between">
          <div>
            <button onClick={() => navigate('/search')} className="text-orange-500 hover:text-orange-600 text-sm">&larr; Back to Search</button>
            <h1 className="text-2xl font-bold text-gray-900">Order #{order.id.substring(0, 8)}</h1>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8">
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-4">Order Status</h2>
          <div className="space-y-3">
            {statusSteps.map((step, i) => {
              const done = currentIdx >= i;
              const active = currentIdx === i;
              const cancelled = order.status === 'CANCELLED' && i > currentIdx;
              const refunded = order.status === 'REFUNDED' && i > currentIdx;
              return (
                <div key={step} className="flex items-center gap-3">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold
                    ${done || cancelled ? 'bg-green-500 text-white' : refunded ? 'bg-red-500 text-white' : active ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-500'}`}>
                    {done || cancelled ? '✓' : i + 1}
                  </div>
                  <span className={`text-sm font-medium ${done ? 'text-green-600' : active ? 'text-orange-600' : 'text-gray-400'}`}>
                    {step.replace('_', ' ')}
                  </span>
                </div>
              );
            })}
            {order.status === 'CANCELLED' && (
              <div className="flex items-center gap-3 mt-2">
                <div className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold bg-red-500 text-white">✕</div>
                <span className="text-sm font-medium text-red-600">CANCELLED</span>
              </div>
            )}
          </div>
        </div>

        {tracking && (
          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <h2 className="font-semibold text-gray-900 mb-3">Delivery Tracking</h2>
            <p className="text-sm text-gray-500">{tracking.partnerName}</p>
            {tracking.estimatedArrivalMinutes >= 0 ? (
              <p className="text-orange-600 font-semibold mt-1">ETA: {tracking.estimatedArrivalMinutes} min</p>
            ) : (
              <p className="text-gray-400 text-sm mt-1">Waiting for partner location...</p>
            )}
          </div>
        )}

        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-3">Order Details</h2>
          <div className="space-y-2">
            {order.items.map(item => (
              <div key={item.id} className="flex justify-between text-sm">
                <span>{item.itemName} x{item.quantity}</span>
                <span>₹{item.totalPrice.toFixed(2)}</span>
              </div>
            ))}
          </div>
          <div className="border-t mt-4 pt-4 grid grid-cols-2 gap-2 text-sm">
            <span className="text-gray-500">Subtotal</span><span className="text-right">₹{order.subtotal.toFixed(2)}</span>
            <span className="text-gray-500">Delivery Fee</span><span className="text-right">₹{order.deliveryFee.toFixed(2)}</span>
            <span className="text-gray-500">Tax</span><span className="text-right">₹{order.tax.toFixed(2)}</span>
            {order.discount > 0 && <><span className="text-green-600">Discount</span><span className="text-right text-green-600">−₹{order.discount.toFixed(2)}</span></>}
            <span className="font-semibold">Total</span><span className="text-right font-semibold">₹{order.totalAmount.toFixed(2)}</span>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-3">Status History</h2>
          <div className="space-y-2">
            {order.statusHistory.map(h => (
              <div key={h.id} className="flex justify-between text-sm">
                <span className="text-gray-700">{h.note}</span>
                <span className="text-gray-400">{new Date(h.createdAt).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>

        {canCancel && (
          <button
            onClick={cancelOrder}
            className="w-full bg-red-500 text-white py-2 rounded-lg hover:bg-red-600 transition-colors"
          >
            Cancel Order
          </button>
        )}
      </main>
    </div>
  );
}
