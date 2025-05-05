(ns cdq.effect)

(defmulti applicable? (fn [[k] effect-ctx]
                        k))

(defmulti handle (fn [[k] effect-ctx]
                   k))

(defmulti useful? (fn [[k] effect-ctx]
                    k))
(defmethod useful? :default [_ _effect-ctx] true)

(defmulti render (fn [[k] _effect-ctx]
                          k))
(defmethod render :default [_ _effect-ctx])

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx))))
