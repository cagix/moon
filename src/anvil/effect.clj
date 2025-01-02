(ns anvil.effect
  (:require [clojure.component :as component]))

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
