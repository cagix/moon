(ns cdq.effect)

(defprotocol Effect
  (applicable? [_ effect-ctx])
  (useful?     [_ effect-ctx ctx])
  (handle      [_ effect-ctx ctx]))

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [ctx effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx ctx))))
