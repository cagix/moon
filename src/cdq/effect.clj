(ns cdq.effect)

(comment
 (defprotocol Effect
   (fooz [_ effect-ctx]))

 (extend-type clojure.lang.PersistentVector
   Effect
   (fooz [[k] effect-ctx]
     (str "dispatching on k internally")))

 (fooz [:effect/bar 123] {}))

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

; TODO this doesnt go here

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [ctx effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx ctx))))
