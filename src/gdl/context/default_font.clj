(ns gdl.context.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [gdl.context :as ctx]))

(defn setup [config]
  (bind-root ctx/default-font (freetype/generate-font config)))

(defn cleanup []
  (dispose ctx/default-font))
