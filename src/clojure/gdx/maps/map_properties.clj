(ns clojure.gdx.maps.map-properties
  (:import (com.badlogic.gdx.maps MapProperties)))

(defn ->clj-map [^MapProperties mp]
  (zipmap (.getKeys   mp)
          (.getValues mp)))

(defn add!
  "properties is a clojure map of string keys to values which get added to the `MapProperties`"
  [^MapProperties mp properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put mp k v)))
