(ns forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.utils :refer [bind-root]]))

(declare default-font)

(defn create [[_ font]]
  (bind-root default-font (freetype/generate-font font)))

(defn destroy [_]
  (dispose default-font))
