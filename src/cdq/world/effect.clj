(ns cdq.world.effect)

(defmulti applicable? (fn [[k] effect-ctx]
                        k))

(defmulti handle (fn [[k] effect-ctx world]
                   k))

(defmulti useful? (fn [[k] effect-ctx world]
                    k))
(defmethod useful? :default [_ _effect-ctx world]
  true)

; ->> render is one step above world !!
; ->> not part of world ! so should not be here ! ?
(defmulti render (fn [[k] _effect-ctx ctx]
                   k))
(defmethod render :default [_ _effect-ctx ctx])

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [world effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx world))))
