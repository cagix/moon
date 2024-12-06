(ns forge.app.default-font
  (:require [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.core :refer [default-font]]
            [forge.utils :refer [bind-root]]))

(defn create [[_ font]]
  (bind-root default-font (freetype/generate-font font)))

(defn destroy [_]
  (dispose default-font))
