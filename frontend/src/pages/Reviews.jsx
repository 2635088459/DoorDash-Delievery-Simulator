import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { reviewService, restaurantService } from '../services/apiService';

const ratingLabel = (rating) => {
  if (rating >= 4) return 'Positive';
  if (rating >= 3) return 'Neutral';
  return 'Negative';
};

const formatDateTime = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const stopWords = new Set([
  '的', '了', '和', '是', '很', '也', '就', '都', '还', '在', '有', '但', '不', '没', '与', '及', '而',
  'this', 'that', 'with', 'for', 'from', 'was', 'were', 'are', 'is', 'the', 'and', 'but', 'not',
  'very', 'good', 'bad', 'nice', 'food', 'order', 'service', 'delivery', '店', '餐', '饭',
]);

const tokenizeComment = (comment) => {
  if (!comment) return [];
  const cleaned = comment
    .toLowerCase()
    .replace(/[0-9]/g, ' ')
    .replace(/[^a-z\u4e00-\u9fa5\s]/g, ' ');
  const tokens = cleaned.split(/\s+/).filter((token) => token && !stopWords.has(token));
  return tokens;
};

const Reviews = () => {
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [ratingSummary, setRatingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('all');
  const [replyDrafts, setReplyDrafts] = useState({});
  const [savingReplyId, setSavingReplyId] = useState(null);

  useEffect(() => {
    const loadReviews = async () => {
      try {
        setLoading(true);
        const ownerRestaurant = await restaurantService.getOwnerRestaurant();
        setRestaurant(ownerRestaurant);
        const [reviewList, rating] = await Promise.all([
          reviewService.getByRestaurant(ownerRestaurant.id),
          reviewService.getRestaurantRating(ownerRestaurant.id),
        ]);
        setReviews(reviewList);
        setReplyDrafts(
          reviewList.reduce((acc, review) => {
            acc[review.id] = review.replyContent || '';
            return acc;
          }, {})
        );
        setRatingSummary(rating);
        setError('');
      } catch (err) {
        console.error('Failed to load reviews:', err);
        setError(err.response?.data?.message || 'Failed to load reviews');
      } finally {
        setLoading(false);
      }
    };

    loadReviews();
  }, []);

  const filteredReviews = useMemo(() => {
    if (filter === 'positive') {
      return reviews.filter((review) => review.isPositive);
    }
    if (filter === 'negative') {
      return reviews.filter((review) => review.isNegative);
    }
    return reviews;
  }, [reviews, filter]);

  const keywordStats = useMemo(() => {
    const counter = new Map();
    filteredReviews.forEach((review) => {
      tokenizeComment(review.comment).forEach((token) => {
        counter.set(token, (counter.get(token) || 0) + 1);
      });
    });

    return Array.from(counter.entries())
      .map(([keyword, count]) => ({ keyword, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 12);
  }, [filteredReviews]);

  const handleReplyChange = (reviewId, value) => {
    setReplyDrafts((prev) => ({
      ...prev,
      [reviewId]: value,
    }));
  };

  const handleReplySubmit = async (reviewId) => {
    const content = (replyDrafts[reviewId] || '').trim();
    if (!content) {
      toast.error('Reply cannot be empty');
      return;
    }

    try {
      setSavingReplyId(reviewId);
      const updated = await reviewService.replyToReview(reviewId, { replyContent: content });
      setReviews((prev) => prev.map((review) => (review.id === reviewId ? updated : review)));
      toast.success('Reply saved');
    } catch (err) {
      console.error('Failed to reply review:', err);
      toast.error(err.response?.data?.message || 'Failed to save reply');
    } finally {
      setSavingReplyId(null);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>
    );
  }

  if (error && !restaurant) {
    return (
      <div className="max-w-5xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => navigate('/restaurant-home')}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            Back to restaurant home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8 mb-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Customer reviews</h1>
            <p className="text-gray-600">{restaurant?.name} · {restaurant?.city}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setFilter('all')}
              className={`px-4 py-2 rounded-lg border ${filter === 'all' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              All
            </button>
            <button
              onClick={() => setFilter('positive')}
              className={`px-4 py-2 rounded-lg border ${filter === 'positive' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              Positive
            </button>
            <button
              onClick={() => setFilter('negative')}
              className={`px-4 py-2 rounded-lg border ${filter === 'negative' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              Negative
            </button>
          </div>
        </div>
      </div>

  <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">Overall rating</div>
          <div className="text-3xl font-bold text-gray-900 mt-2">
            {ratingSummary?.averageRating ? Number(ratingSummary.averageRating).toFixed(1) : '--'}
          </div>
          <div className="text-sm text-gray-500 mt-1">{ratingSummary?.totalReviews ?? 0} reviews</div>
        </div>
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">Food rating</div>
          <div className="text-3xl font-bold text-gray-900 mt-2">
            {ratingSummary?.averageFoodRating ? Number(ratingSummary.averageFoodRating).toFixed(1) : '--'}
          </div>
          <div className="text-sm text-gray-500 mt-1">Delivery rating {ratingSummary?.averageDeliveryRating ? Number(ratingSummary.averageDeliveryRating).toFixed(1) : '--'}</div>
        </div>
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">Review breakdown</div>
          <div className="text-sm text-gray-700 mt-3">Positive {ratingSummary?.positiveReviews ?? 0}</div>
          <div className="text-sm text-gray-700">Neutral {ratingSummary?.neutralReviews ?? 0}</div>
          <div className="text-sm text-gray-700">Negative {ratingSummary?.negativeReviews ?? 0}</div>
        </div>
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">Customer keywords</div>
          {keywordStats.length === 0 ? (
            <div className="text-sm text-gray-500 mt-3">No data yet</div>
          ) : (
            <div className="flex flex-wrap gap-2 mt-3">
              {keywordStats.map((item) => (
                <span
                  key={item.keyword}
                  className="px-2 py-1 rounded-full bg-gray-100 text-gray-700 text-xs"
                >
                  {item.keyword} · {item.count}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">Reviews</h2>
          <p className="text-gray-600 text-sm mt-1">{filteredReviews.length} total</p>
        </div>

        {filteredReviews.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-gray-400 text-6xl mb-4">💬</div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">No reviews yet</h3>
            <p className="text-gray-600">Wait for customers to complete orders and leave reviews</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {filteredReviews.map((review) => (
              <div key={review.id} className="p-6 hover:bg-gray-50 transition-colors">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                  <div>
                    <div className="flex flex-wrap items-center gap-3 mb-2">
                      <h3 className="text-lg font-semibold text-gray-900">Order #{review.orderNumber}</h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${review.isPositive ? 'bg-green-100 text-green-700' : review.isNegative ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'}`}>
                        {ratingLabel(Number(review.overallRating || 0))}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600">Customer: {review.customerName || 'Anonymous'}</div>
                    <div className="text-sm text-gray-500">{formatDateTime(review.createdAt)}</div>
                  </div>
                  <div className="text-sm text-gray-700">
                    <div>Overall rating: {Number(review.overallRating || 0).toFixed(1)}</div>
                    <div>Food rating: {review.foodRating ?? '--'}</div>
                    <div>Delivery rating: {review.deliveryRating ?? '--'}</div>
                  </div>
                </div>
                <div className="mt-4 bg-gray-50 rounded-lg p-4 text-gray-700">
                  {review.comment || 'Customer did not leave a comment.'}
                </div>
                <div className="mt-4">
                  <div className="text-sm font-medium text-gray-700 mb-2">Restaurant reply</div>
                  {review.replyContent ? (
                    <div className="bg-green-50 border border-green-200 rounded-lg p-3 text-sm text-green-800">
                      <div>{review.replyContent}</div>
                      <div className="text-xs text-green-600 mt-2">
                        {review.replyBy || 'Restaurant'} · {formatDateTime(review.replyAt)}
                      </div>
                    </div>
                  ) : null}
                  <textarea
                    value={replyDrafts[review.id] || ''}
                    onChange={(event) => handleReplyChange(review.id, event.target.value)}
                    placeholder="Write a reply..."
                    rows="3"
                    className="mt-2 w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                  />
                  <div className="flex justify-end mt-2">
                    <button
                      type="button"
                      onClick={() => handleReplySubmit(review.id)}
                      disabled={savingReplyId === review.id}
                      className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-60"
                    >
                      {savingReplyId === review.id ? 'Saving...' : 'Save reply'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="mt-6">
        <button
          onClick={() => navigate('/restaurant-home')}
          className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
        >
          Back to restaurant home
        </button>
      </div>
    </div>
  );
};

export default Reviews;
