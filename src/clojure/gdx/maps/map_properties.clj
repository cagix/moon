(ns clojure.gdx.maps.map-properties
  (:import (com.badlogic.gdx.maps MapProperties)))

(defn ->clj-map [^MapProperties mp]
  (zipmap (.getKeys   mp)
          (.getValues mp)))

(defn add! [^MapProperties mp properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put mp k v)))

(defn create [m]
  (let [mp (MapProperties.)]
    (add! mp m)
    mp))
