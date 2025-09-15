(ns clojure.gdx.maps.properties
  (:import (com.badlogic.gdx.maps MapProperties)))

(defn add! [^MapProperties mp properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put mp k v)))

(defn create [properties]
  (doto (MapProperties.)
    (add! properties)))

(defn ->clj [^MapProperties mp]
  (zipmap (.getKeys   mp)
          (.getValues mp)))
