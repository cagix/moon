(ns forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]))

(defn init [font]
  (def default-font (freetype/generate-font font)))

(defn dispose []
  (.dispose default-font))
