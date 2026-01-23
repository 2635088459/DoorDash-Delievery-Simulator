import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useCartStore = create(
  persist(
    (set, get) => ({
      // 购物车商品列表
      items: [],
      // 当前餐厅ID（购物车只能包含同一家餐厅的商品）
      restaurantId: null,
      restaurantName: null,

      // 添加商品到购物车
      addItem: (item, restaurantId, restaurantName) => {
        const { items, restaurantId: currentRestaurantId } = get();

        // 如果添加的商品来自不同餐厅，清空购物车
        if (currentRestaurantId && currentRestaurantId !== restaurantId) {
          set({
            items: [{ ...item, quantity: 1 }],
            restaurantId,
            restaurantName,
          });
          return;
        }

        // 检查商品是否已在购物车中
        const existingItem = items.find(i => i.id === item.id);
        
        if (existingItem) {
          // 增加数量
          set({
            items: items.map(i =>
              i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i
            ),
          });
        } else {
          // 添加新商品
          set({
            items: [...items, { ...item, quantity: 1 }],
            restaurantId,
            restaurantName,
          });
        }
      },

      // 减少商品数量
      decreaseItem: (itemId) => {
        const { items } = get();
        const existingItem = items.find(i => i.id === itemId);

        if (existingItem && existingItem.quantity > 1) {
          set({
            items: items.map(i =>
              i.id === itemId ? { ...i, quantity: i.quantity - 1 } : i
            ),
          });
        } else {
          // 数量为1时，移除商品
          get().removeItem(itemId);
        }
      },

      // 移除商品
      removeItem: (itemId) => {
        const { items } = get();
        const newItems = items.filter(i => i.id !== itemId);
        
        // 如果购物车为空，清除餐厅信息
        if (newItems.length === 0) {
          set({
            items: [],
            restaurantId: null,
            restaurantName: null,
          });
        } else {
          set({ items: newItems });
        }
      },

      // 更新商品数量
      updateQuantity: (itemId, quantity) => {
        const { items } = get();
        
        if (quantity <= 0) {
          get().removeItem(itemId);
          return;
        }

        set({
          items: items.map(i =>
            i.id === itemId ? { ...i, quantity } : i
          ),
        });
      },

      // 清空购物车
      clearCart: () => {
        set({
          items: [],
          restaurantId: null,
          restaurantName: null,
        });
      },

      // 获取商品数量
      getItemQuantity: (itemId) => {
        const item = get().items.find(i => i.id === itemId);
        return item ? item.quantity : 0;
      },

      // 获取总价
      getTotalPrice: () => {
        return get().items.reduce((total, item) => total + (item.price * item.quantity), 0);
      },

      // 获取总商品数
      getTotalItems: () => {
        return get().items.reduce((total, item) => total + item.quantity, 0);
      },
    }),
    {
      name: 'cart-storage', // localStorage key
    }
  )
);

export default useCartStore;
