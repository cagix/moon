(ns forge.property
  (:refer-clojure :exclude [type])
  (:require [forge.schema :as schema]))

(defn type->id-namespace [property-type]
  (keyword (name property-type)))

(defn type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn types []
  (filter #(= "properties" (namespace %)) (keys schema/schemas)))

(defn ->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))
