(ns cdq.effect)

(declare k->method-map)

(defn applicable? [{k 0 :as component} effect-ctx]
  ((:applicable? (k->method-map k)) component effect-ctx))

(defn handle [{k 0 :as component} effect-ctx ctx]
  ((:handle (k->method-map k)) component effect-ctx ctx))

(defn useful? [{k 0 :as component} effect-ctx ctx]
  (if-let [f (:useful? (k->method-map k))]
    (f component effect-ctx ctx)
    true))

(defn render [{k 0 :as component} effect-ctx ctx]
  (if-let [f (:render (k->method-map k))]
    (f component effect-ctx ctx)
    nil))

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [ctx effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx ctx))))
