(ns anvil.effect
  (:require [cdq.context :as world]
            [clojure.component :as component]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn filter-applicable? [effect-ctx effect]
  (filter #(component/applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [context effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(component/useful? % effect-ctx context))))

(defn do-all! [context effect-ctx effect]
  (run! #(component/handle % effect-ctx context)
        (filter-applicable? effect-ctx effect)))

(defn render-info [context effect-ctx effect]
  (run! #(component/render-effect % effect-ctx context) effect))
