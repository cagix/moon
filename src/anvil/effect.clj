(ns anvil.effect
  (:require [clojure.component :as component]))

(defn applicable? [ctx effects]
  (seq (filter #(component/applicable? % ctx) effects)))

(defn useful? [ctx effects]
  (->> effects
       (applicable? ctx)
       (some #(component/useful? % ctx))))

(defn do! [ctx effects]
  (run! #(component/handle % ctx)
        (applicable? ctx effects)))

(defn render [ctx effects]
  (run! #(component/render-effect % ctx)
        effects))
