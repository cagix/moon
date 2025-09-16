(ns clojure.utils)

(defn define-order [order-k-vector]
  (apply hash-map (interleave order-k-vector (range))))

(defn sort-by-order [coll get-item-order-k order]
  (sort-by #((get-item-order-k %) order) < coll))

#_(defn order-contains? [order k]
    ((apply hash-set (keys order)) k))

#_(deftest test-order
    (is
     (= (define-order [:a :b :c]) {:a 0 :b 1 :c 2}))
    (is
     (order-contains? (define-order [:a :b :c]) :a))
    (is
     (not
      (order-contains? (define-order [:a :b :c]) 2)))
    (is
     (=
      (sort-by-order [:c :b :a :b] identity (define-order [:a :b :c]))
      '(:a :b :b :c)))
    (is
     (=
      (sort-by-order [:b :c :null :null :a] identity (define-order [:c :b :a :null]))
      '(:c :b :a :null :null))))

(defn safe-merge [m1 m2]
  {:pre [(not-any? #(contains? m1 %) (keys m2))]}
  (merge m1 m2))

(defn- approx-numbers [a b epsilon]
  (<=
   (Math/abs (float (- a b)))
   epsilon))

(defn- round-n-decimals [^double x n]
  (let [z (Math/pow 10 n)]
    (float
     (/
      (Math/round (float (* x z)))
      z))))

(defn readable-number [^double x]
  {:pre [(number? x)]} ; do not assert (>= x 0) beacuse when using floats x may become -0.000...000something
  (if (or
       (> x 5)
       (approx-numbers x (int x) 0.001)) ; for "2.0" show "2" -> simpler
    (int x)
    (round-n-decimals x 2)))

(defn assoc-ks [m ks v]
  (if (empty? ks)
    m
    (apply assoc m (interleave ks (repeat v)))))

; private thingy ...
(defn- indexed ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d)) => ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

; from clojure.contrib.seq-utils (discontinued in 1.3)
(defn positions
  "Returns a lazy sequence containing the positions at which pred
  is true for items in coll."
  [pred coll]
  (for [[idx elt] (indexed coll) :when (pred elt)] idx))

(defmacro when-seq [[aseq bind] & body]
  `(let [~aseq ~bind]
     (when (seq ~aseq)
       ~@body)))

(defn dissoc-in [m ks]
  (assert (> (count ks) 1))
  (update-in m (drop-last ks) dissoc (last ks)))

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

(def float-rounding-error (double 0.000001)) ; <- FIXME clojure uses doubles?

(defn nearly-equal? [^double x ^double y]
  (<= (Math/abs (- x y)) float-rounding-error))
