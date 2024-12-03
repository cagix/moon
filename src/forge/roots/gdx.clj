(ns forge.roots.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn cursor [file [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (.internal Gdx/files file))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(def k->input-button (partial static-field "Input$Buttons"))
(def k->input-key    (partial static-field "Input$Keys"))
