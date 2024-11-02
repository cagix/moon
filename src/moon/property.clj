(ns moon.property
  (:refer-clojure :exclude [def type])
  (:require [moon.schema :as schema]
            [malli.core :as m]
            [malli.error :as me]))

(defn def [k {:keys [overview]}]
  (defc k {:overview overview}))

(defn type->id-namespace [property-type]
  (keyword (name property-type)))

(defn type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn types []
  (filter #(= "properties" (namespace %)) (keys component-attrs)))

(defn- m-schema [property]
  (-> property type schema/form-of))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(defn validate! [property]
  (let [m-schema (m/schema (m-schema property))]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

(defn overview [property-type]
  (:overview (component-attrs property-type)))

(defn ->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))
