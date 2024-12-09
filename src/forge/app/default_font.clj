(ns forge.app.default-font
  (:require [anvil.graphics :refer [default-font]]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ font]]
  (bind-root default-font (freetype/generate-font font)))

(defn destroy [_]
  (dispose default-font))
