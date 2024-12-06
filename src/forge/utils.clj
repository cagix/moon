(ns forge.utils
  (:require [clj-commons.pretty.repl :as pretty-repl]
            [clojure.pprint :refer [pprint]]))

(defmacro bind-root [sym value]
  `(clojure.lang.Var/.bindRoot (var ~sym) ~value))

#_(defmacro defn-impl [name-sym & fn-body]
    `(bind-root ~name-sym (fn ~name-sym ~@fn-body)))

#_(defmacro def-impl [name-sym value]
    `(bind-root ~name-sym ~value))

(defn safe-get [m k]
  (let [result (get m k ::not-found)]
    (if (= result ::not-found)
      (throw (IllegalArgumentException. (str "Cannot find " (pr-str k))))
      result)))

(defn mapvals [f m]
  (into {} (for [[k v] m]
             [k (f v)])))

(defn recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

; reduce-kv?
(defn apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defn async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint
             with-out-str
             (spit file)))))))

(defn pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))

(defn tile->middle [position]
  (mapv (partial + 0.5) position))

(defn ->tile [position]
  (mapv int position))

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

(let [cnt (atom 0)]
  (defn unique-number! []
    (swap! cnt inc)))

(defn index-of [k ^clojure.lang.PersistentVector v]
  (let [idx (.indexOf v k)]
    (if (= -1 idx)
      nil
      idx)))

(defn find-first
  "Returns the first item of coll for which (pred item) returns logical true.
  Consumes sequences up to the first match, will consume the entire sequence
  and return nil if no match is found."
  [pred coll]
  (first (filter pred coll)))

; libgdx fn is available:
; (MathUtils/isEqual 1 (length v))
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

(defn truncate [s limit]
  (if (> (count s) limit)
    (str (subs s 0 limit) "...")
    s))

(defn ->edn-str [v]
  (binding [*print-level* nil]
    (pr-str v)))

(defn- indexed ; from clojure.contrib.seq-utils (discontinued in 1.3)
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d)) => ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))

(defn utils-positions ; from clojure.contrib.seq-utils (discontinued in 1.3)
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
